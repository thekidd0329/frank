package frank.cognition

import kotlin.math.abs
import kotlin.math.roundToInt

object NeuralTimeTests {
    @JvmStatic
    fun main(args: Array<String>) {
        canonicalTickIsOneMillisecond()
        learningDependsOnDurationNotIntegrationCount()
        shorterExposureLeavesWeakerResidual()
        dynamicsAreStableAcrossReasonableIntegrationSteps()
        println("NeuralTimeTests: PASS")
    }

    private fun canonicalTickIsOneMillisecond() {
        check(abs(NeuralTime.DEFAULT_TICK_SECONDS - 0.001f) < 1e-9f)
        check(abs(NeuralTime.milliseconds(1f) - NeuralTime.DEFAULT_TICK_SECONDS) < 1e-9f)
    }

    private fun learningDependsOnDurationNotIntegrationCount() {
        val raw = "same neural second".encodeToByteArray()
        val signal = DevelopmentalSignal(novelty = 0.5f, reward = 0.6f)

        val fine = NewbornLearningLoop()
        fine.observe(
            raw,
            signal,
            durationSeconds = 1f,
            integrationStepSeconds = 0.001f
        )

        val coarse = NewbornLearningLoop()
        coarse.observe(
            raw,
            signal,
            durationSeconds = 1f,
            integrationStepSeconds = 0.010f
        )

        val fineResidual = fine.residualField.values.single()
        val coarseResidual = coarse.residualField.values.single()

        // The faster/more frequent integration path must not earn more learning.
        check(abs(fineResidual - coarseResidual) < 0.002f) {
            "same neural duration produced different learning: fine=$fineResidual coarse=$coarseResidual"
        }
        check(abs(fine.state.neuralAgeSeconds - 1.0) < 0.002)
        check(abs(coarse.state.neuralAgeSeconds - 1.0) < 0.002)
    }

    private fun shorterExposureLeavesWeakerResidual() {
        val raw = "duration matters".encodeToByteArray()
        val signal = DevelopmentalSignal(novelty = 0.8f, reward = 0.5f)

        val short = NewbornLearningLoop()
        short.observe(raw, signal, durationSeconds = 0.1f)

        val long = NewbornLearningLoop()
        long.observe(raw, signal, durationSeconds = 1.0f)

        val shortResidual = short.residualField.values.single()
        val longResidual = long.residualField.values.single()
        check(longResidual > shortResidual * 5f) {
            "longer exposure should leave a materially stronger residual"
        }
    }

    private fun dynamicsAreStableAcrossReasonableIntegrationSteps() {
        val fine = runAwakeSecond(dt = 0.001f)
        val coarse = runAwakeSecond(dt = 0.010f)

        check(abs(fine.neuralAgeSeconds - 1.0) < 0.002)
        check(abs(coarse.neuralAgeSeconds - 1.0) < 0.002)
        check(abs(fine.h.value - coarse.h.value) < 0.01f)
        check(abs(fine.e.value - coarse.e.value) < 0.01f)
        check(abs(fine.load.value - coarse.load.value) < 0.01f)
        check(abs(fine.contingency.omega - coarse.contingency.omega) < 0.01f)
    }

    private fun runAwakeSecond(dt: Float): NewbornDynamics {
        var state = NewbornDynamics()
        val steps = (1f / dt).roundToInt()
        repeat(steps) {
            state = state.wakeStep(
                predictionError = 0.4f,
                uncertainty = 0.5f,
                residualConflict = 0.2f,
                onlineLearningMagnitude = 0.3f,
                predictedChange = 1f,
                actualChange = 1f,
                dt = dt
            )
        }
        return state
    }
}
