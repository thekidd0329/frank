package frank.cli

import frank.cognition.CommitmentFlags
import frank.cognition.ReasoningEngine
import frank.cognition.ResidualCommitment

object FieldPrinter {
    fun raw(snapshot: List<ResidualCommitment>, labelFor: (Long) -> String?): String = buildString {
        if (snapshot.isEmpty()) {
            appendLine("Frank field: empty")
            return@buildString
        }
        snapshot.sortedBy { it.locus.raw }.forEach { c ->
            val label = labelFor(c.locus.raw)?.let { " label=$it" } ?: ""
            appendLine(
                "locus=${c.locus.raw}$label polarity=${c.polarity} force=${fmt(c.residualForce)} " +
                    "binding=${fmt(c.contextualBinding)} persistence=${c.temporalPersistence.generation}:${c.temporalPersistence.recencyDelta} " +
                    "maturity=${fmt(c.consolidationMaturity)} flags=${c.flags.bits}"
            )
        }
    }

    fun projections(engine: ReasoningEngine, labelFor: (Long) -> String?): String = buildString {
        val beliefs = engine.currentBeliefs(0.05f)
        val goals = engine.currentGoals()
        appendLine("Belief projections (${beliefs.size}):")
        beliefs.forEach { b ->
            val label = labelFor(b.locus.raw) ?: b.locus.raw.toString()
            appendLine("  $label polarity=${b.polarity} confidence=${fmt(b.confidence)} contested=${b.contested} maturity=${fmt(b.consolidationMaturity)}")
        }
        appendLine("Goal projections (${goals.size}):")
        goals.forEach { g ->
            val label = labelFor(g.locus.raw) ?: g.locus.raw.toString()
            appendLine("  $label desired=${g.desiredPolarity} urgency=${fmt(g.urgency)} binding=${fmt(g.binding)}")
        }
        appendLine("Identity projections: ${engine.currentIdentity().size}")
    }

    fun gaps(snapshot: List<ResidualCommitment>, labelFor: (Long) -> String?): List<ResidualCommitment> =
        snapshot
            .filter { it.residualForce < 0.45f || it.flags.has(CommitmentFlags.CONTESTED) }
            .sortedWith(
                compareByDescending<ResidualCommitment> { it.flags.has(CommitmentFlags.CONTESTED) }
                    .thenBy { it.residualForce }
            )

    fun gapText(snapshot: List<ResidualCommitment>, labelFor: (Long) -> String?): String = buildString {
        val gaps = gaps(snapshot, labelFor)
        if (gaps.isEmpty()) {
            appendLine("no weak or contested loci")
            return@buildString
        }
        gaps.forEach { c ->
            val label = labelFor(c.locus.raw) ?: c.locus.raw.toString()
            appendLine("$label force=${fmt(c.residualForce)} contested=${c.flags.has(CommitmentFlags.CONTESTED)}")
        }
    }

    private fun fmt(value: Float): String = "%.3f".format(value)
}
