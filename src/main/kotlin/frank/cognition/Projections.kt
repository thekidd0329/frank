package frank.cognition

data class BeliefProjection(
    val locus: Locus,
    val polarity: Polarity,
    val confidence: Float,
    val contested: Boolean,
    val supportingForce: Float,
    val consolidationMaturity: Float
)

data class RelationProjection(
    val subject: Locus,
    val relationKind: Locus,
    val `object`: Locus,
    val force: Float,
    val contested: Boolean
)

data class GoalProjection(
    val locus: Locus,
    val desiredPolarity: Polarity,
    val urgency: Float,
    val binding: Float
)

data class EpisodeProjection(
    val anchor: TemporalAnchor,
    val loci: List<Locus>,
    val aggregateForce: Float
)

data class IdentityProjection(
    val locus: Locus,
    val confidence: Float,
    val consolidationMaturity: Float
)

class ProjectionEngine(
    private val field: CommitmentField
) {
    fun beliefs(minForce: Float = 0.1f): List<BeliefProjection> =
        field.live(minForce)
            .filterNot { it.flags.has(CommitmentFlags.HOMEOSTATIC) }
            .map { c ->
                BeliefProjection(
                    locus = c.locus,
                    polarity = c.polarity,
                    confidence = c.residualForce,
                    contested = c.flags.has(CommitmentFlags.CONTESTED),
                    supportingForce = c.residualForce,
                    consolidationMaturity = c.consolidationMaturity
                )
            }

    fun highForceBeliefs(limit: Int = 32): List<BeliefProjection> =
        beliefs().sortedByDescending { it.confidence }.take(limit)

    fun relations(): List<RelationProjection> = emptyList()

    fun goals(minBinding: Float = 0.4f, minForce: Float = 0.3f): List<GoalProjection> =
        field.live(minForce)
            .filterNot { it.flags.has(CommitmentFlags.HOMEOSTATIC) }
            .filterNot { it.flags.has(CommitmentFlags.FOUNDATIONAL) }
            .filter { it.contextualBinding >= minBinding }
            .map { c ->
                GoalProjection(c.locus, c.polarity, c.residualForce, c.contextualBinding)
            }

    fun identity(minForce: Float = 0.25f): List<IdentityProjection> =
        field.live(minForce)
            .filter { it.flags.has(CommitmentFlags.IDENTITY) }
            .map { IdentityProjection(it.locus, it.residualForce, it.consolidationMaturity) }

    fun foundationalValues(minForce: Float = 0.25f): List<BeliefProjection> =
        field.live(minForce)
            .filter { it.flags.has(CommitmentFlags.FOUNDATIONAL) }
            .filterNot { it.flags.has(CommitmentFlags.IDENTITY) }
            .map { c ->
                BeliefProjection(
                    c.locus,
                    c.polarity,
                    c.residualForce,
                    c.flags.has(CommitmentFlags.CONTESTED),
                    c.residualForce,
                    c.consolidationMaturity
                )
            }

    fun neuralActivation(
        minForce: Float = 0.25f,
        maxCount: Int = 48
    ): List<ResidualCommitment> = field.activationSet(minForce, maxCount)
}
