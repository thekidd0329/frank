package frank.cognition

/**
 * Bridge from the existing evidence / compact-claim world
 * (Python prototype + frank.memory / frank.entity) into Residual Commitments.
 *
 * This keeps the new ground layer compatible with the already-shipped
 * ontology, combing, and compact-memory contracts.
 */

/** Minimal evidence shape that the bridge understands. */
data class IncomingEvidence(
    val axisId: Int,
    val valueId: Int,
    val polarityPositive: Boolean,
    val confidence: Float,
    val recency: Long,
    val isExplicit: Boolean = false,
    val isInferred: Boolean = false,
    val provenanceId: Long? = null
)

/**
 * Deterministic mapping from (axis, value) → Locus.
 * Stable numeric ontology IDs already exist in AdaptiveOntology / compact memory.
 * We simply re-address them into cognitive space.
 */
object LocusAddressing {
    fun fromAxisValue(axisId: Int, valueId: Int): Locus =
        Locus.fromParts(axisId, valueId)
}

class EvidenceToCommitment(
    private val generationClock: () -> Long = { System.currentTimeMillis() }
) {
    fun convert(ev: IncomingEvidence): ResidualCommitment {
        val locus = LocusAddressing.fromAxisValue(ev.axisId, ev.valueId)
        val polarity = if (ev.polarityPositive) Polarity.POSITIVE else Polarity.NEGATIVE
        var flags = CommitmentFlags.NONE
        if (ev.isExplicit) flags = flags.with(CommitmentFlags.EXPLICIT)
        if (ev.isInferred) flags = flags.with(CommitmentFlags.INFERRED)

        return ResidualCommitment(
            locus = locus,
            polarity = polarity,
            residualForce = ev.confidence.coerceIn(0.0f, 1.0f),
            contextualBinding = if (ev.isExplicit) 0.35f else 0.55f,
            temporalPersistence = TemporalAnchor(
                generation = generationClock(),
                recencyDelta = 0
            ),
            provenance = ev.provenanceId?.let { ProvenanceHandle(it) },
            flags = flags
        )
    }

    fun convertAll(evidence: Iterable<IncomingEvidence>): List<ResidualCommitment> =
        evidence.map { convert(it) }
}
