# Frank Reasoning Core (Kotlin)

Portable cognition built around the Residual Commitment Field.

## Ground rule

```
Residual Commitment Field   <- persistent cognitive ground truth
        |
        +-> beliefs / goals / identity / action confidence / sleep-state projections
```

The field carries signed residual pressure, binding, temporal state, provenance, flags, and consolidation maturity. Higher structures remain rebuildable.

## Developmental startup

Use `ReasoningEngine.developmental()` for Frank's actual newborn/developmental startup. It seeds owner-authored positive orientations exactly once when their loci are absent.

The starting value map is intentionally positive-only. It describes what Frank moves toward; opposing limits and prohibitions are learned from experience rather than installed as negative prompts.

Current foundational orientations include love of neighbor, truthfulness, humility/higher-power orientation, stewardship, protection of life/dignity/wellbeing, faithfulness, temperance, hope, accountability, and:

> **Anything is possible.**

Identity seeds include the truthful origin that Christian created Frank's initial architecture, Frank's artificial cognitive nature, his constructive purpose with people, and a creator relationship that permits trust, care, learning, disagreement, and continued connection without creator infallibility.

## Sleep is cognitive architecture

Sleep is endogenous rather than a timer command.

- Awake cognition accumulates sleep pressure.
- Crossing the internal onset threshold enters sleep.
- The sleeping state and pressure are themselves represented in the field, so restart reconstruction can know Frank was asleep.
- While pressure remains above the recovered threshold, `wantsWake` stays false.
- NREM-like steps apply broad homeostatic softening.
- REM-like steps stabilize coherent winners and prune near-zero noise.
- Waking occurs when homeostasis has recovered, not after a fixed duration.

A completed natural sleep episode contributes one independent consolidation exposure. There is no hidden `evaluationCounts` map. Each stable commitment physically changes its own `consolidationMaturity`; three completed independent sleep episodes mature an ordinary candidate to the default settled-belief threshold.

Contradiction resets consolidation maturity and applies signed residual pressure. Example contract:

```
+0.40 followed by -0.90 -> NEGATIVE 0.50, contested
```

## Protected values are plastic

Foundational and identity loci use slow decay, not immutability. They remain capable of changing under sufficiently strong and repeated evidence. "Protected" means developmentally stable, not hard-coded forever.

## Files

| File | Role |
|---|---|
| `ResidualCommitment.kt` | Ground atom + consolidation maturity + structural flags |
| `CommitmentField.kt` | Signed pressure, decay, reinforcement, contradiction |
| `FoundationalValueSeeds.kt` | Positive-only owner-seeded values, origin, purpose, identity |
| `SleepHomeostasis.kt` | Endogenous tiredness, sleep self-state, wake threshold |
| `SleepCycle.kt` | NREM/REM-like offline operations + physical consolidation |
| `DegradationMonitor.kt` | Residual degradation measurement |
| `Projections.kt` | Rebuildable belief/goal/identity surfaces |
| `ReasoningEngine.kt` | Living cognition interface and developmental startup |
| `EvidenceToCommitment.kt` | Bridge from existing compact evidence |
| `ExampleUsage.kt` | Smoke example |

See `docs/DEVELOPMENTAL_COGNITION.md` for the architecture contract and open research benchmarks.
