package frank.cognition

/**
 * ReasoningEngine — the living heart of the Kotlin reasoning module.
 *
 * Responsibilities:
 *  - own the Residual Commitment Field
 *  - absorb new evidence as commitments
 *  - maintain decay / reinforcement
 *  - expose rebuildable projections
 *  - produce the tight activation set for any neural / inference backend
 *  - support the autonomy threshold logic already present in frank.autonomy
 *
 * This class is deliberately free of Android, UI, and provider APIs.
 * It is pure cognition.
 */
class ReasoningEngine(
    private val field: CommitmentField = CommitmentField(),
    private val defaultDecayFactor: Float = 0.997f   // gentle continuous decay
) {
    private val projections = ProjectionEngine(field)

    // ------------------------------------------------------------------
    // Ground-layer operations
    // ------------------------------------------------------------------

    fun absorb(commitment: ResidualCommitment) {
        field.absorb(commitment)
    }

    fun absorbAll(commitments: Iterable<ResidualCommitment>) {
        commitments.forEach { field.absorb(it) }
    }

    fun tickDecay() {
        field.decayAll(defaultDecayFactor)
    }

    fun reinforce(locus: Locus, delta: Float = 0.1f) {
        val existing = field.get(locus) ?: return
        field.put(existing.reinforced(delta))
    }

    // ------------------------------------------------------------------
    // Projections (rebuildable)
    // ------------------------------------------------------------------

    fun currentBeliefs(minForce: Float = 0.1f) = projections.beliefs(minForce)

    fun currentGoals() = projections.goals()

    fun neuralContext(
        minForce: Float = 0.25f,
        maxCount: Int = 48
    ): List<ResidualCommitment> = projections.neuralActivation(minForce, maxCount)

    // ------------------------------------------------------------------
    // Decision support for autonomy (compatible with existing 0.90 threshold)
    // ------------------------------------------------------------------

    /**
     * Returns a decision-relevant confidence for a locus.
     * Contested commitments are deliberately suppressed so they cannot
     * drive autonomous side effects (mirrors the 40–60% deadband idea
     * already present in the architecture contract).
     */
    fun decisionConfidence(locus: Locus): Float {
        val c = field.get(locus) ?: return 0.0f
        if (c.flags.has(CommitmentFlags.CONTESTED)) return 0.0f
        return c.residualForce
    }

    fun canActAutonomously(locus: Locus, threshold: Float = 0.90f): Boolean =
        decisionConfidence(locus) >= threshold

    // ------------------------------------------------------------------
    // Reconstruction / persistence boundary
    // ------------------------------------------------------------------

    /**
     * The only thing that must survive a restart.
     * Everything else is regenerable from this snapshot.
     */
    fun commitmentSnapshot(): List<ResidualCommitment> = field.snapshot()

    /**
     * Rebuild the field from a previously persisted snapshot.
     * This is the recovery path that satisfies the reconstruction invariant.
     */
    fun restoreFrom(snapshot: List<ResidualCommitment>) {
        field.clear()
        snapshot.forEach { field.put(it) }
    }

    fun liveCount(threshold: Float = 0.05f): Int = field.live(threshold).size

    fun debugSummary(): String = buildString {
        appendLine("CommitmentField size=${field.size}")
        appendLine("Live (≥0.05)=${liveCount()}")
        appendLine("High-force activation set:")
        neuralContext().forEach { c ->
            appendLine("  locus=${c.locus.raw} pol=${c.polarity} force=${"%.3f".format(c.residualForce)} binding=${"%.2f".format(c.contextualBinding)}")
        }
    }
}
