package frank.cognition

object NewbornDynamicsTests {
    @JvmStatic
    fun main(args: Array<String>) {
        learningProgressCanReduceEpistemicTension()
        loadAccumulatesAwakeAndReducesDuringSleep()
        contingencySeparatesPredictedFromUnpredictedChange()
        newbornStartsUndifferentiated()
        println("NewbornDynamicsTests: PASS")
    }

    private fun learningProgressCanReduceEpistemicTension() {
        val initial = EpistemicTension(value = 0.5f)
        val first = initial.update(predictionError = 0.8f, uncertainty = 0.8f)
        val second = first.update(predictionError = 0.2f, uncertainty = 0.4f)
        check(first.value >= 0f)
        check(second.value < first.value || second.fastError < first.fastError)
    }

    private fun loadAccumulatesAwakeAndReducesDuringSleep() {
        val awake = ConsolidationLoad().update(0.8f, 0.4f, 0.6f, sleepGate = 0f)
        val sleeping = awake.update(0f, 0f, 0f, sleepGate = 1f)
        check(awake.value > 0f)
        check(sleeping.value < awake.value)
    }

    private fun contingencySeparatesPredictedFromUnpredictedChange() {
        val state = ContingencyState().update(predictedChange = 1f, actualChange = 1f)
        val mismatch = state.update(predictedChange = 1f, actualChange = -1f)
        check(state.omega > 0.5f)
        check(mismatch.omega < state.omega)
    }

    private fun newbornStartsUndifferentiated() {
        val newborn = NewbornDynamics()
        check(newborn.residualField.isEmpty())
        check(newborn.h.value == 0f)
        check(newborn.e.value == 0f)
        check(newborn.contingency.omega == 0f)
        check(newborn.load.value == 0f)
    }
}
