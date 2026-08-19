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
 *
 * This is the Kotlin-side semantic model. The eventual Rust frank.cog store
 * will pack an equivalent tuple into a fixed-width binary record (Candidate A: 128 bits).
 * Rust object representation ≠ Frank persistent representation.
 */
data class ResidualCommitment(
    /** Address in cognitive space. Similar meanings should land near each other. */
    val locus: Locus,

    /** Direction of the preference. */
    val polarity: Polarity,

    /**
     * How much residual predictive force this commitment still carries.
     * Range is intentionally [0.0, 1.0] for the Kotlin model.
     * Binary packing will quantize (e.g. 8-bit).
     */
    val residualForce: Float,

    /**
     * How tightly this commitment is bound to a particular context / frame.
     * High binding → only active in narrow situations.
     * Low binding → more globally available.
     */
    val contextualBinding: Float,

    /**
     * Temporal persistence / generation marker.
     * Used for decay, recency, and crash-safe generation protocols later.
     */
    val temporalPersistence: TemporalAnchor,

    /**
     * Optional, expensive. Null by default.
     * Only allocated when the cost is justified (high-stakes, explicit learning, etc.).
     */
    val provenance: ProvenanceHandle? = null,

    /** Structural flags that are truly universal across all commitments. */
    val flags: CommitmentFlags = CommitmentFlags.NONE
) {
    init {
        require(residualForce in 0.0f..1.0f) { "residualForce must be in [0,1]" }
        require(contextualBinding in 0.0f..1.0f) { "contextualBinding must be in [0,1]" }
    }

    /** True when this commitment still carries usable force. */
    fun isLive(threshold: Float = 0.05f): Boolean = residualForce >= threshold

    /** Decayed copy (pure). Does not mutate. */
    fun decayed(factor: Float): ResidualCommitment {
        require(factor in 0.0f..1.0f)
        return copy(residualForce = (residualForce * factor).coerceIn(0.0f, 1.0f))
    }

    /** Reinforced copy (pure). */
    fun reinforced(delta: Float): ResidualCommitment {
        return copy(residualForce = (residualForce + delta).coerceIn(0.0f, 1.0f))
    }
}

/**
 * Locus — a stable address in cognitive space.
 * Opaque by design so the addressing scheme can evolve
 * (hash, hierarchical path, learned embedding index, etc.)
 * without rewriting the commitment atom.
 */
@JvmInline
value class Locus(val raw: Long) {
    companion object {
        fun fromParts(high: Int, low: Int): Locus =
            Locus(((high.toLong() and 0xFFFFFFFFL) shl 32) or (low.toLong() and 0xFFFFFFFFL))
    }
}

enum class Polarity {
    POSITIVE,
    NEGATIVE,
    /** Explicit absence / contrast without strong directional force. */
    NEUTRAL
}

/**
 * Temporal anchor kept deliberately small.
 * Full wall-clock timestamps live in secondary indices when needed.
 */
data class TemporalAnchor(
    /** Generation or logical clock tick when this commitment was last reinforced. */
    val generation: Long,
    /** Coarse recency bucket or delta from a known epoch (packing-friendly). */
    val recencyDelta: Int = 0
)

/**
 * Cheap optional handle. The real provenance payload lives elsewhere
 * (or will live in a secondary arena in the binary store).
 */
@JvmInline
value class ProvenanceHandle(val id: Long)

/**
 * Bit-friendly flags. Keep the set tiny and universal.
 */
@JvmInline
value class CommitmentFlags(val bits: Int) {
    companion object {
        val NONE = CommitmentFlags(0)
        val VOLATILE = CommitmentFlags(1 shl 0)
        val EXPLICIT = CommitmentFlags(1 shl 1)
        val INFERRED = CommitmentFlags(1 shl 2)
        val CONTESTED = CommitmentFlags(1 shl 3)
        val HIGH_STAKES = CommitmentFlags(1 shl 4)
    }

    fun has(flag: CommitmentFlags): Boolean = (bits and flag.bits) != 0
    fun with(flag: CommitmentFlags): CommitmentFlags = CommitmentFlags(bits or flag.bits)
    fun without(flag: CommitmentFlags): CommitmentFlags = CommitmentFlags(bits and flag.bits.inv())
}
