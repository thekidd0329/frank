package frank.cognition

import kotlin.math.abs

object NewbornLearningLoopTests {
    @JvmStatic
    fun main(args: Array<String>) {
        repeatedInputReusesLocus()
        contradictionLeavesSignedResidual()
        cancellationPrunesTheLocus()
        stateAndFieldMoveTogetherInNeuralTime()
        println("NewbornLearningLoopTests: PASS")
    }

    private fun repeatedInputReusesLocus() {
        val loop = NewbornLearningLoop()
        val first = loop.observe(
            "same".encodeToByteArray(),
            DevelopmentalSignal(novelty = 0.8f, reward = 0.5f),
            durationSeconds = 1f
        )
        val second = loop.observe(
            "same".encodeToByteArray(),
            DevelopmentalSignal(novelty = 0.1f, familiarity = 0.7f, reward = 0.2f),
            durationSeconds = 1f
        )
        check(first == second)
        check(loop.residualField.size == 1)
    }

    private fun contradictionLeavesSignedResidual() {
        val loop = NewbornLearningLoop()
        val raw = "pattern".encodeToByteArray()
        loop.observe(raw, DevelopmentalSignal(novelty = 0.5f, reward = 0.8f), durationSeconds = 1f)
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, threat = 0.3f), durationSeconds = 1f)
        check(loop.residualField.values.single() > 0f)
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, threat = 1.0f), durationSeconds = 1f)
        check(loop.residualField.values.single() < 0f)
    }

    private fun cancellationPrunesTheLocus() {
        val loop = NewbornLearningLoop()
        val raw = "cancel".encodeToByteArray()
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, reward = 0.4f), durationSeconds = 1f)
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, threat = 0.4f), durationSeconds = 1f)
        check(loop.residualField.isEmpty())
    }

    private fun stateAndFieldMoveTogetherInNeuralTime() {
        val loop = NewbornLearningLoop()
        check(loop.state.neuralAgeSeconds == 0.0)
        loop.observe(
            "novel".encodeToByteArray(),
            DevelopmentalSignal(novelty = 1.0f, reward = 0.6f),
            durationSeconds = 0.1f
        )
        check(abs(loop.state.neuralAgeSeconds - 0.1) < 0.001)
        check(loop.state.affect.curiosity > 0f)
        check(loop.residualField.isNotEmpty())
        val rebuilt = loop.reconstructState()
        check(rebuilt == loop.state)
    }
}
