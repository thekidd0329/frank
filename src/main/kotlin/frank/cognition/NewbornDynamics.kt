package frank.cognition

import kotlin.math.exp
import kotlin.math.tanh

/**
 * Relay #3 primitives after H. These are dynamical state variables, not
 * named mental faculties or emotion labels.
 */
data class EpistemicTension(
    val value: Float = 0f,
    val fastError: Float = 0f,
    val slowError: Float = 0f
) {
    fun update(
        predictionError: Float,
        uncertainty: Float,
        dt: Float = 1f,
        alpha: Float = 0.2f,
        beta: Float = 0.05f,
        gamma: Float = 0.02f
    ): EpistemicTension {
        require(predictionError >= 0f && uncertainty >= 0f && dt >= 0f)
        val nextFast = fastError + alpha * (predictionError - fastError) * dt
        val nextSlow = slowError + beta * (predictionError - slowError) * dt
        val learningProgress = (nextSlow - nextFast).coerceAtLeast(0f)
        val drive = tanh(uncertainty * uncertainty)
        val nextValue = (value + (drive - learningProgress - gamma * value) * dt).coerceAtLeast(0f)
        return EpistemicTension(nextValue, nextFast, nextSlow)
    }
}

data class ConsolidationLoad(val value: Float = 0f) {
    fun update(
        predictionError: Float,
        residualConflict: Float,
        onlineLearningMagnitude: Float,
        sleepGate: Float,
        dt: Float = 1f
    ): ConsolidationLoad {
        val accumulation =
            0.20f * predictionError +
                0.30f * residualConflict +
                0.15f * onlineLearningMagnitude
        val reduction = 0.35f * sleepGate * value
        return copy(value = (value + (accumulation - reduction) * dt).coerceAtLeast(0f))
    }
}

data class ContingencyState(
    val kappa: Float = 0f,
    val omega: Float = 0f
) {
    fun update(predictedChange: Float, actualChange: Float, dt: Float = 1f): ContingencyState {
        val denominator = kotlin.math.abs(predictedChange * actualChange) + 0.000001f
        val nextKappa = (predictedChange * actualChange / denominator).coerceIn(-1f, 1f)
        val target = 1f / (1f + exp(-4f * nextKappa))
        val nextOmega = (omega + (target - omega) * 0.20f * dt).coerceIn(0f, 1f)
        return ContingencyState(nextKappa, nextOmega)
    }
}

data class NewbornDynamics(
    val h: HomeostaticTension = HomeostaticTension.neutral(),
    val e: EpistemicTension = EpistemicTension(),
    val residualField: Map<Long, Float> = emptyMap(),
    val contingency: ContingencyState = ContingencyState(),
    val load: ConsolidationLoad = ConsolidationLoad(),
    val sleepGate: Float = 0f
) {
    fun wakeStep(
        predictionError: Float,
        uncertainty: Float,
        residualConflict: Float,
        onlineLearningMagnitude: Float,
        predictedChange: Float = 0f,
        actualChange: Float = 0f
    ): NewbornDynamics {
        val nextE = e.update(predictionError, uncertainty)
        val nextH = h.step(
            DevelopmentalSignal(
                novelty = uncertainty.coerceIn(0f, 1f),
                processingCost = onlineLearningMagnitude.coerceIn(0f, 1f),
                internalError = predictionError.coerceIn(0f, 1f),
                externalPredictionError = predictionError.coerceIn(0f, 1f),
                sleepGate = sleepGate
            )
        )
        return copy(
            h = nextH,
            e = nextE,
            contingency = contingency.update(predictedChange, actualChange),
            load = load.update(predictionError, residualConflict, onlineLearningMagnitude, sleepGate)
        )
    }

    fun sleepStep(replayConflict: Float, dt: Float = 1f): NewbornDynamics =
        copy(
            sleepGate = 1f,
            load = load.update(0f, replayConflict, 0f, sleepGate = 1f, dt = dt)
        )

    fun wake(): NewbornDynamics = copy(sleepGate = 0f)
}
