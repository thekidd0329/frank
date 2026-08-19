//! Experimental residual-commitment atom.
//!
//! IMPORTANT: this is NOT yet part of the stable `frank.cog` ABI.
//! It is a pressure-test candidate for the ground cognitive primitive.
//!
//! Ground-layer rule:
//! a residual commitment is the complete tuple, not merely a weight.
//! The tuple remains independently inspectable and reversible.
//!
//! Candidate A packs exactly 128 bits / 16 bytes:
//!
//! word 0 (64 bits)
//!   locus              48
//!   context_tag        16
//!
//! word 1 (64 bits)
//!   signed_force        8
//!   binding_strength    7
//!   generation_anchor  20
//!   persistence_class   5
//!   kind                4
//!   flags               4
//!   provenance_handle  16
//!
//! `signed_force` combines polarity and residual-force magnitude. Value -128 is
//! deliberately rejected so the usable range is symmetric: -127..=127.
//! Zero means no directional residual force.
//!
//! `kind` and `flags` are intentionally uninterpreted at this layer. Their
//! semantic meanings must only be assigned if they prove truly universal.
//!
//! `provenance_handle == 0` means no provenance. The common case therefore
//! remains fully inline and exactly 16 bytes.

use core::fmt;

pub const COMMITMENT_BYTES: usize = 16;

pub const LOCUS_BITS: u32 = 48;
pub const CONTEXT_BITS: u32 = 16;
pub const FORCE_BITS: u32 = 8;
pub const BINDING_BITS: u32 = 7;
pub const GENERATION_BITS: u32 = 20;
pub const PERSISTENCE_BITS: u32 = 5;
pub const KIND_BITS: u32 = 4;
pub const FLAGS_BITS: u32 = 4;
pub const PROVENANCE_BITS: u32 = 16;

pub const MAX_LOCUS: u64 = (1u64 << LOCUS_BITS) - 1;
pub const MAX_BINDING: u8 = (1u8 << BINDING_BITS) - 1;
pub const MAX_GENERATION_ANCHOR: u32 = (1u32 << GENERATION_BITS) - 1;
pub const MAX_PERSISTENCE_CLASS: u8 = (1u8 << PERSISTENCE_BITS) - 1;
pub const MAX_KIND: u8 = (1u8 << KIND_BITS) - 1;
pub const MAX_FLAGS: u8 = (1u8 << FLAGS_BITS) - 1;

const FORCE_SHIFT: u32 = 0;
const BINDING_SHIFT: u32 = FORCE_SHIFT + FORCE_BITS;
const GENERATION_SHIFT: u32 = BINDING_SHIFT + BINDING_BITS;
const PERSISTENCE_SHIFT: u32 = GENERATION_SHIFT + GENERATION_BITS;
const KIND_SHIFT: u32 = PERSISTENCE_SHIFT + PERSISTENCE_BITS;
const FLAGS_SHIFT: u32 = KIND_SHIFT + KIND_BITS;
const PROVENANCE_SHIFT: u32 = FLAGS_SHIFT + FLAGS_BITS;

const _: () = assert!(PROVENANCE_SHIFT + PROVENANCE_BITS == 64);

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ResidualCommitment {
    /// Compact address in cognitive space.
    pub locus: u64,
    /// Local contextual discriminator. It is deliberately separate from locus.
    pub context_tag: u16,
    /// Direction + magnitude of the residual commitment.
    /// Valid range is -127..=127; -128 is reserved/invalid.
    pub signed_force: i8,
    /// Context-binding strength, 0..=127.
    pub binding_strength: u8,
    /// Compact relative generation/time anchor, 20 bits.
    pub generation_anchor: u32,
    /// Selects one of at most 32 temporal persistence/decay regimes.
    pub persistence_class: u8,
    /// Four universal structural bits; ontology-specific meanings do not belong here.
    pub kind: u8,
    /// Four universal state bits; meanings remain intentionally unassigned for now.
    pub flags: u8,
    /// Zero means absent. Non-zero addresses an optional expensive auxiliary record.
    pub provenance_handle: u16,
}

impl ResidualCommitment {
    pub fn validate(self) -> Result<(), CommitmentError> {
        if self.locus > MAX_LOCUS {
            return Err(CommitmentError::LocusOutOfRange(self.locus));
        }
        if self.signed_force == i8::MIN {
            return Err(CommitmentError::ReservedForceValue);
        }
        if self.binding_strength > MAX_BINDING {
            return Err(CommitmentError::BindingOutOfRange(self.binding_strength));
        }
        if self.generation_anchor > MAX_GENERATION_ANCHOR {
            return Err(CommitmentError::GenerationOutOfRange(self.generation_anchor));
        }
        if self.persistence_class > MAX_PERSISTENCE_CLASS {
            return Err(CommitmentError::PersistenceClassOutOfRange(
                self.persistence_class,
            ));
        }
        if self.kind > MAX_KIND {
            return Err(CommitmentError::KindOutOfRange(self.kind));
        }
        if self.flags > MAX_FLAGS {
            return Err(CommitmentError::FlagsOutOfRange(self.flags));
        }
        Ok(())
    }

