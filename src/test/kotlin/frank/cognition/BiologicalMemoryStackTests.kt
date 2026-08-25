package frank.cognition

object BiologicalMemoryStackTests {
    @JvmStatic
    fun main(args: Array<String>) {
        opponentPathsProjectSignedState()
        opponentHistorySurvivesProjectionCompression()
        oneShotStrengthDoesNotForceConsolidation()
        replayAndReconstructionCanEarnConsolidation()
        durableCommitmentRebiasesLowerLayer()
        projectionCanBeDestroyedAndRebuilt()
        println("BiologicalMemoryStackTests: PASS")
    }

    private fun opponentPathsProjectSignedState() {
        val positive = OpponentResidual(positivePath = 0.90f, opponentPath = 0.30f)
        check(kotlin.math.abs(positive.signedProjection - 0.60f) < 0.0001f)
        check(positive.dominantPolarity == Polarity.POSITIVE)

        val negative = OpponentResidual(positivePath = 0.20f, opponentPath = 0.80f)
        check(kotlin.math.abs(negative.signedProjection + 0.60f) < 0.0001f)
        check(negative.dominantPolarity == Polarity.NEGATIVE)
    }

    private fun opponentHistorySurvivesProjectionCompression() {
        val a = OpponentResidual(positivePath = 0.90f, opponentPath = 0.30f)
        val b = OpponentResidual(positivePath = 1.40f, opponentPath = 0.80f)
        check(kotlin.math.abs(a.signedProjection - b.signedProjection) < 0.0001f)
        check(a != b)
    }

    private fun oneShotStrengthDoesNotForceConsolidation() {
        val evidence = ConsolidationEvidence(
            replayCount = 0,
            successfulReconstructionCount = 0,
            competitionSurvivalCount = 0,
            contextualReinstatementCount = 0,
            currentResidual = OpponentResidual(1.0f, 0.0f)
        )
        check(!ReconstructionWeightedConsolidationPolicy.shouldConsolidate(evidence))
    }

    private fun replayAndReconstructionCanEarnConsolidation() {
        val evidence = ConsolidationEvidence(
            replayCount = 2,
            successfulReconstructionCount = 2,
            competitionSurvivalCount = 0,
            contextualReinstatementCount = 0,
            currentResidual = OpponentResidual(0.82f, 0.18f)
        )
        check(ReconstructionWeightedConsolidationPolicy.shouldConsolidate(evidence))
    }

    private fun durableCommitmentRebiasesLowerLayer() {
        val locus = Locus.fromParts(9, 4)
        val durable = ResidualCommitment(
            locus = locus,
            polarity = Polarity.NEGATIVE,
            residualForce = 0.72f,
            contextualBinding = 0.55f,
            temporalPersistence = TemporalAnchor(generation = 8)
        )
        val bias = DurableRetrievalProjection.biasFrom(durable)
        check(bias.locus == locus)
        check(bias.positiveBias == 0f)
        check(kotlin.math.abs(bias.opponentBias - 0.72f) < 0.0001f)
        check(kotlin.math.abs(bias.contextualBinding - 0.55f) < 0.0001f)
    }

    private fun projectionCanBeDestroyedAndRebuilt() {
        val locus = Locus.fromParts(7, 11)
        val lower = OpponentResidual(positivePath = 1.15f, opponentPath = 0.47f)

        var projection: ResidualCommitment? = ResidualCommitmentProjection.fromConsolidatedState(
            locus = locus,
            residual = lower,
            contextualBinding = 0.44f,
            generation = 21
        )
        val first = requireNotNull(projection)

        // Destroy the derived semantic projection.
        projection = null
        check(projection == null)

        // Reconstruct from lawful lower-layer consolidated state only.
        val rebuilt = ResidualCommitmentProjection.fromConsolidatedState(
            locus = locus,
            residual = lower,
            contextualBinding = 0.44f,
            generation = 21
        )

        check(rebuilt.locus == first.locus)
        check(rebuilt.polarity == first.polarity)
        check(kotlin.math.abs(rebuilt.residualForce - first.residualForce) < 0.0001f)
        check(kotlin.math.abs(rebuilt.contextualBinding - first.contextualBinding) < 0.0001f)
    }
}
