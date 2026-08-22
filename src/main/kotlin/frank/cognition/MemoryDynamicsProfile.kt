package frank.cognition

/**
 * Memory dynamics are deliberately separated from semantic cognition so the
 * newborn can be exposed to different retention laws without changing what a
 * memory means.
 */
data class MemoryDynamicsProfile(
    val passiveRetention: Float,
    val reinforcementGain: Float,
    val associationGain: Float,
    val retrievalThreshold: Float,
    val pruneThreshold: Float
) {
    init {
        require(passiveRetention in 0f..1f)
        require(reinforcementGain in 0f..1f)
        require(associationGain in 0f..1f)
        require(retrievalThreshold in 0f..1f)
        require(pruneThreshold in 0f..retrievalThreshold)
    }

    companion object {
        /** Existing-style neutral control, intentionally simple rather than optimized. */
        val CONTROL = MemoryDynamicsProfile(
            passiveRetention = 0.90f,
            reinforcementGain = 0.50f,
            associationGain = 0.50f,
            retrievalThreshold = 0.25f,
            pruneThreshold = 0.05f
        )

        /**
         * Golden-ratio-derived experimental profile.
         *
         * No parameter here is presented as biologically correct. The purpose is
         * to make the hypothesis falsifiable: identical experiences can be run
         * under CONTROL and PHI and compared without changing semantics.
         */
        val PHI = MemoryDynamicsProfile(
            passiveRetention = GoldenRatioExperiment.inversePower(1).toFloat(),
            reinforcementGain = GoldenRatioExperiment.inversePower(1).toFloat(),
            associationGain = GoldenRatioExperiment.inversePower(2).toFloat(),
            retrievalThreshold = GoldenRatioExperiment.inversePower(3).toFloat(),
            pruneThreshold = GoldenRatioExperiment.inversePower(6).toFloat()
        )
    }
}
