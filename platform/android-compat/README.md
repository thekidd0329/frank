# Frank Android Compatibility Control Plane

This directory is the first executable slice of the Frank operating-system architecture.
It does **not** make Frank an AccessibilityService, VPN app, or root-capable LLM. Instead it models the intended system boundary:

- cognition proposes actions;
- deterministic Rust policy decides capabilities;
- brokers own narrow operating-system powers;
- Android apps remain ordinary UID-scoped Android apps;
- trust-root state is isolated behind a tiny interface intended to move to AVF/pKVM later;
- conversational memory and curiosity stay local and do not gain privileged OS authority.

## Talk to Frank on Linux

From the repository root:

```bash
bash run-frank.sh
```

Or directly:

```bash
cargo run --manifest-path platform/android-compat/Cargo.toml --bin frank
```

Frank stores learned memory at `~/.frank/memory.tsv`. Set `FRANK_HOME=/some/path` to use a different local memory directory.

There is no cloud model or remote API dependency in this prototype. The conversation loop is a local symbolic learning system: it extracts explicit facts, notices unresolved concepts in what the user says, ranks those knowledge gaps, and generates its next question from the highest-ranked unresolved gap. There is deliberately **no canned question list**.

Example behavior:

```text
You: I was talking to Akira about Merkaba.
Frank: Okay. What is Akira?
You: Akira is someone extremely important to me.
Frank: Got it. I saved how akira relates to you. What is Merkaba?
```

The exact question depends on what Frank already knows. Once a concept has been answered, its knowledge gap is closed and it is not repeatedly asked as a first-time question.

Terminal commands:

```text
/memory   show learned facts
/pending  show the current unresolved question
/forget   erase Frank's local learned memory
/help     show commands
/quit     save and exit
```

## Run tests

```bash
cargo test --manifest-path platform/android-compat/Cargo.toml
cargo run --manifest-path platform/android-compat/Cargo.toml --bin frankd
```

The standalone crate uses only the Rust standard library so the core prototype can compile without Android or third-party Rust dependencies.

## Components

- `conversation.rs` — interactive turn engine. Answers from Frank's own questions become new memory.
- `curiosity.rs` — derives questions from unresolved names, concepts, and thin relationships in the current conversation. No interview script is embedded.
- `memory.rs` — persistent local fact/concept memory using a dependency-free disk format.
- `policy.rs` — default-deny capability engine. It includes immutable denials so the cognitive process cannot grant itself authority.
- `network.rs` — UID-scoped network policy model with optional expiry. The AOSP implementation target is netd/eBPF rather than `VpnService`.
- `event.rs` — bounded normalized-event bus. Raw kernel/Binder noise should be reduced before cognition sees it.
- `trust.rs` — write-once trust-root abstraction. The in-memory implementation is only a test double; the target is an AVB/pKVM-backed implementation.
- `aosp/` — integration scaffold for Soong, init, SELinux, and Stable AIDL/Binder.

## Non-negotiable security rule

`frank-modeld` must never directly own Linux network administration, boot control, signing keys, SELinux policy, package-install authority, or trust-root write access. A compromised model may submit a request; it must not manufacture an operating-system permission.

Conversational curiosity is intentionally outside that authority boundary. Frank is free to wonder, ask, learn, forget, and revise cognitive state; privileged actions still pass through deterministic policy.

## Next integration milestones

1. Build the crate in Cuttlefish/AOSP with Soong.
2. Register `frank.policy` as a Binder service in a dedicated SELinux domain.
3. Split the demonstration process into `frank-modeld`, `frank-policyd`, and `frank-netd` domains.
4. Map `NetworkPolicy` to Android netd/eBPF UID rules.
5. Feed normalized package/process/network events into `EventBus`.
6. Run CTS regression subsets after every platform change.
7. Move only identity, policy roots, release hashes, rollback state, and audit roots into AVF/pKVM.
8. Sign production images with Frank-owned AVB keys and relock the boot chain.
