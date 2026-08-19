//! Experimental three-variable residual-commitment atom.
//!
//! IMPORTANT: this is NOT yet part of the stable `frank.cog` ABI.
//! Candidate A exists to pressure-test the smallest ground record that still
//! preserves Frank's reconstruction invariant.
//!
//! The ground atom has exactly three semantic variables:
//!
//! 1. `locus_id`          — WHERE the residual force lives.
//! 2. `net_force`         — DIRECTION + STRENGTH, signed in [-1.0, 1.0].
//! 3. `last_updated_tick` — WHEN the commitment was last materially maintained.
//!
//! Zero is meaningful as equilibrium, but it is not durable state. An absent
//! locus means there is no lasting commitment there. If decay or cancellation
//! reaches the prune threshold, the record is removed rather than stored as an
//! empty node.
//!
//! Candidate A packs these three variables into exactly 128 bits / 16 bytes:
//!
//! word 0 (64 bits)
//!   locus_id            64
//!
//! word 1 (64 bits)
//!   net_force (f32)     32
//!   last_updated_tick   32
//!
//! The 32-bit tick is an EXPERIMENTAL relative/local clock for Candidate A.
//! Epoch and wrap semantics remain image-level research questions. If real
//! measurements prove 32 bits insufficient, Candidate A loses; the architecture
//! must not fake precision to protect a packing target.

use core::fmt;

pub const COMMITMENT_BYTES: usize = 16;
pub const MIN_FORCE: f32 = -1.0;
pub const MAX_FORCE: f32 = 1.0;

pub type LocusId = u64;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct ResidualCommitment {
    /// Stable address in Frank's cognitive space.
    ///
    /// Identity: this particular meaning lives here.
    /// Topology: the address/neighborhood may also determine locality and decay
    /// behavior once the locus-construction scheme is experimentally settled.
    pub locus_id: LocusId,

    /// Signed residual force. Sign is polarity; magnitude is strength.
    /// Valid durable range is [-1.0, 1.0] excluding exactly 0.0.
    pub net_force: f32,

    /// Relative/local logical tick when this record was last materialized by
    /// reinforcement, contradiction, or a lazy-decay query.
    pub last_updated_tick: u32,
}

impl ResidualCommitment {
    pub fn new(
        locus_id: LocusId,
        net_force: f32,
        last_updated_tick: u32,
    ) -> Result<Self, CommitmentError> {
        let commitment = Self {
            locus_id,
            net_force,
            last_updated_tick,
        };
        commitment.validate()?;
        Ok(commitment)
    }

    pub fn validate(self) -> Result<(), CommitmentError> {
        validate_force(self.net_force)?;
        if self.net_force == 0.0 {
            return Err(CommitmentError::ZeroForceIsNotDurable);
        }
        Ok(())
    }

    /// Derived polarity code only. It is deliberately not stored as a fourth field.
    /// Durable commitments therefore return only -1 or +1; 0 is the neutral/pruned state.
    pub fn direction(self) -> i8 {
        if self.net_force > 0.0 {
            1
        } else if self.net_force < 0.0 {
            -1
        } else {
            0
        }
    }

    /// Lazily evaluate force at `current_tick` without inventing a global decay scan.
    ///
    /// `retention_per_tick` is supplied by the locus topology/region policy. The
    /// exact mapping from locus bits to neighborhood/decay class is intentionally
    /// NOT frozen in Candidate A.
    pub fn effective_force(
        self,
        current_tick: u32,
        retention_per_tick: f32,
    ) -> Result<f32, CommitmentError> {
        validate_retention(retention_per_tick)?;
        if current_tick < self.last_updated_tick {
            return Err(CommitmentError::TickWentBackward {
                current: current_tick,
                last_updated: self.last_updated_tick,
            });
        }
        let delta = current_tick - self.last_updated_tick;
        Ok(self.net_force * retention_per_tick.powf(delta as f32))
    }

    /// Materialize lazy decay at query time.
    ///
    /// Returning `None` means the fruit has fallen off the tree: the locus should
    /// be removed from durable storage rather than persisted with zero force.
    pub fn materialize_at(
        self,
        current_tick: u32,
        retention_per_tick: f32,
        prune_threshold: f32,
    ) -> Result<Option<Self>, CommitmentError> {
        validate_threshold(prune_threshold)?;
        let force = self.effective_force(current_tick, retention_per_tick)?;
        if force.abs() <= prune_threshold {
            return Ok(None);
        }
        Self::new(self.locus_id, force, current_tick).map(Some)
    }

    /// Apply signed incoming evidence after lazy decay.
    ///
    /// Same-sign evidence reinforces. Opposite-sign evidence contradicts. No
    /// separate polarity or contestation field is required: signed arithmetic
    /// supplies the push/pull directly.
    pub fn apply_evidence(
        self,
        current_tick: u32,
        evidence_force: f32,
        retention_per_tick: f32,
        prune_threshold: f32,
    ) -> Result<Option<Self>, CommitmentError> {
        validate_force(evidence_force)?;
        validate_threshold(prune_threshold)?;

        if evidence_force == 0.0 {
            return self.materialize_at(current_tick, retention_per_tick, prune_threshold);
        }

        let decayed = self.effective_force(current_tick, retention_per_tick)?;
        let combined = (decayed + evidence_force).clamp(MIN_FORCE, MAX_FORCE);

        if combined.abs() <= prune_threshold {
            return Ok(None);
        }

        Self::new(self.locus_id, combined, current_tick).map(Some)
    }

