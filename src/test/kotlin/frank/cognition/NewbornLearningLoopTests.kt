package frank.cognition

object NewbornLearningLoopTests {
    @JvmStatic
    fun main(args: Array<String>) {
        repeatedInputReusesLocus()
        contradictionLeavesSignedResidual()
        cancellationPrunesTheLocus()
        stateAndFieldMoveTogether()
        passiveAgingWeakensWithoutInversion()
        pairedExperienceBecomesRetrievable()
        println("NewbornLearningLoopTests: PASS")
    }

    private fun repeatedInputReusesLocus() {
        val loop = NewbornLearningLoop()
        val first = loop.observe("same".encodeToByteArray(), DevelopmentalSignal(novelty = 0.8f, reward = 0.5f))
        val second = loop.observe("same".encodeToByteArray(), DevelopmentalSignal(novelty = 0.1f, familiarity = 0.7f, reward = 0.2f))
        check(first == second)
        check(loop.residualField.size == 1)
    }

    private fun contradictionLeavesSignedResidual() {
        val loop = NewbornLearningLoop()
        val raw = "pattern".encodeToByteArray()
        loop.observe(raw, DevelopmentalSignal(novelty = 0.5f, reward = 0.8f))
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, threat = 0.3f))
        check(loop.residualField.values.single() > 0f)
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, threat = 1.0f))
        check(loop.residualField.values.single() < 0f)
    }

    private fun cancellationPrunesTheLocus() {
        val loop = NewbornLearningLoop()
        val raw = "cancel".encodeToByteArray()
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, reward = 0.4f))
        loop.observe(raw, DevelopmentalSignal(novelty = 0.1f, threat = 0.4f))
        check(loop.residualField.isEmpty())
    }

    private fun stateAndFieldMoveTogether() {
        val loop = NewbornLearningLoop()
        check(loop.state.ageTicks == 0L)
        loop.observe("novel".encodeToByteArray(), DevelopmentalSignal(novelty = 1.0f, reward = 0.6f))
        check(loop.state.ageTicks == 1L)
        check(loop.state.affect.curiosity > 0f)
        check(loop.residualField.isNotEmpty())
        val rebuilt = loop.reconstructState()
        check(rebuilt == loop.state)
    }

    private fun passiveAgingWeakensWithoutInversion() {
        val loop = NewbornLearningLoop(memoryProfile = MemoryDynamicsProfile.PHI)
        val raw = "positive".encodeToByteArray()
        val locus = loop.observe(raw, DevelopmentalSignal(reward = 1f))
        val before = loop.residualField.getValue(locus)
        loop.ageMemory()
        val after = loop.residualField.getValue(locus)
        check(after > 0f && after < before)
    }

    private fun pairedExperienceBecomesRetrievable() {
        val loop = NewbornLearningLoop(memoryProfile = MemoryDynamicsProfile.PHI)
        val first = "shape-a".encodeToByteArray()
        val second = "sound-a".encodeToByteArray()
        repeat(4) { loop.associate(first, second) }
        val expected = loop.locusOf(second)
        check(loop.recall(first).any { it.locus == expected })
    }
}
