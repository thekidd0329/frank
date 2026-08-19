# Frank Android Compatibility Control Plane

This directory is the first executable slice of the Frank operating-system architecture.
It does **not** make Frank an AccessibilityService, VPN app, or root-capable LLM. Instead it models the intended system boundary:

- cognition proposes actions;
- deterministic Rust policy decides capabilities;
- brokers own narrow operating-system powers;
- Android apps remain ordinary UID-scoped Android apps;
- trust-root state is isolated behind a tiny interface intended to move to AVF/pKVM later.

## Run locally

```bash
cargo test --manifest-path platform/android-compat/Cargo.toml
cargo run --manifest-path platform/android-compat/Cargo.toml --bin frankd
```

The standalone crate uses only the Rust standard library so it can run in CI without Android dependencies.

## Components

- `policy.rs` — default-deny capability engine. It includes immutable denials so the cognitive process cannot grant itself authority.
- `network.rs` — UID-scoped network policy model with optional expiry. The AOSP implementation target is netd/eBPF rather than `VpnService`.
- `event.rs` — bounded normalized-event bus. Raw kernel/Binder noise should be reduced before cognition sees it.
- `trust.rs` — write-once trust-root abstraction. The in-memory implementation is only a test double; the target is an AVB/pKVM-backed implementation.
- `aosp/` — integration scaffold for Soong, init, SELinux, and Stable AIDL/Binder.

## Non-negotiable security rule

`frank-modeld` must never directly own Linux network administration, boot control, signing keys, SELinux policy, package-install authority, or trust-root write access. A compromised model may submit a request; it must not manufacture an operating-system permission.

## Next integration milestones

1. Build the crate in Cuttlefish/AOSP with Soong.
2. Register `frank.policy` as a Binder service in a dedicated SELinux domain.
3. Split the demonstration process into `frank-modeld`, `frank-policyd`, and `frank-netd` domains.
4. Map `NetworkPolicy` to Android netd/eBPF UID rules.
5. Feed normalized package/process/network events into `EventBus`.
6. Run CTS regression subsets after every platform change.
7. Move only identity, policy roots, release hashes, rollback state, and audit roots into AVF/pKVM.
8. Sign production images with Frank-owned AVB keys and relock the boot chain.
