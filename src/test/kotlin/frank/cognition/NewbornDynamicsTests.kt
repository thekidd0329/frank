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
        // This represents a real learning-progress condition: the slow error
        // trace still reflects a previously difficult prediction while the
        // fast trace has already fallen. In that state, slow-fast is positive
        // and should relieve epistemic tension.
        val improving = EpistemicTension(
            value = 0.5f,
            fastError = 0.1f,
            slowError = 0.5f
        )
        val after = improving.update(predictionError = 0.1f, uncertainty = 0.2f)
        check(after.fastError < after.slowError)
        check(after.value < improving.value)
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
