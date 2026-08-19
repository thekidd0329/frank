package frank.cognition

/**
 * Minimal smoke example — not a unit test, just a readable demonstration
 * that the reconstruction invariant and decision path work.
 *
 * Run from any Kotlin main or scratch file that has this package on the classpath.
 */
object ExampleUsage {
    @JvmStatic
    fun main(args: Array<String>) {
        val engine = ReasoningEngine()

        // Simulate incoming evidence from the existing compact-claim / ontology world
        val bridge = EvidenceToCommitment()
        val evidence = listOf(
            IncomingEvidence(axisId = 1, valueId = 42, polarityPositive = true, confidence = 0.92f, recency = 1L, isExplicit = true),
            IncomingEvidence(axisId = 1, valueId = 42, polarityPositive = false, confidence = 0.40f, recency = 2L, isInferred = true), // contest
            IncomingEvidence(axisId = 7, valueId = 3, polarityPositive = true, confidence = 0.81f, recency = 3L),
            IncomingEvidence(axisId = 9, valueId = 11, polarityPositive = true, confidence = 0.55f, recency = 4L)
        )

        engine.absorbAll(bridge.convertAll(evidence))

        println("=== After absorb ===")
        println(engine.debugSummary())

        // Decision support
        val locusHigh = LocusAddressing.fromAxisValue(1, 42)
        val locusOther = LocusAddressing.fromAxisValue(7, 3)
        println("decisionConfidence(1,42) = ${engine.decisionConfidence(locusHigh)}")
        println("canActAutonomously(1,42) = ${engine.canActAutonomously(locusHigh)}")
        println("canActAutonomously(7,3)  = ${engine.canActAutonomously(locusOther)}")

        // Reconstruction invariant demo
        val snapshot = engine.commitmentSnapshot()
        val recovered = ReasoningEngine()
        recovered.restoreFrom(snapshot)

        println("\n=== After restore from snapshot only ===")
        println(recovered.debugSummary())
        println("Beliefs regenerated: ${recovered.currentBeliefs().size}")
        println("Goals regenerated:   ${recovered.currentGoals().size}")
    }
}
