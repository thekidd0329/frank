//! FRANK native cognitive substrate experiments.
//!
//! Ground contract:
//! - Residual Commitment Field is the authoritative cognitive state.
//! - Atom = (locus_id, net_force, last_updated_tick).
//! - Zero is not durable.
//! - Sparse fruit-bearing tree.
//! - Rust-native ground runtime; no Python reasoning runtime.
//! - Locus birth and higher-order binding remain experimental.

pub mod commitment;
pub mod schema;
pub mod field;
pub mod allocator;
pub mod coactivation;

pub use commitment::{
    CommitmentError, LocusId, PackedCommitment, ResidualCommitment,
    COMMITMENT_BYTES, MAX_FORCE, MIN_FORCE,
};
pub use field::{CommitmentField, DEFAULT_PRUNE};
pub use allocator::{allocate_locus, content_preferred_locus, AllocatorConfig};
pub use coactivation::{derived_locus, maybe_birth_coactivation, co_occurrence_force};

/// One bounded experimental developmental step.
///
/// At most one previous active locus may be supplied, intentionally preventing
/// combinatorial co-activation fan-out at this layer.
pub fn observe(
    field: &mut CommitmentField,
    observation: &[u8],
    evidence_force: f32,
    previous_active: Option<LocusId>,
    alloc_cfg: &AllocatorConfig,
) -> Result<Option<ObserveResult>, CommitmentError> {
    let locus = allocate_locus(field, observation, alloc_cfg);
    let Some(commitment) = field.birth_or_reinforce(locus, evidence_force)? else {
        return Ok(None);
    };

    let mut derived = None;
    if let Some(prev) = previous_active {
        if let Some((d, f)) = maybe_birth_coactivation(field, prev, locus)? {
            derived = Some((d, f));
        }
    }

    Ok(Some(ObserveResult {
        locus,
        net_force: commitment.net_force,
        derived,
    }))
}

#[derive(Clone, Debug)]
pub struct ObserveResult {
    pub locus: LocusId,
    pub net_force: f32,
    pub derived: Option<(LocusId, f32)>,
}
