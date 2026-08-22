package frank.cognition

import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.min

/**
 * Minimal newborn learning loop.
 *
 * Raw observations are reduced to opaque stable loci. No labels, language,
 * personality, relationships, or pretrained semantic categories are injected.
 *
 * Learning is integrated over simulated neural time. Repeating a function call
 * faster on stronger hardware must not create extra learning unless additional
 * neural time is explicitly supplied.
 */
class NewbornLearningLoop(
    private val pruneThreshold: Float = 0.0001f,
    /**
     * Residual-force change per simulated second at unit signed evidence.
     * This is deliberately exposed for later biological calibration.
     */
    private val residualLearningRatePerSecond: Float = 1.0f
) {
    private val residuals = LinkedHashMap<Long, Float>()
    var state: NewbornState = NewbornState()
        private set

    val residualField: Map<Long, Float>
        get() = residuals.toMap()

    /**
     * Integrate one observation for a specified amount of simulated neural time.
     *
     * durationSeconds says how long the experience exists in the simulated brain.
     * integrationStepSeconds controls numerical resolution only. Changing the step
     * size while preserving duration should not materially change the result.
     */
    fun observe(
        raw: ByteArray,
        signal: DevelopmentalSignal,
        durationSeconds: Float = NeuralTime.DEFAULT_TICK_SECONDS,
        integrationStepSeconds: Float = NeuralTime.DEFAULT_TICK_SECONDS
    ): Long {
        require(raw.isNotEmpty()) { "newborn observations cannot be empty" }
        require(durationSeconds > 0f) { "observation duration must be positive" }
        require(integrationStepSeconds > 0f) { "integration step must be positive" }

        val locus = stableLocus(raw)
        val signedEvidence = signal.reward - signal.threat
        var residual = residuals[locus] ?: 0f
        var remaining = durationSeconds.toDouble()
        val step = integrationStepSeconds.toDouble()

        while (remaining > 1e-12) {
            val dt = min(step, remaining).toFloat()
            residual = (
                residual +
                    signedEvidence * residualLearningRatePerSecond * dt
                ).coerceIn(-1f, 1f)
            state = state.experience(signal, dt = dt)
            remaining -= dt.toDouble()
        }

        // Prune only after the complete exposure. Otherwise a small, biologically
        // plausible per-millisecond increment could be erased before it accumulates.
        if (abs(residual) <= pruneThreshold) residuals.remove(locus)
        else residuals[locus] = residual

        return locus
    }

    fun recover(amount: Float): NewbornState {
        state = state.recover(amount)
        return state
    }

    fun reconstructState(): NewbornState = state.copy()

    private fun stableLocus(raw: ByteArray): Long {
        var hash = -0x340d631b8c467e7L
        raw.forEach { byte ->
            hash = (hash xor byte.toLong()) * 0x100000001b3L
        }
        return hash
    }
}
