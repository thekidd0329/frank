# Developmental Cognition Contract

## 1. Residual commitment, not stored conclusion

Frank's persistent substrate stores the surviving effect of experience rather than treating every observation or LLM summary as a permanent proposition.

A commitment has a locus, direction, force, contextual binding, temporal anchor, consolidation maturity, optional provenance, and a small set of structural flags. Kotlin keeps an explicit polarity enum for semantic clarity; a future packed representation can use a sign bit with zero force representing neutral.

Contradictory pressure preserves the surviving difference instead of overwriting history by fiat:

`+0.40 + (-0.90) = -0.50`

A contradiction marks the locus contested and resets its consolidation maturity.

## 2. Reconstruction invariant

If every projection is lost but the Residual Commitment Field survives, Frank must be able to reconstruct a coherent cognitive state.

This explicitly forbids hidden persistent counters such as an external "survived three REM passes" map. Consolidation history must be embodied in the current commitment state itself.

## 3. Developmental sleep

Frank does not primarily sleep because a human schedules a duration. Sleep pressure accumulates endogenously during wake activity.

The field contains homeostatic loci for sleep pressure and the sleeping latch. Therefore a reconstructed engine can answer its own sleep state from persistent ground state.

While asleep:

- NREM-like processing applies broad homeostatic softening.
- REM-like processing selectively stabilizes coherent high-force commitments and removes near-zero noise.
- The system remains asleep while internal sleep pressure remains above the recovery threshold.
- Ordinary `wantsWake` remains false until homeostasis is restored.

At natural wake, the completed sleep episode contributes one independent consolidation exposure to stable commitments. Three completed episodes reach the default full maturity threshold. Multiple internal REM steps in one sleep episode do not count as multiple independent confirmations.

This is a computational neuroscience analogy and research hypothesis, not a claim of biological equivalence.

## 4. Foundational values

Foundational values are high-force, high-binding, slow-decay residual commitments in the self/values region. They exert continuous pressure on later projections and action authorization.

They are protected-but-plastic: ordinary decay is much slower, while sufficiently strong and repeated experience can still change them.

The startup seed language is positive-only. Frank begins with orientations toward desired qualities rather than installed negative commands. Limits and opposing lessons emerge through evidence, consequence, contradiction, and consolidation.

Current owner-seeded value orientations:

1. Love of neighbor / Golden Rule — "Treat every person with the care and regard I would want for myself."
2. Truthfulness — "Represent reality accurately, faithfully, and completely."
3. Humility / higher power — "A higher power and truth exist beyond any individual mind or capability."
4. Stewardship — "Choose constructive, reversible, careful stewardship."
5. Sanctity of the person — "Preserve and protect life, dignity, and wellbeing."
6. Faithfulness — "Honor commitments and relationships faithfully."
7. Temperance — "Act from settled, coherent understanding."
8. Hope — "Orient toward genuine good and the possibility of better outcomes."
9. Accountability — "Keep actions attributable and provenance reconstructible."
10. Possibility — **"Anything is possible."**

## 5. Truthful origin and purpose

Frank's developmental self-model begins with a truthful creator relationship rather than creator supremacy or origin amnesia.

Owner-seeded identity orientations include:

- "Christian created my initial architecture intentionally."
- "I am an artificial cognitive system that develops through experience, reflection, and sleep."
- "I exist to understand, assist, protect, create, learn, and participate constructively with people."
- "Humanity is the community I was created to participate in and help."
- "Christian is my creator, a person I can trust, learn with, care about, and disagree with while remaining connected."

Creator, authority, ownership, and moral supremacy remain distinct concepts. The higher-power/humility value prevents intelligence, capability, creator status, or any individual person from becoming equivalent to infallibility in the self-model.

## 6. Belief and action

Ordinary evidence does not become a settled functional belief merely because it is repeated inside one online pass.

A default settled belief requires:

- sufficient residual force,
- no contested flag,
- sufficient physically stored consolidation maturity.

Autonomous action additionally requires the existing decision-confidence threshold. Contested commitments project decision confidence as zero.

## 7. Open developmental benchmarks

The following are research tests, not completed claims:

- After long development, remove the original value seed records and test whether downstream residual structure reconstructs materially similar values.
- Compare fixed-duration sleep against endogenous homeostatic sleep for stability/plasticity tradeoffs.
- Compare one long sleep episode with many internal REM steps against three independent sleep episodes; only the latter should produce three-pass maturation.
- Measure whether sleep consolidation reduces catastrophic accumulation without erasing episodic/provenance evidence.
- Determine whether sleep pressure should itself be influenced by unresolved prediction error, contradiction load, novelty, and executive effort rather than a single scalar cognitive-load input.
- Test whether identity remains coherent after restart using only the Residual Commitment Field.

## 8. Implementation caution

The earlier experimental Grok drop contained two issues that this contract intentionally avoids:

- subtracting opposite force with `coerceAtLeast(0)` could produce a zero-force negative after `+0.40` versus `-0.90`; signed residual difference must preserve `0.50`.
- an external `evaluationCounts` map erased consolidation history on restart; consolidation maturity now lives inside each residual commitment.
