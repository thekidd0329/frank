package frank.cognition

/**
 * Biology-first memory stack scaffolding.
 *
 * This does not replace the C-phi-plus substrate. It defines the semantic contract
 * for consuming lower-layer dynamics without turning working memory or long-term
 * memory into ordinary software save/read operations.
 */

data class OpponentResidual(
    val positivePath: Float,
    val opponentPath: Float
) {
    init {
        require(positivePath >= 0f)
        require(opponentPath >= 0f)
    }

    val signedProjection: Float get() = positivePath - opponentPath

    val dominantPolarity: Polarity
        get() = when {
            signedProjection > 0f -> Polarity.POSITIVE
            signedProjection < 0f -> Polarity.NEGATIVE
            else -> Polarity.NEUTRAL
        }
}

data class TransientEnsembleState(
    val locus: Locus,
    val opponentResidual: OpponentResidual,
    val active: Boolean,
    val inhibited: Boolean,
    val recentlyReactivated: Boolean,
    val ageTicks: Long
)

data class ConsolidationTag(
    val locus: Locus,
    val residualAtTag: OpponentResidual,
    val taggedAtTick: Long,
    val replayCount: Int = 0,
    val successfulReconstructionCount: Int = 0,
    val competitionSurvivalCount: Int = 0,
    val contextualReinstatementCount: Int = 0
) {
    init {
        require(taggedAtTick >= 0)
        require(replayCount >= 0)
        require(successfulReconstructionCount >= 0)
        require(competitionSurvivalCount >= 0)
        require(contextualReinstatementCount >= 0)
    }

    fun replayed() = copy(replayCount = replayCount + 1)
    fun reconstructed() = copy(successfulReconstructionCount = successfulReconstructionCount + 1)
    fun survivedCompetition() = copy(competitionSurvivalCount = competitionSurvivalCount + 1)
    fun reinstatedByContext() = copy(contextualReinstatementCount = contextualReinstatementCount + 1)
}

data class ConsolidationEvidence(
    val replayCount: Int,
    val successfulReconstructionCount: Int,
    val competitionSurvivalCount: Int,
    val contextualReinstatementCount: Int,
    val currentResidual: OpponentResidual
)

/**
 * Promotion policy is intentionally isolated from persistence.
 * A tag is temporary evidence of eligibility, not durable cognitive truth.
 */
fun interface ConsolidationPolicy {
    fun shouldConsolidate(evidence: ConsolidationEvidence): Boolean
}

/**
 * Conservative initial policy for experimentation, not a claim of final biology.
 * No one-shot amplitude threshold can independently force long-term storage.
 */
object ReconstructionWeightedConsolidationPolicy : ConsolidationPolicy {
    override fun shouldConsolidate(evidence: ConsolidationEvidence): Boolean {
        val replayed = evidence.replayCount >= 2
        val reconstructable = evidence.successfulReconstructionCount >= 2
        val survived = evidence.competitionSurvivalCount >= 1
        val contextReturned = evidence.contextualReinstatementCount >= 1
        return reconstructable && (replayed || (survived && contextReturned))
    }
}

/**
 * Converts consolidated opponent-path state into the existing durable semantic atom.
 * The signed force is a projection of lower-layer opponent history rather than an
 * independently evolving duplicate truth.
 */
object ResidualCommitmentProjection {
    fun fromConsolidatedState(
        locus: Locus,
        residual: OpponentResidual,
        contextualBinding: Float,
        generation: Long,
        provenance: ProvenanceHandle? = null,
        flags: CommitmentFlags = CommitmentFlags.NONE
    ): ResidualCommitment {
        require(contextualBinding in 0f..1f)
        val signed = residual.signedProjection
        val polarity = when {
            signed > 0f -> Polarity.POSITIVE
            signed < 0f -> Polarity.NEGATIVE
            else -> Polarity.NEUTRAL
        }
        return ResidualCommitment(
            locus = locus,
            polarity = polarity,
            residualForce = kotlin.math.abs(signed).coerceIn(0f, 1f),
            contextualBinding = contextualBinding,
            temporalPersistence = TemporalAnchor(generation = generation),
            provenance = provenance,
            flags = flags
        )
    }
}

/**
 * Retrieval does not return a finished belief. It returns a lower-layer bias request.
 * The substrate must still reactivate/reconstruct an ensemble before semantic
 * projections become current again.
 */
data class ReactivationBias(
    val locus: Locus,
    val positiveBias: Float,
    val opponentBias: Float,
    val contextualBinding: Float
)

object DurableRetrievalProjection {
    fun biasFrom(commitment: ResidualCommitment): ReactivationBias {
        val force = commitment.residualForce
        return when (commitment.polarity) {
            Polarity.POSITIVE -> ReactivationBias(commitment.locus, force, 0f, commitment.contextualBinding)
            Polarity.NEGATIVE -> ReactivationBias(commitment.locus, 0f, force, commitment.contextualBinding)
            Polarity.NEUTRAL -> ReactivationBias(commitment.locus, 0f, 0f, commitment.contextualBinding)
        }
    }
}
