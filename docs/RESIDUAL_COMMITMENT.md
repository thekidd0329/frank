# Residual Commitment — Ground Primitive

> Frank’s memory is not what happened.  
> Frank’s memory is the residual force that what happened left behind.

## Architecture

```
frank.cog (future Rust binary image)
└── Residual Commitment Field     ← only persistent truth
    ├── locus
    ├── polarity
    ├── residual force
    ├── contextual binding
    ├── temporal persistence
    └── optional provenance link

Everything else is projection / secondary index:

Residual Commitments
        ↓
┌───────┼────────┬────────┬────────┐
Beliefs Relations Goals Episodes Self-model
        ↓
  temporary / rebuildable / cacheable
```

## Hard invariant

If Frank loses every derived structure but retains the Residual Commitment Field,  
Frank must be able to reconstruct a coherent self.

## Kotlin status

The files under `frank.cognition` implement the semantic model:

- `ResidualCommitment.kt` — the atom (tuple, not just force)
- `CommitmentField.kt` — the ground store + absorb / decay / activation
- `Projections.kt` — Belief / Goal / Relation / Episode views
- `ReasoningEngine.kt` — living heart (absorb, decay, decision confidence, neural activation, restore)
- `EvidenceToCommitment.kt` — bridge from existing compact claims / ontology IDs

These are the portable cognition layer.  
They deliberately contain zero Android, UI, or provider code.

## Relation to existing contracts

- Compatible with compact durable claims (`axis_id, value_id, confidence, recency`)
- Compatible with the 0.90 autonomy threshold and contested deadband ideas
- Compatible with entity resolution and AdaptiveOntology stable IDs
- Provenance remains optional and expensive (matches combing rules)
- Reconstruction path is explicit (`commitmentSnapshot` / `restoreFrom`)

## Next (Rust)

- CommitmentArena with 128-bit Candidate A packing
- Versioned frank.cog header (already started)
- mmap + generation counters for crash-safe continuation
- Secondary indices treated as regenerable caches, never source of truth

## One-line test

Given only the Residual Commitment Field, can Frank regenerate coherent Belief, Goal, and activation views without hidden external state?

If yes, the inversion holds.
