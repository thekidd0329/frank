package frank.cognition

/**
 * Higher structures are projections, not ground truth.
 *
 * These types exist so the rest of the Kotlin surface (entity resolution,
 * autonomy, planner, UI) can keep talking in familiar terms while the
 * persistent substrate remains the Residual Commitment Field.
 *
 * Reconstruction invariant:
 *   Given only a CommitmentField, these views must be regenerable.
 */

/** Compact belief view derived from one or more commitments. */
data class BeliefProjection(
    val locus: Locus,
    val polarity: Polarity,
    val confidence: Float,
    val contested: Boolean,
    val supportingForce: Float
)

/** Relation-style view. Subject/object are themselves loci (or resolved entity IDs). */
data class RelationProjection(
    val subject: Locus,
    val relationKind: Locus,   // relation types are also loci in cognitive space
    val `object`: Locus,
    val force: Float,
    val contested: Boolean
)

/** Goal view. Goals are high-binding, high-force commitments with future-directed temporal anchors. */
data class GoalProjection(
    val locus: Locus,
    val desiredPolarity: Polarity,
    val urgency: Float,
    val binding: Float
)

/** Episode / temporal chain view. Lightweight; full narrative is generated on demand. */
data class EpisodeProjection(
    val anchor: TemporalAnchor,
    val loci: List<Locus>,
    val aggregateForce: Float
)

/**
 * ProjectionEngine — rebuilds the familiar cognitive surfaces from the commitment field.
 *
 * This is deliberately simple and deterministic so it can serve as the reference
 * implementation of the reconstruction invariant.
 */
class ProjectionEngine(
    private val field: CommitmentField
) {
    fun beliefs(minForce: Float = 0.1f): List<BeliefProjection> =
        field.live(minForce).map { c ->
            BeliefProjection(
                locus = c.locus,
                polarity = c.polarity,
                confidence = c.residualForce,
                contested = c.flags.has(CommitmentFlags.CONTESTED),
                supportingForce = c.residualForce
            )
        }

    fun highForceBeliefs(limit: Int = 32): List<BeliefProjection> =
        beliefs()
            .sortedByDescending { it.confidence }
            .take(limit)

    /**
     * Very light relation extraction.
     * Real systems will maintain a secondary RelationIndex;
     * this version only surfaces commitments whose loci are already
     * known to encode relational structure (placeholder for now).
     */
    fun relations(): List<RelationProjection> {
        // Placeholder: a production version will decode structured loci
        // or consult a regenerable secondary index.
        return emptyList()
    }

    fun goals(minBinding: Float = 0.4f, minForce: Float = 0.3f): List<GoalProjection> =
        field.live(minForce)
            .filter { it.contextualBinding >= minBinding }
            .map { c ->
                GoalProjection(
                    locus = c.locus,
                    desiredPolarity = c.polarity,
                    urgency = c.residualForce,
                    binding = c.contextualBinding
                )
            }

    /**
     * Activation set ready for neural hand-off.
     * Returns the raw commitments so the caller can decide how to materialize text/context.
     */
    fun neuralActivation(
        minForce: Float = 0.25f,
        maxCount: Int = 48
    ): List<ResidualCommitment> = field.activationSet(minForce, maxCount)
}
