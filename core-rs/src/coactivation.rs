//! Experimental bounded higher-order residual birth from explicit co-activation.
//!
//! This is intentionally NOT an all-pairs fan-out mechanism. One developmental
//! observation may nominate at most one prior active locus. That bound is deliberate:
//! unbounded "many good paths / many bad paths" expansion is treated as a runtime
//! conflict/crisis problem, not something the ground layer should multiply blindly.
//!
//! IMPORTANT: the derived u64 fingerprint is not reversible to its parent pair.
//! Therefore this module pressure-tests higher-order residual persistence, but does
//! NOT yet satisfy the hard reconstruction invariant for a relation whose endpoints
//! must be recovered from the field alone.

use crate::commitment::LocusId;
use crate::field::CommitmentField;

pub fn derived_locus(a: LocusId, b: LocusId) -> LocusId {
    let (x, y) = if a <= b { (a, b) } else { (b, a) };
    let mut h = x.wrapping_mul(0x9E3779B97F4A7C15);
    h ^= y.wrapping_mul(0xBF58476D1CE4E5B9);
    h = h.wrapping_add(0x94D049BB133111EB);
    h ^= h >> 32;
    h = h.wrapping_mul(0xBF58476D1CE4E5B9);
    h ^= h >> 29;
    h & 0x7FFF_FFFF_FFFF_FFFF
}

pub fn co_occurrence_force(fa: f32, fb: f32) -> f32 {
    let mag = (fa.abs() * fb.abs()).sqrt().min(1.0);
    if mag < 0.05 { 0.0 } else { mag }
}

pub fn maybe_birth_coactivation(
    field: &mut CommitmentField,
    locus_a: LocusId,
    locus_b: LocusId,
) -> Result<Option<(LocusId, f32)>, crate::commitment::CommitmentError> {
    if locus_a == locus_b { return Ok(None); }

    let fa = match field.effective_force(locus_a)? { Some(f) => f, None => return Ok(None) };
    let fb = match field.effective_force(locus_b)? { Some(f) => f, None => return Ok(None) };

    let force = co_occurrence_force(fa, fb);
    if force == 0.0 { return Ok(None); }

    let derived = derived_locus(locus_a, locus_b);
    match field.birth_or_reinforce(derived, force)? {
        Some(updated) => Ok(Some((derived, updated.net_force))),
        None => Ok(None),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pairing_is_order_independent() {
        assert_eq!(derived_locus(0xA1, 0xB2), derived_locus(0xB2, 0xA1));
    }

    #[test]
    fn explicit_pair_can_birth_one_higher_order_residual() {
        let mut f = CommitmentField::new(0.90);
        f.set_tick(10);
        f.birth(0xA1, 0.50).unwrap();
        f.birth(0xB2, 0.50).unwrap();
        let r = maybe_birth_coactivation(&mut f, 0xA1, 0xB2).unwrap().unwrap();
        assert!(r.1 > 0.0);
        assert!(f.contains(r.0));
    }

    #[test]
    fn weak_forces_do_not_birth() {
        let mut f = CommitmentField::new(0.90);
        f.set_tick(1);
        f.birth(0xA1, 0.01).unwrap();
        f.birth(0xB2, 0.01).unwrap();
        assert!(maybe_birth_coactivation(&mut f, 0xA1, 0xB2).unwrap().is_none());
    }
}
