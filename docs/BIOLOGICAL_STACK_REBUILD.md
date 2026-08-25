# Frank Biological Stack Rebuild

## Governing rule

Biology is the reference architecture. Existing software patterns are not preserved merely because they are conventional. Any state, boundary, persistence rule, memory transition, or control mechanism must justify itself against the biological process it is intended to model.

This rebuild treats the existing Kotlin cognition and C-phi-plus work as observations from two abstraction levels, not as competing runtimes.

## Stack

### Layer 0 — C-phi-plus substrate

Models local, non-semantic dynamics:

- continuously valued nonzero trace state
- excitation/evidence
- local inhibition and opponent competition
- firing eligibility distinct from evidence
- refractory behavior
- supported versus unsupported decay
- dormancy without erasure
- reactivation from retained subthreshold state
- compartment/locality constraints

Layer 0 does **not** own semantic labels, beliefs, goals, entities, language, or long-term autobiographical truth.

### Layer 1 — Transient ensemble / working state

Models the biological fact that working memory can be carried by both active firing and activity-silent short-lived synaptic state.

A transient ensemble is a population of substrate traces plus their local relationships. It may be:

- actively firing
- subthreshold but recently potentiated
- inhibited by a competitor
- dormant but rapidly recoverable

There is no `saveWorkingMemory()` operation.

### Layer 2 — Tagging / eligibility for consolidation

Recent activation may leave a temporary consolidation tag. A tag is not yet long-term memory.

A tag records only what the substrate can lawfully support, such as:

- locus/ensemble identity
- residual strength
- contextual co-activation/binding evidence
- recency/persistence
- whether the trace survived competition
- whether later replay/reactivation re-engaged it

Weakly activated material can remain transient and disappear from active projections without being promoted.

### Layer 3 — Replay and consolidation

Offline and quiet-wake replay reactivates selected traces. Consolidation is earned through repeated successful reactivation and stabilization, not through a direct software write.

Candidate long-term memories are selected by biology-inspired factors:

- recurrence/replay
- prediction usefulness
- contextual reinstatement
- competition survival
- stability over time
- successful reconstruction after interruption

Sleep/offline phases may change the representation. Consolidation is therefore transformation, not byte-for-byte copying.

### Layer 4 — Residual Commitment Field

The Residual Commitment Field becomes the durable cognitive projection of consolidated lower-layer history.

It is not an independent second ground truth.

A Residual Commitment must be derivable from consolidated substrate history and sufficient to re-bias/reseed the lower layers during retrieval.

Long-term memory write path:

`experience -> local C-phi-plus dynamics -> transient ensemble -> temporary tag -> replay/reinstatement -> consolidation -> Residual Commitment`

Retrieval path:

`Residual Commitment -> contextual cue -> lower-layer bias/reinstatement -> transient ensemble -> active projection`

### Layer 5 — Semantic projections

Beliefs, goals, identity, relations, symbols, and language remain reconstructable projections over the durable field and current lower-layer state.

They are not permitted to become a hidden second memory database.

## Signed state versus opponent pathways

Signed residuals at the semantic layer and opponent pathways at the substrate layer are not competing models.

A higher-layer signed quantity may be reconstructed from lower-layer opponent activity:

`effective_signed_state = positive_path_strength - opponent_path_strength`

The lower layer therefore retains both histories while the upper layer may expose a compact signed projection when useful.

## Memory classes are states, not storage products

The architecture does not create separate software databases named short-term memory and long-term memory.

Instead, memory class is determined by dynamical state:

- immediate sensory/transient: newly driven, weakly stabilized
- working: active or activity-silent but rapidly reinstatable
- tagged: eligible for later replay/consolidation
- consolidating: repeatedly replayed/reconstructed
- durable: represented in Residual Commitment Field
- dormant durable: not currently active but recoverable from durable state

## Reconstruction invariant

A durable cognitive state passes only if:

1. derived semantic projections can be deleted,
2. transient working activation can be deleted,
3. the Residual Commitment Field plus lawful substrate rules can reconstruct the relevant cognitive projection,
4. no hidden duplicate cognitive truth is required.

## Anti-conventionality rule

Do not introduce a mechanism because it resembles a standard AI, database, cache, vector store, neural-network, operating-system, or application architecture.

If the design starts converging toward a familiar software architecture, stop and ask which biological process is actually being modeled.

Similarity to existing software is acceptable only when the biological mechanism independently demands the same structure.

## Immediate implementation order

1. Preserve C-phi-plus as the lower dynamical substrate.
2. Define a neutral transient-ensemble interface above individual traces.
3. Add temporary consolidation tags without making them durable truth.
4. Implement replay/reinstatement as reactivation of existing traces, not copying.
5. Derive Residual Commitments from consolidated lower-layer history.
6. Make retrieval reseed/bias lower-layer ensembles.
7. Make Kotlin semantic state reconstructable from that stack.
8. Run destruction/reconstruction tests at every layer boundary.
9. Keep GoldenRatioExperiment excluded from convergence controls when testing independent emergence.
