package frank.cognition

import kotlin.math.abs

object NewbornDynamicsTests {
    @JvmStatic
    fun main(args: Array<String>) {
        learningProgressCanReduceEpistemicTension()
        loadAccumulatesAwakeAndReducesDuringSleep()
        contingencySeparatesPredictedFromUnpredictedChangeOverTime()
        newbornStartsUndifferentiated()
        println("NewbornDynamicsTests: PASS")
    }

    private fun learningProgressCanReduceEpistemicTension() {
        // This represents a real learning-progress condition: the slow error
        // trace still reflects a previously difficult prediction while the
        // fast trace has already fallen. In that state, slow-fast is positive
        // and should relieve epistemic tension.
        val improving = EpistemicTension(
            value = 0.5f,
            fastError = 0.1f,
            slowError = 0.5f
        )
        val after = improving.update(
            predictionError = 0.1f,
            uncertainty = 0.2f,
            dt = NeuralTime.DEFAULT_TICK_SECONDS
        )
        check(after.fastError < after.slowError)
        check(after.value < improving.value)
    }

    private fun loadAccumulatesAwakeAndReducesDuringSleep() {
        val awake = ConsolidationLoad().update(
            predictionError = 0.8f,
            residualConflict = 0.4f,
            onlineLearningMagnitude = 0.6f,
            sleepGate = 0f,
            dt = 1f
        )
        val sleeping = awake.update(
            predictionError = 0f,
            residualConflict = 0f,
            onlineLearningMagnitude = 0f,
            sleepGate = 1f,
            dt = 1f
        )
        check(awake.value > 0f)
        check(sleeping.value < awake.value)
    }

    private fun contingencySeparatesPredictedFromUnpredictedChangeOverTime() {
        var learned = ContingencyState()
        repeat(5_000) {
            learned = learned.update(
                predictedChange = 1f,
                actualChange = 1f,
                dt = NeuralTime.DEFAULT_TICK_SECONDS
            )
        }
        val beforeMismatch = learned.omega
        repeat(1_000) {
            learned = learned.update(
                predictedChange = 1f,
                actualChange = -1f,
                dt = NeuralTime.DEFAULT_TICK_SECONDS
            )
        }
        check(beforeMismatch > 0.5f)
        check(learned.omega < beforeMismatch)
    }

    private fun newbornStartsUndifferentiated() {
        val newborn = NewbornDynamics()
        check(newborn.residualField.isEmpty())
        check(newborn.h.value == 0f)
        check(newborn.e.value == 0f)
        check(newborn.contingency.omega == 0f)
        check(newborn.load.value == 0f)
        check(abs(newborn.neuralAgeSeconds) < 1e-12)
    }
}
