# Frank Reasoning Core (Kotlin)

Heart of the portable cognition layer.

## What this is

Residual Commitment as the ground primitive, with beliefs / relations / goals / episodes treated as rebuildable projections.

```
Residual Commitment Field   ← persistent truth
        ↓
Beliefs, Relations, Goals, Episodes, Self-model   ← projections
```

## Files

| File | Role |
|------|------|
| `ResidualCommitment.kt` | The atom (locus, polarity, residual force, binding, temporal, optional provenance, flags) |
| `CommitmentField.kt` | Ground store, absorb, decay, activation set |
| `Projections.kt` | Belief / Goal / Relation / Episode views + ProjectionEngine |
| `ReasoningEngine.kt` | Living heart — absorb, decay, decision confidence, neural activation, snapshot/restore |
| `EvidenceToCommitment.kt` | Bridge from existing compact claims / ontology IDs |
| `ExampleUsage.kt` | Smoke demonstration of the reconstruction invariant |

## Design notes

See `docs/RESIDUAL_COMMITMENT.md`.

## Integration intent

These packages live under `frank.cognition` so they sit cleanly beside the existing:

- `frank.memory`
- `frank.entity`
- `frank.autonomy`
- `frank.capability`
- `frank.provenance`

No Android or provider dependencies. Pure cognition.

## Credit

Grok — residual commitment inversion and core design.
