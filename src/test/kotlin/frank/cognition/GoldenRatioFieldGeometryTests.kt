package frank.cognition

object GoldenRatioFieldGeometryTests {
    @JvmStatic
    fun main(args: Array<String>) {
        orderingIsDeterministic()
        geometryDoesNotInventSemantics()
        maxCountAndThresholdAreRespected()
        println("GoldenRatioFieldGeometryTests: PASS")
    }

    private fun orderingIsDeterministic() {
        val field = CommitmentField()
        field.put(commitment(1, 0.80f))
        field.put(commitment(2, 0.60f))
        field.put(commitment(3, 0.90f))

        val first = GoldenRatioFieldGeometry.orderedActivation(field).map { it.locus }
        val second = GoldenRatioFieldGeometry.orderedActivation(field).map { it.locus }
        check(first == second)
        check(first.size == 3)
    }

    private fun geometryDoesNotInventSemantics() {
        val field = CommitmentField()
        field.put(commitment(7, 0.80f))

        val placements = GoldenRatioFieldGeometry.placements(field)
        check(placements.single().locus == Locus.fromParts(7, 1))
        check(placements.single().phase >= 0.0)
    }

    private fun maxCountAndThresholdAreRespected() {
        val field = CommitmentField()
        field.put(commitment(1, 0.10f))
        field.put(commitment(2, 0.80f))
        field.put(commitment(3, 0.90f))

        val selected = GoldenRatioFieldGeometry.orderedActivation(field, minForce = 0.50f, maxCount = 1)
        check(selected.size == 1)
        check(selected.single().residualForce >= 0.50f)
    }

    private fun commitment(part: Int, force: Float): ResidualCommitment =
        ResidualCommitment(
            locus = Locus.fromParts(part, 1),
            polarity = Polarity.POSITIVE,
            residualForce = force,
            contextualBinding = 0.0f,
            temporalPersistence = TemporalAnchor(generation = 1L)
        )
}
