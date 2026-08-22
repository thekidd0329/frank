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
    private val pruneThreshold: Float = 0.0001f,
    private val memoryProfile: MemoryDynamicsProfile = MemoryDynamicsProfile.CONTROL
) {
    private val residuals = LinkedHashMap<Long, Float>()
    private val associations = DevelopmentalAssociationField(memoryProfile)

    var state: NewbornState = NewbornState()
        private set

    val residualField: Map<Long, Float>
        get() = residuals.toMap()

    val associationField: Map<DevelopmentalAssociationField.Edge, Float>
        get() = associations.snapshot()

    fun observe(raw: ByteArray, signal: DevelopmentalSignal): Long {
        require(raw.isNotEmpty()) { "newborn observations cannot be empty" }
        val locus = locusOf(raw)
        val signedEvidence = signal.reward - signal.threat
        val current = residuals[locus] ?: 0f
        val next = (current + signedEvidence).coerceIn(-1f, 1f)

        if (abs(next) <= pruneThreshold) residuals.remove(locus)
        else residuals[locus] = next

        state = state.experience(signal)
        return locus
    }

    /**
     * Learn that two already-experienced opaque patterns occurred together.
     * Nothing in this operation assigns either pattern a semantic label.
     */
    fun associate(firstRaw: ByteArray, secondRaw: ByteArray, salience: Float = 1f) {
        require(firstRaw.isNotEmpty() && secondRaw.isNotEmpty())
        associations.associate(locusOf(firstRaw), locusOf(secondRaw), salience)
    }

    fun recall(raw: ByteArray, limit: Int = 8): List<DevelopmentalAssociationField.Recall> {
        require(raw.isNotEmpty())
        return associations.recall(locusOf(raw), limit)
    }

    /**
     * Passive memory aging. Residual polarity is preserved: time can weaken a
     * trace toward zero but cannot turn positive evidence into negative evidence.
     */
    fun ageMemory(steps: Int = 1) {
        require(steps >= 0)
        repeat(steps) {
            val iterator = residuals.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val next = entry.value * memoryProfile.passiveRetention
                if (abs(next) <= maxOf(pruneThreshold, memoryProfile.pruneThreshold)) {
                    iterator.remove()
                } else {
                    entry.setValue(next)
                }
            }
            associations.decay()
        }
    }

    fun recover(amount: Float): NewbornState {
        state = state.recover(amount)
        return state
    }

    fun reconstructState(): NewbornState = state.copy()

    fun locusOf(raw: ByteArray): Long {
        require(raw.isNotEmpty()) { "newborn observations cannot be empty" }
        var hash = -0x340d631b8c467e7L
        raw.forEach { byte ->
            hash = (hash xor byte.toLong()) * 0x100000001b3L
        }
        return hash
    }
}
