package frank.cognition

import java.util.LinkedHashMap
import kotlin.math.abs

/**
 * Minimal newborn learning loop.
 *
 * Raw observations are reduced to opaque stable loci. No labels, language,
 * personality, relationships, or pretrained semantic categories are injected.
 */
class NewbornLearningLoop(
    private val pruneThreshold: Float = 0.0001f
) {
    private val residuals = LinkedHashMap<Long, Float>()
    var state: NewbornState = NewbornState()
        private set

    val residualField: Map<Long, Float>
        get() = residuals.toMap()

    /**
     * Stable cognitive address for a raw observation.
     * Exposed so the teaching surface and the Residual Commitment Field can
     * refer to the same experience without inventing a second addressing rule.
     */
    fun locusFor(raw: ByteArray): Long {
        require(raw.isNotEmpty()) { "newborn observations cannot be empty" }
        return stableLocus(raw)
    }

    fun observe(raw: ByteArray, signal: DevelopmentalSignal): Long {
        val locus = locusFor(raw)
        val signedEvidence = signal.reward - signal.threat
        val current = residuals[locus] ?: 0f
        val next = (current + signedEvidence).coerceIn(-1f, 1f)

        if (abs(next) <= pruneThreshold) residuals.remove(locus)
        else residuals[locus] = next

        state = state.experience(signal)
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
