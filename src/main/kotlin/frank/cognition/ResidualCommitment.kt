package frank.cognition

/**
 * Residual Commitment — the ground primitive of Frank's persistent cognition.
 *
 * Invariant:
 *   If Frank loses every derived structure but retains the Residual Commitment Field,
 *   Frank must be able to reconstruct himself.
 *
 * Memory is not what happened.
 * Memory is the residual force that what happened left behind.
 */
data class ResidualCommitment(
    val locus: Locus,
    val polarity: Polarity,
    val residualForce: Float,
    val contextualBinding: Float,
    val temporalPersistence: TemporalAnchor,

    /**
     * Physical consolidation state carried by the commitment itself.
     * This replaces hidden external "survived N passes" counters.
     * Independent completed sleep episodes can progressively mature a stable
     * commitment until it becomes eligible to function as a settled belief.
     */
    val consolidationMaturity: Float = 0.0f,

    val provenance: ProvenanceHandle? = null,
    val flags: CommitmentFlags = CommitmentFlags.NONE
) {
    init {
        require(residualForce in 0.0f..1.0f) { "residualForce must be in [0,1]" }
        require(contextualBinding in 0.0f..1.0f) { "contextualBinding must be in [0,1]" }
        require(consolidationMaturity in 0.0f..1.0f) { "consolidationMaturity must be in [0,1]" }
    }

    fun isLive(threshold: Float = 0.05f): Boolean = residualForce >= threshold

    fun decayed(factor: Float): ResidualCommitment {
        require(factor in 0.0f..1.0f)
        return copy(residualForce = (residualForce * factor).coerceIn(0.0f, 1.0f))
    }

    fun reinforced(delta: Float): ResidualCommitment =
        copy(residualForce = (residualForce + delta).coerceIn(0.0f, 1.0f))

    fun consolidated(delta: Float = 1.0f / 3.0f): ResidualCommitment =
        copy(consolidationMaturity = (consolidationMaturity + delta).coerceIn(0.0f, 1.0f))
}

@JvmInline
value class Locus(val raw: Long) {
    companion object {
        fun fromParts(high: Int, low: Int): Locus =
            Locus(((high.toLong() and 0xFFFFFFFFL) shl 32) or (low.toLong() and 0xFFFFFFFFL))
    }
}

/**
 * Kotlin keeps an explicit semantic enum for readability.
 * The future packed binary representation can use one sign bit for positive/negative;
 * a zero residualForce represents neutral / no surviving directional commitment.
 */
enum class Polarity {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

data class TemporalAnchor(
    val generation: Long,
    val recencyDelta: Int = 0
)

@JvmInline
value class ProvenanceHandle(val id: Long)

@JvmInline
value class CommitmentFlags(val bits: Int) {
    companion object {
        val NONE = CommitmentFlags(0)
        val VOLATILE = CommitmentFlags(1 shl 0)
        val EXPLICIT = CommitmentFlags(1 shl 1)
        val INFERRED = CommitmentFlags(1 shl 2)
        val CONTESTED = CommitmentFlags(1 shl 3)
        val HIGH_STAKES = CommitmentFlags(1 shl 4)
        val FOUNDATIONAL = CommitmentFlags(1 shl 5)
        val SLOW_DECAY = CommitmentFlags(1 shl 6)
        val IDENTITY = CommitmentFlags(1 shl 7)
        val HOMEOSTATIC = CommitmentFlags(1 shl 8)
    }

    fun has(flag: CommitmentFlags): Boolean = (bits and flag.bits) != 0
    fun with(flag: CommitmentFlags): CommitmentFlags = CommitmentFlags(bits or flag.bits)
    fun without(flag: CommitmentFlags): CommitmentFlags = CommitmentFlags(bits and flag.bits.inv())
    fun merged(other: CommitmentFlags): CommitmentFlags = CommitmentFlags(bits or other.bits)
}
