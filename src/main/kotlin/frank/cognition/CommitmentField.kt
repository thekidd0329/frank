package frank.cognition

/**
 * CommitmentField — the only persistent ground truth.
 *
 * All higher structures (Beliefs, Relations, Goals, Episodes, Self-model)
 * are rebuildable projections or secondary indices over this field.
 *
 * This Kotlin implementation is the semantic / working model.
 * The eventual Rust frank.cog will map an equivalent field via mmap'd arenas.
 */
class CommitmentField(
    private val commitments: MutableMap<Locus, ResidualCommitment> = linkedMapOf()
) {
    val size: Int get() = commitments.size

    fun get(locus: Locus): ResidualCommitment? = commitments[locus]

    fun put(commitment: ResidualCommitment) {
        commitments[commitment.locus] = commitment
    }

    fun remove(locus: Locus): ResidualCommitment? = commitments.remove(locus)

    fun all(): Collection<ResidualCommitment> = commitments.values

    fun live(threshold: Float = 0.05f): List<ResidualCommitment> =
        commitments.values.filter { it.isLive(threshold) }

    /**
     * High-force subset suitable for neural hand-off.
     * This is the activation set the inference layer should see.
     */
    fun activationSet(
        minForce: Float = 0.25f,
        maxCount: Int = 64
    ): List<ResidualCommitment> =
        commitments.values
            .filter { it.residualForce >= minForce }
            .sortedByDescending { it.residualForce }
            .take(maxCount)

    /**
     * Apply uniform decay. Pure relative to the field mutation.
     */
    fun decayAll(factor: Float) {
        val updated = commitments.mapValues { (_, c) -> c.decayed(factor) }
        commitments.clear()
        commitments.putAll(updated)
    }

    /**
     * Merge / reinforce an incoming commitment at the same locus.
     * Opposite polarity with comparable force marks the locus contested.
     */
    fun absorb(incoming: ResidualCommitment) {
        val existing = commitments[incoming.locus]
        if (existing == null) {
            put(incoming)
            return
        }

        when {
            existing.polarity == incoming.polarity -> {
                // Same direction → reinforce
                val combinedForce = (existing.residualForce + incoming.residualForce * 0.5f)
                    .coerceIn(0.0f, 1.0f)
                put(
                    existing.copy(
                        residualForce = combinedForce,
                        temporalPersistence = incoming.temporalPersistence,
                        flags = existing.flags.without(CommitmentFlags.CONTESTED)
                    )
                )
            }
            else -> {
                // Competing polarities → contest and reduce net force
                val net = kotlin.math.abs(existing.residualForce - incoming.residualForce)
                val survivingPolarity = if (existing.residualForce >= incoming.residualForce)
                    existing.polarity else incoming.polarity
                put(
                    existing.copy(
                        polarity = survivingPolarity,
                        residualForce = net,
                        flags = existing.flags.with(CommitmentFlags.CONTESTED),
                        temporalPersistence = incoming.temporalPersistence
                    )
                )
            }
        }
    }

    /**
     * Snapshot for reconstruction tests / persistence boundary.
     */
    fun snapshot(): List<ResidualCommitment> = commitments.values.toList()

    fun clear() = commitments.clear()
}
