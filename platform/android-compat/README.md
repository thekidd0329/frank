# Frank Android Compatibility Control Plane

This directory is the executable slice of Frank's Android-compatible **OS control plane**. It is not Frank's mind.

Frank's cognitive ground truth remains the existing Kotlin `frank.cognition` Residual Commitment Field. The Rust code here is intentionally limited to deterministic OS authority: policy, action routing, UID-scoped networking, normalized events, and trust-root integration.

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
- `action.rs` — policy/consent-gated backend-neutral action router.
- `network.rs` — UID-scoped network policy model, targeting AOSP netd/eBPF integration.
- `event.rs` — bounded normalized event bus.
- `trust.rs` — tiny trust-root abstraction intended to move behind AVB/AVF/pKVM.
- `aosp/` — Soong, init, SELinux, AIDL/Binder, and Android-17 action-backend integration scaffold.

The action path is deliberately layered:

```text
Kotlin RCF / goal-control
        ↓ action proposal
Rust policy + consent gate
        ↓
AppFunctions      Computer Control      Native broker
(structured)      (UI fallback)         (OS capability)
        ↓                  ↓                    ↓
              normal Android platform
```

`frank-modeld` should normally hold only `ProposeAction`. The privileged executor (`frank-actiond`) receives separate capabilities for AppFunctions, Computer Control, and native brokers. A model prompt therefore cannot grant itself operating-system authority.

If a target app needs networking, the action may carry an explicit UID-scoped `NetworkNeed`. The router verifies that capability but never grants it implicitly; temporary policy changes remain the job of the network-policy service.

Run the Rust control-plane prototype independently with:

```bash
cargo test --manifest-path platform/android-compat/Cargo.toml
cargo run --manifest-path platform/android-compat/Cargo.toml --bin frankd
cargo run --manifest-path platform/android-compat/Cargo.toml --bin frank-actiond
```

See `aosp/ACTION_BACKENDS.md` for the Android 17 AppFunctions / Computer Control adapter contract and user-takeover/consent rules.

## Non-negotiable boundary

The cognitive process may learn, decay, contradict, project, ask, and propose. It must not directly own Computer Control permission/AppOp state, Linux network administration, boot control, signing keys, SELinux policy, package-install authority, or trust-root write access. Evidence may change belief; it cannot promote itself into authority.
