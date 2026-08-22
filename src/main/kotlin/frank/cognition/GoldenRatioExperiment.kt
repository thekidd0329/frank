package frank.cognition

/**
 * Canonical geometric scaffold for Frank's developmental architecture.
 *
 * The golden ratio is a canonical organizing principle for spacing and allocation,
 * without becoming a pre-installed semantic identity or finished trait. This class contains no semantic labels,
 * personality, emotion, or ontology.
 */
object GoldenRatioExperiment {
    const val PHI: Double = 1.6180339887498948482

    /** Returns the next Fibonacci number after [current] and [previous]. */
    fun nextFibonacci(previous: Long, current: Long): Long {
        require(previous >= 0 && current >= 0) { "Fibonacci inputs must be non-negative" }
        return Math.addExact(previous, current)
    }

    /**
     * Returns phi^-power. This is the canonical source for experiments that
     * compare phi-derived scales against ordinary control constants.
     */
    fun inversePower(power: Int): Double {
        require(power >= 0) { "power must be non-negative" }
        return Math.pow(PHI, -power.toDouble())
    }

    /**
     * Produces [count] positive geometric weights whose adjacent ratio is PHI.
     * The result is normalized and contains no cognitive interpretation.
     */
    fun normalizedWeights(count: Int): List<Double> {
        require(count > 0) { "count must be positive" }
        val raw = List(count) { index -> Math.pow(PHI, index.toDouble()) }
        val total = raw.sum()
        return raw.map { it / total }
    }

    /**
     * Maps a bounded [index] into a deterministic golden-angle phase.
     * This is useful for probing locality/coverage, not for naming concepts.
     */
    fun goldenAnglePhase(index: Int): Double {
        require(index >= 0) { "index must be non-negative" }
        val goldenAngle = 2.0 * Math.PI / (PHI * PHI)
        return (index * goldenAngle) % (2.0 * Math.PI)
    }
}
