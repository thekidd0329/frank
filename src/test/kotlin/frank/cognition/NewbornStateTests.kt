package frank.cognition

object NewbornStateTests {
    @JvmStatic
    fun main(args: Array<String>) {
        newbornStartsWithoutMatureSemantics()
        experienceChangesInternalCondition()
        contradictionAndRecoveryAreVisible()
        println("NewbornStateTests: PASS")
    }

    private fun newbornStartsWithoutMatureSemantics() {
        val newborn = NewbornState()
        check(newborn.ageTicks == 0L)
        check(newborn.learnedSignals == 0)
        check(newborn.affect == MockAffect.neutral())
        check(newborn.wakePressure == 0f)
    }

    private fun experienceChangesInternalCondition() {
        val newborn = NewbornState()
        val after = newborn.experience(
            DevelopmentalSignal(
                novelty = 0.8f,
                unresolvedPressure = 0.4f,
                reward = 0.7f
            )
        )
        check(after.ageTicks == 1L)
        check(after.learnedSignals == 1)
        check(after.affect.valence > newborn.affect.valence)
        check(after.affect.curiosity > newborn.affect.curiosity)
        check(after.unmetNeedPressure > newborn.unmetNeedPressure)
    }

    private fun contradictionAndRecoveryAreVisible() {
        val newborn = NewbornState()
            .experience(DevelopmentalSignal(novelty = 0.2f, reward = 0.8f))
            .experience(DevelopmentalSignal(novelty = 0.1f, threat = 0.8f, unresolvedPressure = 0.6f))
        check(newborn.affect.valence < 0.3f)
        check(newborn.affect.safety < 0f)
        val recovered = newborn.recover(1f)
        check(recovered.wakePressure <= newborn.wakePressure)
        check(recovered.affect.arousal < newborn.affect.arousal)
    }
}
