package frank.cognition

/**
 * The initial developmental condition for Frank.
 *
 * Newborn state contains capacity and dynamical variables, not a scripted
 * personality, vocabulary, relationship map, or moral ontology.
 */
data class NewbornState(
    val ageTicks: Long = 0L,
    val wakePressure: Float = 0.0f,
    val unmetNeedPressure: Float = 0.0f,
    val noveltyPressure: Float = 0.0f,
    val homeostaticTension: HomeostaticTension = HomeostaticTension.neutral(),
    val affect: MockAffect = MockAffect.neutral(),
    val learnedSignals: Int = 0
) {
    fun experience(signal: DevelopmentalSignal): NewbornState {
        val nextWake = (wakePressure + signal.processingCost).coerceIn(0f, 1f)
        val nextNeed = (unmetNeedPressure + signal.unresolvedPressure - signal.resolvedPressure)
            .coerceIn(0f, 1f)
        val nextNovelty = (noveltyPressure + signal.novelty - signal.familiarity)
            .coerceIn(0f, 1f)
        return copy(
            ageTicks = ageTicks + 1,
            wakePressure = nextWake,
            unmetNeedPressure = nextNeed,
            noveltyPressure = nextNovelty,
            homeostaticTension = homeostaticTension.step(signal),
            affect = affect.integrate(signal),
            learnedSignals = learnedSignals + 1
        )
    }

    fun recover(amount: Float): NewbornState {
        require(amount in 0f..1f)
        val signal = DevelopmentalSignal(
            novelty = 0f,
            processingCost = 0f,
            sleepGate = amount,
            internalError = 0f,
            externalPredictionError = 0f
        )
        return copy(
            wakePressure = (wakePressure - amount).coerceAtLeast(0f),
            unmetNeedPressure = (unmetNeedPressure - amount * 0.5f).coerceAtLeast(0f),
            noveltyPressure = (noveltyPressure - amount * 0.25f).coerceAtLeast(0f),
            homeostaticTension = homeostaticTension.step(signal),
            affect = affect.recover(amount)
        )
    }
}

/** H: homeostatic/allostatic tension. */
data class HomeostaticTension(
    val value: Float,
    val equilibrium: Float
) {
    init {
        require(value >= 0f)
        require(equilibrium >= 0f)
    }

    fun step(signal: DevelopmentalSignal, dt: Float = 1f): HomeostaticTension {
        require(dt >= 0f)
        val internal = signal.internalError
        val externalExcess = (signal.externalPredictionError * signal.externalPredictionError - signal.externalThreshold)
            .coerceAtLeast(0f)
        val damping = signal.reliabilityDamping * (value - equilibrium)
        val sleepDamping = signal.sleepGate * (value - equilibrium)
        val derivative =
            signal.wakeRate * (1f - signal.sleepGate) +
                signal.internalGain * internal +
                signal.externalGain * externalExcess -
                signal.dampingRate * damping -
                signal.sleepDamping * sleepDamping
        return copy(value = (value + derivative * dt).coerceAtLeast(0f))
    }

    companion object {
        fun neutral() = HomeostaticTension(value = 0f, equilibrium = 0f)
    }
}

data class DevelopmentalSignal(
    val novelty: Float,
    val familiarity: Float = 0f,
    val processingCost: Float = 0.05f,
    val unresolvedPressure: Float = 0f,
    val resolvedPressure: Float = 0f,
    val reward: Float = 0f,
    val threat: Float = 0f,
    val internalError: Float = 0f,
    val externalPredictionError: Float = 0f,
    val externalThreshold: Float = 0.25f,
    val sleepGate: Float = 0f,
    val wakeRate: Float = 0.05f,
    val internalGain: Float = 0.25f,
    val externalGain: Float = 0.25f,
    val dampingRate: Float = 0.10f,
    val sleepDamping: Float = 0.25f,
    val reliabilityDamping: Float = 0f
) {
    init {
        listOf(
            novelty, familiarity, processingCost, unresolvedPressure, resolvedPressure,
            reward, threat, internalError, externalPredictionError, externalThreshold,
            sleepGate, wakeRate, internalGain, externalGain, dampingRate,
            sleepDamping, reliabilityDamping
        ).forEach { require(it >= 0f) }
        require(sleepGate <= 1f)
    }
}

data class MockAffect(
    val valence: Float,
    val arousal: Float,
    val safety: Float,
    val curiosity: Float
) {
    init {
        listOf(valence, arousal, safety, curiosity).forEach { require(it in -1f..1f) }
    }

    fun integrate(signal: DevelopmentalSignal): MockAffect =
        copy(
            valence = (valence + signal.reward * 0.35f - signal.threat * 0.45f).coerceIn(-1f, 1f),
            arousal = (arousal + signal.novelty * 0.25f + signal.threat * 0.35f - signal.familiarity * 0.10f)
                .coerceIn(-1f, 1f),
            safety = (safety + signal.reward * 0.25f - signal.threat * 0.50f).coerceIn(-1f, 1f),
            curiosity = (curiosity + signal.novelty * 0.30f - signal.familiarity * 0.12f)
                .coerceIn(-1f, 1f)
        )

    fun recover(amount: Float): MockAffect =
        copy(
            arousal = (arousal - amount * 0.25f).coerceIn(-1f, 1f),
            safety = (safety + amount * 0.10f).coerceIn(-1f, 1f)
        )

    companion object {
        fun neutral() = MockAffect(valence = 0f, arousal = 0f, safety = 0f, curiosity = 0f)
    }
}
