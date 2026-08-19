package frank.cognition

/**
 * Tiny executable contract checks for the newborn ground rules.
 * No test framework dependency is required; CI can compile this with the rest
 * of the Kotlin sources or call main explicitly later.
 */
object ResidualCommitmentTests {
    @JvmStatic
    fun main(args: Array<String>) {
        decayNeverInvertsPolarity()
        strongerContradictionKeepsResidualDifference()
        snapshotReconstructsProjectionInputs()
        println("ResidualCommitmentTests: PASS")
    }

    private fun decayNeverInvertsPolarity() {
        val field = CommitmentField()
        val locus = Locus.fromParts(1, 1)
        field.put(commitment(locus, Polarity.POSITIVE, 0.8f))
        field.decayAll(0.5f)

        val after = requireNotNull(field.get(locus))
        check(after.polarity == Polarity.POSITIVE)
        check(kotlin.math.abs(after.residualForce - 0.4f) < 0.0001f)
    }

    private fun strongerContradictionKeepsResidualDifference() {
        val field = CommitmentField()
        val locus = Locus.fromParts(2, 1)
        field.absorb(commitment(locus, Polarity.POSITIVE, 0.4f))
        field.absorb(commitment(locus, Polarity.NEGATIVE, 0.9f))

        val after = requireNotNull(field.get(locus))
        check(after.polarity == Polarity.NEGATIVE)
        check(kotlin.math.abs(after.residualForce - 0.5f) < 0.0001f)
    }

    private fun snapshotReconstructsProjectionInputs() {
        val original = ReasoningEngine()
        original.absorb(commitment(Locus.fromParts(7, 3), Polarity.POSITIVE, 0.81f))

        val recovered = ReasoningEngine()
        recovered.restoreFrom(original.commitmentSnapshot())

        check(recovered.currentBeliefs().size == original.currentBeliefs().size)
        check(recovered.neuralContext() == original.neuralContext())
    }

    private fun commitment(locus: Locus, polarity: Polarity, force: Float) =
        ResidualCommitment(
            locus = locus,
            polarity = polarity,
            residualForce = force,
            contextualBinding = 0.5f,
            temporalPersistence = TemporalAnchor(generation = 1L)
        )
}
