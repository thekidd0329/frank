package frank.cognition

object HomeostaticTensionTests {
    @JvmStatic
    fun main(args: Array<String>) {
        wakefulnessRaisesH()
        internalAndExternalErrorRaiseH()
        sleepDampsTowardEquilibrium()
        println("HomeostaticTensionTests: PASS")
    }

    private fun wakefulnessRaisesH() {
        val start = HomeostaticTension.neutral()
        val after = start.step(DevelopmentalSignal(novelty = 0f, processingCost = 0.4f, wakeRate = 0.5f))
        check(after.value > start.value)
    }

    private fun internalAndExternalErrorRaiseH() {
        val start = HomeostaticTension.neutral()
        val after = start.step(
            DevelopmentalSignal(
                novelty = 0f,
                processingCost = 0f,
                internalError = 0.8f,
                externalPredictionError = 0.9f,
                externalThreshold = 0.25f,
                internalGain = 0.5f,
                externalGain = 0.5f
            )
        )
        check(after.value > 0f)
    }

    private fun sleepDampsTowardEquilibrium() {
        val start = HomeostaticTension(value = 1f, equilibrium = 0.2f)
        val after = start.step(
            DevelopmentalSignal(
                novelty = 0f,
                processingCost = 0f,
                sleepGate = 1f,
                sleepDamping = 0.5f
            )
        )
        check(after.value < start.value)
        check(after.value >= 0f)
    }
}
