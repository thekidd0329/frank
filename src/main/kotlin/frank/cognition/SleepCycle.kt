package frank.cognition

/**
 * Sleep-like offline cognition.
 *
 * NREM-like work applies broad homeostatic softening.
 * REM-like work selectively stabilizes coherent high-force residuals and prunes noise.
 * One completed natural sleep episode contributes one independent consolidation exposure.
 * The result is written into consolidationMaturity on each ResidualCommitment itself,
 * never into a hidden evaluation counter.
 */
class SleepCycle(
    private val field: CommitmentField,
    private val nremDecay: Float = 0.996f,
    private val remWinnerScale: Float = 1.006f,
    private val winnerMinForce: Float = 0.35f,
    private val pruneThreshold: Float = 0.03f,
    private val maturityPerCompletedSleep: Float = 1.0f / 3.0f
) {
    data class ConsolidationReport(
        val matured: Int,
        val pruned: Int,
        val meanMaturity: Float
    )

    fun offlineStep(state: CognitiveState) {
        when (state) {
            CognitiveState.AWAKE -> Unit
            CognitiveState.NREM -> field.decayAll(nremDecay)
            CognitiveState.REM -> remStep()
        }
    }

    private fun remStep() {
        field.snapshot().forEach { c ->
            if (c.flags.has(CommitmentFlags.HOMEOSTATIC)) return@forEach
            if (c.flags.has(CommitmentFlags.CONTESTED)) return@forEach

            when {
                c.residualForce < pruneThreshold && !c.flags.has(CommitmentFlags.FOUNDATIONAL) ->
                    field.remove(c.locus)
                c.residualForce >= winnerMinForce ->
                    field.put(c.copy(residualForce = (c.residualForce * remWinnerScale).coerceAtMost(1.0f)))
            }
        }
    }

    /**
     * Called exactly once when endogenous sleep pressure has recovered enough to wake.
     * This is one independent consolidation pass regardless of how many NREM/REM steps
     * happened inside the episode.
     */
    fun completeEpisode(): ConsolidationReport {
        var matured = 0
        var pruned = 0

        field.snapshot().forEach { c ->
            if (c.flags.has(CommitmentFlags.HOMEOSTATIC)) return@forEach

            if (c.residualForce < pruneThreshold && !c.flags.has(CommitmentFlags.FOUNDATIONAL)) {
                field.remove(c.locus)
                pruned++
                return@forEach
            }

            if (!c.flags.has(CommitmentFlags.CONTESTED) && c.residualForce >= 0.20f) {
                field.put(c.consolidated(maturityPerCompletedSleep))
                matured++
            }
        }

        val cognitive = field.snapshot().filterNot { it.flags.has(CommitmentFlags.HOMEOSTATIC) }
        val mean = if (cognitive.isEmpty()) 0.0f else cognitive.map { it.consolidationMaturity }.average().toFloat()
        return ConsolidationReport(matured = matured, pruned = pruned, meanMaturity = mean)
    }
}
