package frank.cognition

/**
 * A pre-language information need derived from Frank's own cognitive state.
 *
 * This is deliberately not a scripted English question. The cognition layer
 * chooses what remains unresolved; a later language surface may verbalize the
 * selected intent without deciding its subject for Frank.
 */
data class EpistemicGap(
    val locus: Locus,
    val pressure: Float,
    val residualForce: Float,
    val consolidationMaturity: Float,
    val contested: Boolean
) {
    init {
        require(pressure in 0f..1f)
        require(residualForce in 0f..1f)
        require(consolidationMaturity in 0f..1f)
    }
}

object EpistemicGapDetector {
    /**
     * Rank observed loci by unresolved cognitive pressure.
     *
     * An observed locus remains a gap when its surviving force is weak, its
     * consolidation is immature, or the field marks it contested. The detector
     * does not invent subject matter: candidates must come from loci Frank has
     * actually encountered.
     */
    fun rank(
        observedLoci: Iterable<Locus>,
        commitments: Iterable<ResidualCommitment>,
        limit: Int = 8
    ): List<EpistemicGap> {
        require(limit >= 0)
        if (limit == 0) return emptyList()

        val byLocus = commitments.associateBy { it.locus }
        return observedLoci
            .distinct()
            .map { locus ->
                val commitment = byLocus[locus]
                val force = commitment?.residualForce ?: 0f
                val maturity = commitment?.consolidationMaturity ?: 0f
                val contested = commitment?.flags?.has(CommitmentFlags.CONTESTED) == true

                val uncertaintyPressure = 1f - force
                val consolidationPressure = 1f - maturity
                val conflictPressure = if (contested) 1f else 0f
                val pressure = (
                    uncertaintyPressure * 0.50f +
                        consolidationPressure * 0.35f +
                        conflictPressure * 0.15f
                    ).coerceIn(0f, 1f)

                EpistemicGap(
                    locus = locus,
                    pressure = pressure,
                    residualForce = force,
                    consolidationMaturity = maturity,
                    contested = contested
                )
            }
            .filter { it.pressure > 0.05f }
            .sortedWith(
                compareByDescending<EpistemicGap> { it.pressure }
                    .thenBy { it.locus.raw }
            )
            .take(limit)
    }
}
