//! Experimental novel-locus identity scaffold.
//!
//! This is NOT a solved ontology-free topology algorithm. It currently answers only:
//! "Can the same opaque observation bytes recover the same address after restart?"
//!
//! It does NOT yet provide semantic locality. A mixed content hash deliberately spreads
//! nearby inputs apart, so topology/neighbor meaning remains unsolved. Hash collisions
//! also cannot be distinguished without adding more identity state; that failure is
//! explicit rather than hidden behind an unreachable collision walk.

use crate::commitment::LocusId;
use crate::field::CommitmentField;

fn mix64(mut x: u64) -> u64 {
    x = x.wrapping_mul(0x9E3779B97F4A7C15);
    x ^= x >> 32;
    x = x.wrapping_mul(0xBF58476D1CE4E5B9);
    x ^= x >> 29;
    x = x.wrapping_mul(0x94D049BB133111EB);
    x ^= x >> 32;
    x
}

pub fn content_preferred_locus(observation: &[u8]) -> LocusId {
    let mut h: u64 = 0x517cc1b727220a95;
    for &b in observation {
        h = h.wrapping_add(b as u64);
        h = mix64(h);
    }
    h & 0x7FFF_FFFF_FFFF_FFFF
}

#[derive(Clone, Debug, Default)]
pub struct AllocatorConfig;

pub fn allocate_locus(_field: &CommitmentField, observation: &[u8], _config: &AllocatorConfig) -> LocusId {
    content_preferred_locus(observation)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn stable_for_same_bytes() {
        let f = CommitmentField::new(0.90);
        let cfg = AllocatorConfig;
        let obs = b"first pattern ever";
        assert_eq!(allocate_locus(&f, obs, &cfg), content_preferred_locus(obs));
        assert_eq!(allocate_locus(&f, obs, &cfg), allocate_locus(&f, obs, &cfg));
    }

    #[test]
    fn allocator_does_not_claim_semantic_locality() {
        let a = content_preferred_locus(b"pattern-A");
        let b = content_preferred_locus(b"pattern-B");
        assert_ne!(a, b);
    }
}
