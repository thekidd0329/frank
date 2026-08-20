# Frank architecture — current contract

This document captures the implementation boundary after the Lexie benchmark, the multi-agent architecture review, and the first Android-platform design pass.

## Proven milestones

- Lexie benchmark passes with stale-evidence rejection, contradiction handling, tie-breaker behavior, and stopping logic.
- The Python prototype remains a historical/reference test harness only; it is not Frank's cognitive runtime or source of cognitive authority.
- Kotlin now carries the portable semantic reference for entity, memory, autonomy, and Residual Commitment behavior.
- Rust remains available for later native ground/runtime work where it provides a concrete advantage; the newborn phase does not require a Rust rewrite merely to exist.

## Residual Commitment ground primitive

The persistent cognitive source of truth is the Residual Commitment Field. Beliefs, relations, goals, episodes, self-model surfaces, working activation sets, and secondary indices are projections or rebuildable caches.

Hard reconstruction invariant:

> If Frank loses every derived structure but retains the Residual Commitment Field, he must be able to reconstruct a coherent cognitive state without hidden external state.

The newborn ground rule is intentionally smaller than language or personality:

- reinforcement preserves or strengthens residual force;
- time without reinforcement reduces residual force toward zero;
- passive decay never flips polarity by itself;
- contradictory evidence supplies directional pressure and may change the surviving polarity;
- no LLM is the ground cognitive authority.

The Kotlin reference implementation lives under `frank.cognition`. `docs/RESIDUAL_COMMITMENT.md` defines the reconstruction invariant and `docs/NEWBORN_PRIMITIVE.md` defines the pre-language degradation rule. Storage layout and native-runtime decisions remain subordinate to the cognitive contract rather than defining it.

## Entity resolution

Entity cognition uses canonical `EntityId` values and full ranked `EntityHypothesisSet` results. Platform-specific IDs stay inside provider adapters.

The supplied `EntityResolver` and `CompactMemoryEntityStore` are preserved as contract code. `EntityStoreIntegration` is the integration boundary that:

- prevents positive NAME-claim presence from becoming a blanket exact-name match;
- preserves alias-only candidate generation;
- expands the generic `parent` role hint to father/mother relationship claims;
- leaves deferred fields (`alias.strength`, `contextEntityIds`, `nowMillis/lastSeenMillis`) untouched.

## Compact memory

Durable claims contain numeric axis/value identity plus confidence, recency, polarity, scope, timestamps, and observation count. Raw source text is not retained in the durable claim.

`AdaptiveOntology` keeps axis IDs global and value IDs local to each axis. IDs are stable once assigned. Candidate ontology entries are proposed before canonicalization. Cheap normalized/trigram matching provides an alias path without requiring an embedding model for every candidate.

Compact claims remain an evidence/compatibility surface. `EvidenceToCommitment` bridges stable axis/value IDs into the Residual Commitment Field; compact claims are not promoted into a second competing source of cognitive truth.

## Combing and promotion

Combing may observe authorized local material, but durable promotion is gated by evidence type:

- explicit owner statements may promote immediately;
- strong direct quotes may promote immediately;
- outbound observations require repetition;
- passive/inbound observations require three timestamps;
- drafts/clippings have zero promotion authority.

Ontology canonicalization happens only after the promotion gate clears.

## Contradictions

A persisted claim is updated rather than duplicated into permanent opposing claims. Bayesian odds updates move compact-claim confidence. Claims entering the configured 40–60% deadband are marked `CONTESTED` and should not drive autonomous side effects until resolved.

At the Residual Commitment layer, opposite polarity applies directional pressure at the same locus. Residual force is the surviving magnitude of that competition; simple time decay is not contradiction and therefore cannot invert polarity.

## Capabilities

Frank reasons about provider-agnostic capabilities, not app APIs. Capability metadata includes observable axes, entity-resolution outputs, uncertainty dimensions reduced, scopes, cost, latency, risk, availability, and side-effect classification.

The mature Android action hierarchy is deterministic:

1. use a structured AppFunction when an allowed capability exists;
2. use Computer Control only when structured execution is unavailable and the target is eligible;
3. use a native Frank broker for operations that belong to the operating system itself;
4. otherwise block, defer, or request an alternate path.

Cognition chooses the desired outcome. The authority plane chooses the permitted execution mechanism.

## Autonomy

Autonomy is based on decision-relevant probabilities only. Relevant probabilities multiply; unrelated diagnostic certainty does not participate. Source exhaustiveness multiplies the joint result so a clean zero from one channel cannot masquerade as complete knowledge when other high-probability channels remain unchecked.

The default execution threshold remains 0.90. Contested Residual Commitments are suppressed from autonomous decision confidence in the current Kotlin reference implementation.

