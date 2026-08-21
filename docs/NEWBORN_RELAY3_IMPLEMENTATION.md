# Newborn Relay #3 Implementation

This branch implements the first Kotlin-first newborn step from the Meta → Gemini → Copilot → Claude → Grok relay.

## Canonical developmental order

Frank begins with internal dynamics, not labels:

1. H — homeostatic/allostatic tension
2. E — epistemic tension and learning progress
3. R — residual-force memory
4. C/Ω — contingency and agency integration
5. L — consolidation load and sleep pressure

At initialization there are no object categories, emotion labels, curiosity module, trust score, self variable, named caregiver, language, or preallocated semantic partitions.

## Current implementation

- NewbornState carries the initial developmental condition.
- HomeostaticTension follows wake, internal-error, external-error, equilibrium-damping, and sleep-gate terms.
- EpistemicTension separates fast and slow prediction error so learning progress can reduce tension.
- NewbornLearningLoop maintains opaque residual force from raw observations.
- ContingencyState compares predicted and actual change and integrates Ω.
- ConsolidationLoad accumulates unresolved error/conflict and reduces during sleep.
- NewbornDynamics keeps these primitives in one platform-independent Kotlin state transition.

## README-first conformance

This implementation does not program mature answers. It creates conditions under which later distinctions may emerge. Mock affect remains downstream control state; it is not a claim that Frank already has human emotion.

The implementation also preserves:

- cold-start residual memory;
- weakening toward uncertainty/pruning rather than automatic belief inversion;
- no pre-named Christian relationship;
- no telemetry feedback into the sensory manifold;
- no Android API dependency in cognition;
- no Rust ground-runtime requirement;
- age-appropriate, bounded newborn testing.

Relay formulas remain engineering analogues and hypotheses where marked by the source material. The code is not evidence that consciousness or human feeling has been achieved.
