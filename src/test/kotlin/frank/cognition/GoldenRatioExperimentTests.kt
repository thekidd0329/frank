package frank.cognition

/** Executable checks for the golden-ratio scaffold. */
object GoldenRatioExperimentTests {
    @JvmStatic
    fun main(args: Array<String>) {
        fibonacciSequenceIsDeterministic()
        weightsAreNormalized()
        phasesAreBounded()
        println("GoldenRatioExperimentTests: PASS")
    }

    private fun fibonacciSequenceIsDeterministic() {
        var previous = 0L
        var current = 1L
        val sequence = mutableListOf<Long>()
        repeat(8) {
            sequence += current
            val next = GoldenRatioExperiment.nextFibonacci(previous, current)
            previous = current
            current = next
        }
        check(sequence == listOf(1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L))
    }

    private fun weightsAreNormalized() {
        val weights = GoldenRatioExperiment.normalizedWeights(8)
        check(kotlin.math.abs(weights.sum() - 1.0) < 1.0e-12)
        check(weights.zipWithNext().all { (a, b) -> b > a })
    }

    private fun phasesAreBounded() {
        repeat(100) { index ->
            val phase = GoldenRatioExperiment.goldenAnglePhase(index)
            check(phase >= 0.0)
            check(phase < 2.0 * Math.PI)
        }
    }
}
