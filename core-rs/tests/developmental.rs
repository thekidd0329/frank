//! Developmental pressure tests + Claude source-verified simulation re-run.

use frank_core::*;

const R: f32 = 0.90;
const EPS: f32 = 1e-5;

#[test]
fn claude_source_verified_trace() {
    let mut f = CommitmentField::new(R);
    f.set_tick(1);
    f.birth(0xA1, 0.40).unwrap();
    let a = f.snapshot()[0];
    assert!((a.net_force - 0.40).abs() < EPS);
    assert_eq!(a.last_updated_tick, 1);

    f.set_tick(2);
    let a = f.apply_evidence(0xA1, 0.30).unwrap().unwrap();
    assert!((a.net_force - 0.66).abs() < EPS);
    assert_eq!(a.last_updated_tick, 2);

    f.set_tick(5);
    let a = f.materialize(0xA1).unwrap().unwrap();
    assert!((a.net_force - 0.48114).abs() < 1e-4);
    assert_eq!(a.last_updated_tick, 5);

    f.set_tick(6);
    let a = f.apply_evidence(0xA1, -0.90).unwrap().unwrap();
    assert!((a.net_force + 0.466974).abs() < 1e-4);
    assert_eq!(a.last_updated_tick, 6);

    f.set_tick(7);
    f.birth(0xB2, 0.80).unwrap();
    assert_eq!(f.len(), 2);

    f.set_tick(10);
    let a = f.apply_evidence(0xA1, 0.50).unwrap().unwrap();
    assert!((a.net_force - 0.194).abs() < 0.01);
    let b = f.apply_evidence(0xB2, 0.50).unwrap().unwrap();
    assert!((b.net_force - 1.0).abs() < EPS);
    assert_eq!(b.last_updated_tick, 10);

    let derived = maybe_birth_coactivation(&mut f, 0xA1, 0xB2).unwrap();
    assert!(derived.is_some());
    assert_eq!(f.len(), 3);

    f.set_tick(11);
    let _ = f.materialize(0xA1).unwrap();
    let b = f.materialize(0xB2).unwrap().unwrap();
    assert!((b.net_force - 0.9).abs() < 0.01);
}

#[test]
fn clamp_collapses_distinct_histories() {
    let mut f1 = CommitmentField::new(1.0);
    f1.set_tick(1);
    f1.birth(0xC1, 0.70).unwrap();
    f1.apply_evidence(0xC1, 0.50).unwrap();

    let mut f2 = CommitmentField::new(1.0);
    f2.set_tick(1);
    f2.birth(0xC1, 0.90).unwrap();
    f2.apply_evidence(0xC1, 0.40).unwrap();

    let a1 = f1.snapshot()[0];
    let a2 = f2.snapshot()[0];
    assert_eq!(a1.net_force, a2.net_force);
    assert_eq!(a1.last_updated_tick, a2.last_updated_tick);
}

#[test]
fn first_locus_stability() {
    let mut f = CommitmentField::new(R);
    let cfg = AllocatorConfig;
    let obs = b"raw sensory spike alpha";

    f.set_tick(1);
    let r1 = observe(&mut f, obs, 0.55, None, &cfg).unwrap().unwrap();
    let first = r1.locus;

    f.tick();
    f.tick();
    let r2 = observe(&mut f, obs, 0.30, None, &cfg).unwrap().unwrap();
    assert_eq!(r2.locus, first);
    assert!(f.contains(first));
    assert!(r2.net_force > 0.55);
}

#[test]
fn divergent_streams() {
    let cfg = AllocatorConfig;
    let mut fa = CommitmentField::new(R);
    let mut fb = CommitmentField::new(R);

    let stream_a = [b"pattern-A-1".as_slice(), b"pattern-A-2", b"pattern-A-1"];
    let stream_b = [b"pattern-B-1".as_slice(), b"pattern-B-2", b"pattern-B-3"];

    for (i, obs) in stream_a.iter().enumerate() {
        fa.set_tick((i + 1) as u32);
        observe(&mut fa, obs, 0.60, None, &cfg).unwrap().unwrap();
    }
    for (i, obs) in stream_b.iter().enumerate() {
        fb.set_tick((i + 1) as u32);
        observe(&mut fb, obs, 0.60, None, &cfg).unwrap().unwrap();
    }

    let loci_a: Vec<_> = fa.loci().collect();
    let loci_b: Vec<_> = fb.loci().collect();
    assert_ne!(loci_a, loci_b);
}

#[test]
fn reconstruct_from_field_alone() {
    let mut f = CommitmentField::new(R);
    let cfg = AllocatorConfig;

    f.set_tick(1);
    let r1 = observe(&mut f, b"event-1", 0.70, None, &cfg).unwrap().unwrap();
    f.set_tick(2);
    let r2 = observe(&mut f, b"event-2", 0.65, Some(r1.locus), &cfg).unwrap().unwrap();
    assert!(r2.derived.is_some());

    let surviving = f.snapshot();
    let tick = f.current_tick;
    let mut rebuilt = CommitmentField::new(R);
    rebuilt.set_tick(tick);
    for c in surviving { rebuilt.insert_raw(c).unwrap(); }

    assert_eq!(rebuilt.len(), f.len());
    assert!(rebuilt.contains(r1.locus));
    assert!(rebuilt.contains(r2.locus));
    if let Some((d, _)) = r2.derived { assert!(rebuilt.contains(d)); }
}

#[test]
fn minimal_prediction_loop() {
    let mut f = CommitmentField::new(R);
    let cfg = AllocatorConfig;
    let pattern = b"repeating-motif";

    for t in 1..=5u32 {
        f.set_tick(t);
        observe(&mut f, pattern, 0.45, None, &cfg).unwrap().unwrap();
    }

    let locus = content_preferred_locus(pattern);
    f.set_tick(6);
    let predicted = f.effective_force(locus).unwrap().unwrap();
    assert!(predicted > 0.0);

    let after = observe(&mut f, pattern, 0.40, None, &cfg).unwrap().unwrap();
    assert!(after.net_force > predicted);

    f.tick();
    let anti = observe(&mut f, b"anti-motif", -0.70, None, &cfg).unwrap().unwrap();
    assert!(anti.net_force < 0.0);
    assert!(f.contains(locus));
}

#[test]
fn observation_exact_cancellation_prunes() {
    let mut f = CommitmentField::new(1.0);
    let cfg = AllocatorConfig;
    let obs = b"cancel-me";
    f.set_tick(1);
    let first = observe(&mut f, obs, 0.50, None, &cfg).unwrap().unwrap();
    assert!(f.contains(first.locus));
    let second = observe(&mut f, obs, -0.50, None, &cfg).unwrap();
    assert!(second.is_none());
    assert!(!f.contains(first.locus));
}

#[test]
fn sparse_and_prune() {
    let mut f = CommitmentField::new(0.5);
    f.set_tick(1);
    f.birth(0xDEAD, 0.10).unwrap();
    assert_eq!(f.len(), 1);

    f.set_tick(20);
    let m = f.materialize(0xDEAD).unwrap();
    assert!(m.is_none());
    assert!(f.is_empty());
}
