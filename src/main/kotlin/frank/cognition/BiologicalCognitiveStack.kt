package frank.cognition

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * End-to-end biology-first stack.
 *
 * sensation -> developmental locus recruitment -> opponent transient state
 * -> temporary tag -> replay/reconstruction -> durable residual projection
 * -> retrieval bias -> reactivation
 *
 * Durable semantic state is a projection of surviving lower-layer dynamics.
 */
class BiologicalCognitiveStack(
    private val cortex: DevelopmentalCortex = DevelopmentalCortex(),
    private val field: CommitmentField = CommitmentField(),
    private val consolidationPolicy: ConsolidationPolicy = ReconstructionWeightedConsolidationPolicy,
    private val floor: Float = 0.0f
) {
    private val transient = linkedMapOf<Long, OpponentResidual>()
    private val tags = linkedMapOf<Long, ConsolidationTag>()
    private var tick: Long = 0

    data class StepResult(
        val tick: Long,
        val recruitment: LocusRecruitment,
        val residual: OpponentResidual,
        val consolidated: ResidualCommitment?
    )

    fun experience(
        features: SensoryFeatureVector,
        appetitiveDrive: Float = 0f,
        aversiveDrive: Float = 0f,
        contextualBinding: Float = 0.5f
    ): StepResult {
        require(appetitiveDrive >= 0f && aversiveDrive >= 0f)
        require(contextualBinding in 0f..1f)
        tick++
        val recruitment = cortex.observe(features, tick)
        val locus = recruitment.primary
        val old = transient[locus.raw] ?: OpponentResidual(floor, floor)

        // Separate nonnegative opponent channels; no signed neuron is required.
        val next = OpponentResidual(
            positivePath = min(1.61803398875f, max(floor, old.positivePath * 0.99f) + appetitiveDrive),
            opponentPath = min(1.61803398875f, max(floor, old.opponentPath * 0.99f) + aversiveDrive)
        )
        transient[locus.raw] = next

        var tag = tags[locus.raw]
        val salience = abs(next.signedProjection)
        if (tag == null && salience >= 0.10f) {
            tag = ConsolidationTag(locus, next, tick)
        }
        if (tag != null) {
            tag = tag.survivedCompetition()
            if (!recruitment.born) tag = tag.reinstatedByContext()
            tags[locus.raw] = tag
        }

        val consolidated = maybeConsolidate(locus, contextualBinding)
        return StepResult(tick, recruitment, next, consolidated)
    }

    /** Offline replay is endogenous reactivation, not copying a record to another database. */
    fun replay(locus: Locus, contextualBinding: Float = 0.5f): ResidualCommitment? {
        val tag = tags[locus.raw] ?: return null
        tags[locus.raw] = tag.replayed()
        return maybeConsolidate(locus, contextualBinding)
    }

    /**
     * Reconstruction succeeds when durable bias can re-establish the same opponent dominance.
     * Current activation is deliberately discarded first.
     */
    fun reconstruct(locus: Locus): OpponentResidual? {
        transient.remove(locus.raw)
        val durable = field.get(locus) ?: return null
        val bias = DurableRetrievalProjection.biasFrom(durable)
        val rebuilt = OpponentResidual(
            positivePath = max(floor, bias.positiveBias),
            opponentPath = max(floor, bias.opponentBias)
        )
        transient[locus.raw] = rebuilt
        val expected = durable.polarity
        if (rebuilt.dominantPolarity == expected) {
            tags[locus.raw]?.let { tags[locus.raw] = it.reconstructed() }
        }
        return rebuilt
    }

    fun durable(locus: Locus): ResidualCommitment? = field.get(locus)
    fun active(locus: Locus): OpponentResidual? = transient[locus.raw]
    fun associations(locus: Locus) = cortex.neighbors(locus)
    fun locusCount(): Int = cortex.snapshotLoci().size
    fun durableCount(): Int = field.size

    private fun maybeConsolidate(locus: Locus, contextualBinding: Float): ResidualCommitment? {
        val tag = tags[locus.raw] ?: return null
        val current = transient[locus.raw] ?: return null
        val evidence = ConsolidationEvidence(
            replayCount = tag.replayCount,
            successfulReconstructionCount = tag.successfulReconstructionCount,
            competitionSurvivalCount = tag.competitionSurvivalCount,
            contextualReinstatementCount = tag.contextualReinstatementCount,
            currentResidual = current
        )
        if (!consolidationPolicy.shouldConsolidate(evidence)) return null
        val projected = ResidualCommitmentProjection.fromConsolidatedState(
            locus = locus,
            residual = current,
            contextualBinding = contextualBinding,
            generation = tick
        )
        if (projected.polarity != Polarity.NEUTRAL && projected.residualForce > 0f) field.put(projected)
        return projected
    }
}
