package frank.cognition

import kotlin.math.abs
import kotlin.math.min

/**
 * A biological consolidation gate should not require a durable memory to exist
 * before that memory can be consolidated. Reconstruction here means pattern
 * completion from a partial transient cue during replay, not retrieval from LTM.
 */
data class PatternCompletionProbe(
    val locus: Locus,
    val cueFraction: Float,
    val recoveredPolarity: Polarity,
    val recoveredMagnitude: Float
) {
    init {
        require(cueFraction in 0f..1f)
        require(recoveredMagnitude >= 0f)
    }
}

class BiologicalConsolidator(
    private val policy: ConsolidationPolicy = ReconstructionWeightedConsolidationPolicy
) {
    private val tags = linkedMapOf<Long, ConsolidationTag>()

    fun tag(locus: Locus, residual: OpponentResidual, tick: Long) {
        tags.putIfAbsent(locus.raw, ConsolidationTag(locus, residual, tick))
    }

    fun noteCompetitionSurvival(locus: Locus) {
        tags[locus.raw]?.let { tags[locus.raw] = it.survivedCompetition() }
    }

    fun noteContextReinstatement(locus: Locus) {
        tags[locus.raw]?.let { tags[locus.raw] = it.reinstatedByContext() }
    }

    fun replayWithPartialCue(locus: Locus, current: OpponentResidual, cueFraction: Float): PatternCompletionProbe? {
        val tag = tags[locus.raw] ?: return null
        require(cueFraction in 0f..1f)
        val expected = tag.residualAtTag.dominantPolarity
        val currentPolarity = current.dominantPolarity
        val completion = if (currentPolarity == expected) abs(current.signedProjection) else 0f
        var next = tag.replayed()
        if (cueFraction < 1f && currentPolarity == expected && completion > 0.05f) next = next.reconstructed()
        tags[locus.raw] = next
        return PatternCompletionProbe(locus, cueFraction, currentPolarity, completion)
    }

    fun eligible(locus: Locus, current: OpponentResidual): Boolean {
        val tag = tags[locus.raw] ?: return false
        return policy.shouldConsolidate(
            ConsolidationEvidence(
                replayCount = tag.replayCount,
                successfulReconstructionCount = tag.successfulReconstructionCount,
                competitionSurvivalCount = tag.competitionSurvivalCount,
                contextualReinstatementCount = tag.contextualReinstatementCount,
                currentResidual = current
            )
        )
    }

    fun snapshot(locus: Locus): ConsolidationTag? = tags[locus.raw]
}
