# Frank Android Compatibility Architecture

## Goal

Frank should behave like an independent operating-system control plane while preserving Android application compatibility. Android remains the compatibility/data plane; Frank owns local cognition, policy, memory, network authority, observability, identity, and audit. The language model is not the security boundary.

## Four-plane model

### 1. Cognitive plane
Local model, memory, reasoning, UI, planning, and user-facing personality. This plane may be probabilistic and replaceable. It produces proposals, not privileged side effects.

### 2. Control plane
Deterministic Rust services such as `frank-policyd`, `frank-netd`, `frank-observerd`, and `frank-memoryd`. These services expose narrow Binder/AIDL contracts and run in separate SELinux domains.

### 3. Compatibility plane
AOSP framework, ART, package manager, Activity/Window infrastructure, media framework, and ordinary APKs. Preserve stock semantics wherever practical so CTS remains a meaningful compatibility metric.

### 4. Trust plane
Verified Boot, signed release metadata, rollback state, device/Frank identity, policy roots, and eventually a minimal AVF/pKVM protected workload. Heavy model inference does not belong in the protected VM.

## Authority flow

```text
Android app or Frank cognition
            |
            v
     capability request
            |
            v
      frank-policyd
      deterministic
            |
        allow/deny
            |
            v
 narrow broker (net/files/device)
            |
            v
 SELinux + kernel enforcement
```

A prompt injection must be able to confuse cognition without becoming equivalent to root.

## Network architecture

Do not use `VpnService` as the final firewall. The target is UID-aware policy integrated with Android/Linux networking and netd/eBPF enforcement.

Policy is represented independently of the model, for example:

```text
uid 10342
internet       allow
local_network  deny
background     deny
metered        allow
expires        optional
```

The model may explain or propose a policy change. Only the deterministic policy service and network broker may apply it.

## Observability / machine proprioception

Avoid treating AccessibilityService as Frank's primary eyes or motor cortex. The OS build should normalize system-level events such as process lifecycle, foreground package changes, network attempts, display state, audio sessions, Bluetooth state, and later carefully selected Binder/eBPF observations.

Raw telemetry should not be dumped directly into an LLM. A bounded normalizer/event bus should translate it into semantic state first.

## Android compatibility strategy

Preserve Treble/VINTF/vendor boundaries and reuse existing vendor HALs and firmware. Frank should avoid becoming responsible for modem, camera, GPU, Wi-Fi, Bluetooth, sensor, and DRM implementations unless absolutely necessary.

Track compatibility as separate tiers:

1. Ordinary APK compatibility.
2. Android framework/CTS compatibility.
3. Vendor/VTS compatibility.
4. Google-service-dependent application compatibility.
5. Strong-attestation, banking, Wallet, and DRM-sensitive applications.

Tier 5 is not a prototype-one success criterion.

## AOSP implementation target

Initial development should use Cuttlefish or another AOSP development target before physical hardware. The first meaningful demonstration is an unchanged Android APK whose network authority is controlled by a Frank Rust policy service beneath the app layer.

Suggested system services:

```text
frank-modeld      cognition only
frank-policyd     capability decisions
frank-netd        network enforcement broker
frank-observerd   normalized event source
frank-memoryd     semantic memory boundary
frank-trustd      narrow bridge to protected trust state
```

Each service should receive a dedicated SELinux domain and the minimum Binder/service-manager permissions required for its job.

## AVF / pKVM role

Use protected virtualization for the smallest trust-critical state, not for the whole assistant. Candidate protected state includes device identity, Frank identity, policy-root hashes, release/model integrity hashes, rollback counters, signing verification state, and audit-chain roots.

The host Android/Frank cognitive world should be treated as potentially compromisable without automatically exposing this protected state.

## Verified production chain

Development can use an unlocked bootloader and userdebug builds. A production Frank device should move toward a locked boot chain, Frank-owned AVB signing keys, rollback protection, signed policy, and A/B updates. Permanent root is a development convenience, not the target trust model.

## Android 17 pressure

Android 17 reinforces this design by making ordinary apps a worse place to emulate an operating system: background audio is more constrained, accessibility is more tightly policed in high-protection modes, runtime memory is more explicitly governed, and static-final runtime invariants are hardened. Those restrictions should be treated as a reason to move Frank into the platform rather than to build increasingly fragile permission workarounds.

## Prototype acceptance criteria

The first architecture milestone passes when all of the following are true:

- an unchanged Android app runs normally;
- its UID receives default-deny Frank network policy;
- `frank-policyd` can grant/revoke one narrow network capability;
- the cognitive component cannot alter network policy directly;
- SELinux prevents unauthorized Binder/service access;
- normalized events are bounded before cognition consumes them;
- a policy/trust value marked immutable cannot be silently overwritten;
- Frank tests pass and Android compatibility regressions are measurable separately.
