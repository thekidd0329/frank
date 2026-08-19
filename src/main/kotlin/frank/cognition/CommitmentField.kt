package frank.cognition

import kotlin.math.abs

/**
 * CommitmentField — the only persistent cognitive ground truth.
 * Beliefs, values, self-model views, sleep-state projections, and goals are derived.
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

    fun activationSet(
        minForce: Float = 0.25f,
        maxCount: Int = 64
    ): List<ResidualCommitment> =
        commitments.values
            .filter { it.residualForce >= minForce }
            .sortedByDescending { it.residualForce }
            .take(maxCount)

    /**
     * Uniform decay with two structural exceptions:
     * - HOMEOSTATIC commitments are actively regulated elsewhere, so ordinary memory decay skips them.
     * - SLOW_DECAY commitments remain plastic but lose force at one tenth the ordinary rate.
     */
    fun decayAll(factor: Float) {
        require(factor in 0.0f..1.0f)
        val updated = commitments.mapValues { (_, c) ->
            when {
                c.flags.has(CommitmentFlags.HOMEOSTATIC) -> c
                c.flags.has(CommitmentFlags.SLOW_DECAY) -> {
                    val ordinaryLoss = 1.0f - factor
                    c.decayed((1.0f - ordinaryLoss * 0.10f).coerceIn(0.0f, 1.0f))
                }
                else -> c.decayed(factor)
            }
        }
        commitments.clear()
        commitments.putAll(updated)
    }

    /**
     * Same-polarity evidence reinforces. Opposing evidence applies signed pressure.
     * +0.40 followed by -0.90 therefore leaves a NEGATIVE commitment with force 0.50.
     * Contradiction resets consolidation maturity because the commitment is unsettled again.
     */
    fun absorb(incoming: ResidualCommitment) {
        val existing = commitments[incoming.locus]
        if (existing == null) {
            put(incoming)
            return
        }

        val mergedFlags = existing.flags.merged(incoming.flags)

        if (existing.polarity == incoming.polarity) {
            val combinedForce = (existing.residualForce + incoming.residualForce * 0.5f)
                .coerceIn(0.0f, 1.0f)
            put(
                existing.copy(
                    residualForce = combinedForce,
                    consolidationMaturity = maxOf(existing.consolidationMaturity, incoming.consolidationMaturity),
                    temporalPersistence = incoming.temporalPersistence,
                    provenance = incoming.provenance ?: existing.provenance,
                    flags = mergedFlags.without(CommitmentFlags.CONTESTED)
                )
            )
            return
        }

        val net = abs(existing.residualForce - incoming.residualForce)
        val survivor = if (existing.residualForce >= incoming.residualForce) existing else incoming
        put(
            survivor.copy(
                residualForce = net,
                consolidationMaturity = 0.0f,
                temporalPersistence = incoming.temporalPersistence,
                flags = mergedFlags.with(CommitmentFlags.CONTESTED)
            )
        )
    }

    fun snapshot(): List<ResidualCommitment> = commitments.values.toList()

    fun clear() = commitments.clear()
}