    /// Candidate A reversible 16-byte packing.
    pub fn pack(self) -> Result<PackedCommitment, CommitmentError> {
        self.validate()?;

        let mut bytes = [0u8; COMMITMENT_BYTES];
        bytes[..8].copy_from_slice(&self.locus_id.to_le_bytes());
        bytes[8..12].copy_from_slice(&self.net_force.to_bits().to_le_bytes());
        bytes[12..16].copy_from_slice(&self.last_updated_tick.to_le_bytes());
        Ok(PackedCommitment(bytes))
    }
}

/// Exact Candidate A 16-byte representation.
///
/// This wrapper exists so the on-disk experiment never depends on Rust struct
/// layout, padding, compiler version, or target ABI.
#[derive(Clone, Copy, Eq, PartialEq, Hash)]
#[repr(transparent)]
pub struct PackedCommitment(pub [u8; COMMITMENT_BYTES]);

impl PackedCommitment {
    pub const fn as_bytes(&self) -> &[u8; COMMITMENT_BYTES] {
        &self.0
    }

    pub fn unpack(self) -> Result<ResidualCommitment, CommitmentError> {
        let locus_id = u64::from_le_bytes(self.0[..8].try_into().expect("fixed 8-byte slice"));
        let force_bits = u32::from_le_bytes(self.0[8..12].try_into().expect("fixed 4-byte slice"));
        let last_updated_tick =
            u32::from_le_bytes(self.0[12..16].try_into().expect("fixed 4-byte slice"));

        ResidualCommitment::new(locus_id, f32::from_bits(force_bits), last_updated_tick)
    }
}

impl fmt::Debug for PackedCommitment {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_tuple("PackedCommitment").field(&self.0).finish()
    }
}

fn validate_force(force: f32) -> Result<(), CommitmentError> {
    if !force.is_finite() || !(MIN_FORCE..=MAX_FORCE).contains(&force) {
        return Err(CommitmentError::ForceOutOfRange);
    }
    Ok(())
}

fn validate_retention(retention: f32) -> Result<(), CommitmentError> {
    if !retention.is_finite() || !(0.0..=1.0).contains(&retention) {
        return Err(CommitmentError::RetentionOutOfRange);
    }
    Ok(())
}

fn validate_threshold(threshold: f32) -> Result<(), CommitmentError> {
    if !threshold.is_finite() || !(0.0..=1.0).contains(&threshold) {
        return Err(CommitmentError::PruneThresholdOutOfRange);
    }
    Ok(())
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum CommitmentError {
    ForceOutOfRange,
    ZeroForceIsNotDurable,
    RetentionOutOfRange,
    PruneThresholdOutOfRange,
    TickWentBackward { current: u32, last_updated: u32 },
}

impl fmt::Display for CommitmentError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{self:?}")
    }
}

impl std::error::Error for CommitmentError {}

#[cfg(test)]
mod tests {
    use super::*;

    const EPS: f32 = 0.000_001;

    #[test]
    fn candidate_a_is_exactly_128_bits() {
        assert_eq!(core::mem::size_of::<PackedCommitment>(), 16);
    }

    #[test]
    fn pack_unpack_round_trip_preserves_all_three_variables() {
        let original = ResidualCommitment::new(0xA1, -0.625, 42).unwrap();
        let unpacked = original.pack().unwrap().unpack().unwrap();
        assert_eq!(unpacked.locus_id, original.locus_id);
        assert_eq!(unpacked.net_force.to_bits(), original.net_force.to_bits());
        assert_eq!(unpacked.last_updated_tick, original.last_updated_tick);
    }

    #[test]
    fn polarity_is_derived_from_signed_force() {
        assert_eq!(ResidualCommitment::new(1, -0.2, 0).unwrap().direction(), -1);
        assert_eq!(ResidualCommitment::new(1, 0.2, 0).unwrap().direction(), 1);
    }

    #[test]
    fn zero_is_transition_state_not_durable_state() {
        assert_eq!(
            ResidualCommitment::new(1, 0.0, 0),
            Err(CommitmentError::ZeroForceIsNotDurable)
        );
    }

    #[test]
    fn lazy_decay_does_not_invert_direction() {
        let c = ResidualCommitment::new(0xA1, 0.40, 1).unwrap();
        let force = c.effective_force(4, 0.90).unwrap();
        assert!((force - 0.2916).abs() < EPS);
        assert!(force > 0.0);
    }

    #[test]
    fn same_sign_evidence_reinforces_after_lazy_decay() {
        let c = ResidualCommitment::new(0xA1, 0.40, 1).unwrap();
        let next = c.apply_evidence(2, 0.30, 0.90, 0.000_001).unwrap().unwrap();
        assert!((next.net_force - 0.66).abs() < EPS);
        assert_eq!(next.last_updated_tick, 2);
    }

    #[test]
    fn opposite_sign_evidence_can_flip_direction() {
        let c = ResidualCommitment::new(0xA1, 0.40, 1).unwrap();
        let next = c.apply_evidence(2, -0.90, 0.90, 0.000_001).unwrap().unwrap();
        assert!((next.net_force + 0.54).abs() < EPS);
        assert_eq!(next.direction(), -1);
    }

    #[test]
    fn exact_cancellation_prunes_instead_of_storing_zero() {
        let c = ResidualCommitment::new(0xA1, 0.50, 1).unwrap();
        let next = c.apply_evidence(1, -0.50, 1.0, 0.0).unwrap();
        assert_eq!(next, None);
    }

    #[test]
    fn near_zero_force_can_be_pruned_by_policy_threshold() {
        let c = ResidualCommitment::new(0xA1, 0.01, 1).unwrap();
        let next = c.materialize_at(2, 0.90, 0.02).unwrap();
        assert_eq!(next, None);
    }
}
