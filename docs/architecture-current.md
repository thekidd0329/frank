# Frank architecture — current contract

This document captures the implementation boundary after the Lexie benchmark and the multi-agent architecture review.

## Proven milestones

- Lexie benchmark passes with stale-evidence rejection, contradiction handling, tie-breaker behavior, and stopping logic.
- Python prototype remains the deterministic cognition/parsing benchmark.
- Kotlin core now carries the portable entity/memory/authority-facing architecture.

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

## Combing and promotion

Combing may observe authorized local material, but durable promotion is gated by evidence type:

- explicit owner statements may promote immediately;
- strong direct quotes may promote immediately;
- outbound observations require repetition;
- passive/inbound observations require three timestamps;
- drafts/clippings have zero promotion authority.

Ontology canonicalization happens only after the promotion gate clears.

## Contradictions

A persisted claim is updated rather than duplicated into permanent opposing claims. Bayesian odds updates move claim confidence. Claims entering the configured 40–60% deadband are marked `CONTESTED` and should not drive autonomous side effects until resolved.

## Capabilities

Frank reasons about provider-agnostic capabilities, not app APIs. Capability metadata includes observable axes, entity-resolution outputs, uncertainty dimensions reduced, scopes, cost, latency, risk, availability, and side-effect classification.

## Autonomy

Autonomy is based on decision-relevant probabilities only. Relevant probabilities multiply; unrelated diagnostic certainty does not participate. Source exhaustiveness multiplies the joint result so a clean zero from one channel cannot masquerade as complete knowledge when other high-probability channels remain unchecked.

The default execution threshold remains 0.90.

## Action provenance and correction

Actions retain compact provenance: capability, timestamp, entity fragments, supporting claim IDs, decision confidence, outcome, and optional parent action. No raw source body is required.

A correction targets the exact claims that justified the action and demotes them through the claim-conflict tracker. This is the negative-learning path that prevents autonomous mistakes from silently reinforcing themselves over time.

## Deferred surfaces

The following remain intentionally present but behaviorally deferred rather than silently reinterpreted:

- `EntityAlias.strength`
- `EntityResolver.contextEntityIds`
- `CompactMemoryEntityStore.nowMillis` / claim `lastSeenMillis` interaction

These should receive behavior only through a later reviewed contract change.
