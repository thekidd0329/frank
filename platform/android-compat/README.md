# Frank Android Compatibility Control Plane

This directory is the executable slice of Frank's Android-compatible **OS control plane**. It is not Frank's mind.

Frank's cognitive ground truth remains the existing Kotlin `frank.cognition` Residual Commitment Field. The Rust code here is intentionally limited to deterministic OS authority: policy, UID-scoped networking, normalized events, and trust-root integration.

## Newborn teacher on Linux

From the repository root:

```bash
bash run-frank.sh --field ~/.frank/newborn.cog
```

`run-frank.sh` compiles the existing Kotlin source tree and starts `frank.cli.TeachMain`. No second Python/Rust memory chatbot is involved.

The terminal is only teacher I/O over existing cognition:

```text
teacher evidence
    -> EvidenceToCommitment
    -> ReasoningEngine.absorb(...)
    -> CommitmentField
    -> rebuildable projections
```

Plain text and `/say` are journaled as `USERASSERTED` evidence only and **do not alter the Residual Commitment Field**. The field changes only through explicit absorb/reinforce/contradict paths.

Commands:

```text
/observe <axis> <value> [force] [polarity]
/say <free text>
/reinforce <axis> <value> [force]
/contradict <axis> <value> [force]
/tick [n]
/field
/project
/gaps
/ask
/restore
/quit
```

`/gaps` finds weak or contested residual commitments. `/ask` (and state-changing teaching commands) select the next question target from those field gaps. The target comes from cognition; wording is a thin deterministic surface and is never ground truth.

`/restore` exercises the reconstruction invariant: snapshot the Residual Commitment Field, rebuild the engine from that field, and fail if the activation set changes.

### Important locus rule

The existing bridge addresses a locus by `(axisId, valueId)`. Therefore `teacher.work=music` and `teacher.work=design` are separate loci. Opposing pressure must target the same axis/value locus unless a future exclusivity/competition projection explicitly relates alternatives.

The teacher CLI uses deterministic IDs derived from the teacher-provided English labels. Those labels are acknowledged scaffolding for this development phase, not evidence that Frank learned English ontology from nothing.

## Android control-plane prototype

The Rust crate remains separate from cognition:

- `policy.rs` — default-deny capability engine; cognition cannot grant itself authority.
- `network.rs` — UID-scoped network policy model, targeting AOSP netd/eBPF integration.
- `event.rs` — bounded normalized event bus.
- `trust.rs` — tiny trust-root abstraction intended to move behind AVB/AVF/pKVM.
- `aosp/` — Soong, init, SELinux, and Stable AIDL/Binder integration scaffold.

Run it independently with:

```bash
cargo test --manifest-path platform/android-compat/Cargo.toml
cargo run --manifest-path platform/android-compat/Cargo.toml --bin frankd
```

## Non-negotiable boundary

The cognitive process may learn, decay, contradict, project, and ask. It must not directly own Linux network administration, boot control, signing keys, SELinux policy, package-install authority, or trust-root write access. Evidence may change belief; it cannot promote itself into authority.
