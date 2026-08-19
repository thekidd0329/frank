# Frank architecture — current contract

This document captures the implementation boundary after the Lexie benchmark and the multi-agent architecture review.

## Proven milestones

- Lexie benchmark passes with stale-evidence rejection, contradiction handling, tie-breaker behavior, and stopping logic.
- The Python prototype remains a historical/reference test harness only; it is not Frank's cognitive runtime or source of cognitive authority.
- Kotlin now carries the portable semantic reference for entity, memory, autonomy, and Residual Commitment behavior.
- Rust is the target ground runtime for the persistent cognitive substrate; packing and persistence details remain intentionally unfrozen until Candidate A is pressure-tested.

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

The Kotlin reference implementation lives under `frank.cognition`. `docs/RESIDUAL_COMMITMENT.md` defines the reconstruction invariant and `docs/NEWBORN_PRIMITIVE.md` defines the pre-language degradation rule. A storage-layout-agnostic Rust semantic sketch lives under `prototype/rust` until the 128-bit Candidate A layout is frozen.

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

## Autonomy

Autonomy is based on decision-relevant probabilities only. Relevant probabilities multiply; unrelated diagnostic certainty does not participate. Source exhaustiveness multiplies the joint result so a clean zero from one channel cannot masquerade as complete knowledge when other high-probability channels remain unchecked.

The default execution threshold remains 0.90. Contested Residual Commitments are suppressed from autonomous decision confidence in the current Kotlin reference implementation.

## Action provenance and correction

Actions retain compact provenance: capability, timestamp, entity fragments, supporting claim IDs, decision confidence, outcome, and optional parent action. No raw source body is required.

A correction targets the exact claims that justified the action and demotes them through the claim-conflict tracker. This is the negative-learning path that prevents autonomous mistakes from silently reinforcing themselves over time.

Residual Commitment provenance remains optional because provenance payloads are secondary evidence records, not the ground atom itself.

## Deferred surfaces

The following remain intentionally present but behaviorally deferred rather than silently reinterpreted:

- `EntityAlias.strength`
- `EntityResolver.contextEntityIds`
- `CompactMemoryEntityStore.nowMillis` / compact claim `lastSeenMillis` interaction
- exact 128-bit Residual Commitment field widths and locus addressing beyond the current `(axisId,valueId)` compatibility bridge
- mmap / generation-counter persistence protocol
- persistent secondary-index selection

These should receive behavior only through a later reviewed contract change.