    pub fn pack(self) -> Result<PackedCommitment, CommitmentError> {
        self.validate()?;

        let word0 = (self.locus & MAX_LOCUS) | ((self.context_tag as u64) << 48);

        let mut word1 = 0u64;
        word1 |= (self.signed_force as u8 as u64) << FORCE_SHIFT;
        word1 |= (self.binding_strength as u64) << BINDING_SHIFT;
        word1 |= (self.generation_anchor as u64) << GENERATION_SHIFT;
        word1 |= (self.persistence_class as u64) << PERSISTENCE_SHIFT;
        word1 |= (self.kind as u64) << KIND_SHIFT;
        word1 |= (self.flags as u64) << FLAGS_SHIFT;
        word1 |= (self.provenance_handle as u64) << PROVENANCE_SHIFT;

        let mut bytes = [0u8; COMMITMENT_BYTES];
        bytes[..8].copy_from_slice(&word0.to_le_bytes());
        bytes[8..].copy_from_slice(&word1.to_le_bytes());
        Ok(PackedCommitment(bytes))
    }

    pub fn polarity(self) -> Polarity {
        match self.signed_force.cmp(&0) {
            core::cmp::Ordering::Less => Polarity::Negative,
            core::cmp::Ordering::Equal => Polarity::Neutral,
            core::cmp::Ordering::Greater => Polarity::Positive,
        }
    }

    pub fn force_magnitude(self) -> u8 {
        self.signed_force.unsigned_abs()
    }