## Action provenance and correction

Actions retain compact provenance: capability, timestamp, entity fragments, supporting claim IDs, decision confidence, outcome, and optional parent action. No raw source body is required.

A correction targets the exact claims that justified the action and demotes them through the claim-conflict tracker. This is the negative-learning path that prevents autonomous mistakes from silently reinforcing themselves over time.

Residual Commitment provenance remains optional because provenance payloads are secondary evidence records, not the ground atom itself.

## Local model runtime boundary

Frank's local inference runtime is an execution organ, not Frank's cognitive identity.

The target process split is:

```text
frank-sensed
    normalized sensory input

frank-contextd
    device/world observations and live capabilities

frank-brain
    RCF, developmental cognition, goals, learning

frank-modeld
    replaceable local model execution across CPU/GPU/NPU

frank-policyd
    authority, consequence gating, capability policy

frank-actiond
    effects and post-action verification
```

`frank-brain` must not depend on accelerator-specific APIs. It requests bounded inference through a narrow model protocol. Model choice, vendor delegate choice, CPU/GPU/NPU routing, crashes, and runtime replacement must not migrate or redefine Frank's identity, memory, goals, or Residual Commitment state.

Executable inference code and model data are separate trust classes:

- model weights, tokenizers, and prototypes are replaceable data;
- runtime binaries, native delegates, and accelerator adapters are signed executable artifacts;
- downloaded mutable executable code is not loaded directly into cognition.

## IPC and large-data transport

Binder/AIDL is the control plane. Shared memory is the high-volume data plane.

Large tensors, audio windows, image buffers, and similar payloads should be passed by file descriptor/shared-memory region with compact metadata over Binder rather than copied through Binder transactions. New code must not depend on ashmem-specific behavior; the implementation should remain compatible with the platform's memfd/`ASharedMemory` direction.

## Page-size contract

Frank-owned native code must be page-size agnostic. No code may assume a 4096-byte page.

Native release checks should include:

- 4 KiB and 16 KiB page-size execution where applicable;
- ELF alignment validation;
- mmap/shared-memory tests using runtime page size;
- representative integration testing on a 16 KiB Android target before production release.

This applies especially to model mappings, arenas, tensor buffers, residual storage, ring buffers, and shared-memory IPC.

## Android platform boundary

Frank OS should preserve Android's solved compatibility infrastructure wherever possible:

```text
/system       near-stock AOSP
/system_ext   only unavoidable deep framework coupling
/product      Frank UX and product configuration
/vendor       hardware-specific implementation
```

The long-term authority layer should sit behind stable IPC and be independently replaceable where Android's modularity mechanisms permit it. App sandboxing, PackageManager semantics, verified boot, OTA machinery, Treble/VINTF boundaries, and ordinary APK behavior are infrastructure to preserve rather than reinvent.

Frank gains authority as a trusted platform component above the normal UID sandbox; normal applications do not inherit Frank's privilege domain.

## Kernel and hardware-interface policy

A custom Frank kernel is not a newborn requirement. Prefer Android's GKI/vendor-module model and existing platform networking primitives before introducing kernel forks.

For network filtering, accounting, and policy enforcement, prefer Android networking policy and supported eBPF/netd mechanisms unless a demonstrated missing primitive requires deeper work.

For hardware integration:

- use an existing AOSP HAL unchanged when it already expresses the needed capability;
- use a stable AIDL extension when a vendor capability must extend an existing contract;
- define a new stable AIDL HAL for genuinely Frank-specific hardware;
- do not casually fork upstream AOSP HAL contracts to add Frank-only methods.

## Observation and authority symmetry

Frank does not receive universal passive visibility merely because he is the assistant, and cognition does not receive direct machine authority merely because it produced an intention.

```text
Android/device/app state
        ↓
context broker + scope/policy filter
        ↓
Frank cognition
        ↓
policy/consequence/capability gate
        ↓
Android effects
```

Nothing enters cognition with automatic trust, and nothing leaves cognition with automatic authority.

## Deferred surfaces

The following remain intentionally present but behaviorally deferred rather than silently reinterpreted:

- `EntityAlias.strength`
- `EntityResolver.contextEntityIds`
- `CompactMemoryEntityStore.nowMillis` / compact claim `lastSeenMillis` interaction
- exact packed Residual Commitment field widths and locus addressing beyond the current `(axisId,valueId)` compatibility bridge
- mmap / generation-counter persistence protocol
- persistent secondary-index selection
- Android APEX/service packaging details until the first platform integration branch needs them
- NPU/vendor accelerator implementation until a concrete target device requires it
- pKVM/AVF trust-core implementation until Frank's host-side identity and update contracts are stable

These should receive behavior only through a later reviewed contract change.