    pub const fn has_provenance(self) -> bool {
        self.provenance_handle != 0
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Polarity {
    Negative,
    Neutral,
    Positive,
}

/// Exact 16-byte reversible representation.
///
/// This wrapper exists specifically so callers never depend on Rust struct layout.
#[derive(Clone, Copy, Eq, PartialEq, Hash)]
#[repr(transparent)]
pub struct PackedCommitment(pub [u8; COMMITMENT_BYTES]);

impl PackedCommitment {
    pub const fn as_bytes(&self) -> &[u8; COMMITMENT_BYTES] {
        &self.0
    }

    pub fn unpack(self) -> Result<ResidualCommitment, CommitmentError> {
        let word0 = u64::from_le_bytes(self.0[..8].try_into().expect("fixed 8-byte slice"));
        let word1 = u64::from_le_bytes(self.0[8..].try_into().expect("fixed 8-byte slice"));

        let raw_force = ((word1 >> FORCE_SHIFT) & mask(FORCE_BITS)) as u8;

        let result = ResidualCommitment {
            locus: word0 & MAX_LOCUS,
            context_tag: (word0 >> 48) as u16,
            signed_force: raw_force as i8,
            binding_strength: ((word1 >> BINDING_SHIFT) & mask(BINDING_BITS)) as u8,
            generation_anchor: ((word1 >> GENERATION_SHIFT) & mask(GENERATION_BITS)) as u32,
            persistence_class: ((word1 >> PERSISTENCE_SHIFT) & mask(PERSISTENCE_BITS)) as u8,
            kind: ((word1 >> KIND_SHIFT) & mask(KIND_BITS)) as u8,
            flags: ((word1 >> FLAGS_SHIFT) & mask(FLAGS_BITS)) as u8,
            provenance_handle: ((word1 >> PROVENANCE_SHIFT) & mask(PROVENANCE_BITS)) as u16,
        };

        result.validate()?;
        Ok(result)
    }
}

impl fmt::Debug for PackedCommitment {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_tuple("PackedCommitment").field(&self.0).finish()
    }
}

const fn mask(bits: u32) -> u64 {
    (1u64 << bits) - 1
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum CommitmentError {
    LocusOutOfRange(u64),
    ReservedForceValue,
    BindingOutOfRange(u8),
    GenerationOutOfRange(u32),
    PersistenceClassOutOfRange(u8),
    KindOutOfRange(u8),
    FlagsOutOfRange(u8),
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

    fn example() -> ResidualCommitment {
        ResidualCommitment {
            locus: 0x00A1_B2C3_D4E5,
            context_tag: 0xBEEF,
            signed_force: -93,
            binding_strength: 101,
            generation_anchor: 0xA_BCDE,
            persistence_class: 17,
            kind: 6,
            flags: 0b1010,
            provenance_handle: 0,
        }
    }

    #[test]
    fn packed_atom_is_exactly_16_bytes() {
        assert_eq!(core::mem::size_of::<PackedCommitment>(), 16);
    }

    #[test]
    fn candidate_a_uses_exactly_128_bits() {
        assert_eq!(
            LOCUS_BITS
                + CONTEXT_BITS
                + FORCE_BITS
                + BINDING_BITS
                + GENERATION_BITS
                + PERSISTENCE_BITS
                + KIND_BITS
                + FLAGS_BITS
                + PROVENANCE_BITS,
            128
        );
    }

    #[test]
    fn pack_unpack_round_trip_is_exact() {
        let original = example();
        let unpacked = original.pack().unwrap().unpack().unwrap();
        assert_eq!(unpacked, original);
    }

    #[test]
    fn positive_negative_and_neutral_are_distinct() {
        let mut c = example();

        c.signed_force = -127;
        assert_eq!(c.polarity(), Polarity::Negative);
        assert_eq!(c.force_magnitude(), 127);

        c.signed_force = 0;
        assert_eq!(c.polarity(), Polarity::Neutral);
        assert_eq!(c.force_magnitude(), 0);

        c.signed_force = 127;
        assert_eq!(c.polarity(), Polarity::Positive);
        assert_eq!(c.force_magnitude(), 127);
    }

    #[test]
    fn reserved_negative_128_is_rejected() {
        let mut c = example();
        c.signed_force = -128;
        assert_eq!(c.pack(), Err(CommitmentError::ReservedForceValue));
    }

    #[test]
    fn context_is_separable_from_locus() {
        let mut a = example();
        let mut b = example();
        a.context_tag = 1;
        b.context_tag = 2;

        assert_eq!(a.locus, b.locus);
        assert_ne!(a.pack().unwrap(), b.pack().unwrap());

        let aa = a.pack().unwrap().unpack().unwrap();
        let bb = b.pack().unwrap().unpack().unwrap();
        assert_eq!(aa.locus, bb.locus);
        assert_ne!(aa.context_tag, bb.context_tag);
    }

    #[test]
    fn contradictory_commitments_can_share_locus_and_context() {
        let mut positive = example();
        let mut negative = example();
        positive.signed_force = 72;
        negative.signed_force = -81;

        assert_eq!(positive.locus, negative.locus);
        assert_eq!(positive.context_tag, negative.context_tag);
        assert_ne!(positive.pack().unwrap(), negative.pack().unwrap());
    }

    #[test]
    fn provenance_is_optional_and_does_not_change_width() {
        let mut without = example();
        let mut with = example();

        without.provenance_handle = 0;
        with.provenance_handle = 42;

        assert!(!without.has_provenance());
        assert!(with.has_provenance());
        assert_eq!(without.pack().unwrap().as_bytes().len(), COMMITMENT_BYTES);
        assert_eq!(with.pack().unwrap().as_bytes().len(), COMMITMENT_BYTES);
    }

    #[test]
    fn all_maximum_field_values_round_trip() {
        let c = ResidualCommitment {
            locus: MAX_LOCUS,
            context_tag: u16::MAX,
            signed_force: 127,
            binding_strength: MAX_BINDING,
            generation_anchor: MAX_GENERATION_ANCHOR,
            persistence_class: MAX_PERSISTENCE_CLASS,
            kind: MAX_KIND,
            flags: MAX_FLAGS,
            provenance_handle: u16::MAX,
        };

        assert_eq!(c.pack().unwrap().unpack().unwrap(), c);
    }

    #[test]
    fn out_of_range_fields_are_rejected_before_packing() {
        let mut c = example();
        c.locus = MAX_LOCUS + 1;
        assert!(matches!(c.pack(), Err(CommitmentError::LocusOutOfRange(_))));

        let mut c = example();
        c.binding_strength = MAX_BINDING + 1;
        assert!(matches!(c.pack(), Err(CommitmentError::BindingOutOfRange(_))));

        let mut c = example();
        c.generation_anchor = MAX_GENERATION_ANCHOR + 1;
        assert!(matches!(c.pack(), Err(CommitmentError::GenerationOutOfRange(_))));

        let mut c = example();
        c.persistence_class = MAX_PERSISTENCE_CLASS + 1;
        assert!(matches!(
            c.pack(),
            Err(CommitmentError::PersistenceClassOutOfRange(_))
        ));

        let mut c = example();
        c.kind = MAX_KIND + 1;
        assert!(matches!(c.pack(), Err(CommitmentError::KindOutOfRange(_))));

        let mut c = example();
        c.flags = MAX_FLAGS + 1;
        assert!(matches!(c.pack(), Err(CommitmentError::FlagsOutOfRange(_))));
    }
}
