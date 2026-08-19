package frank.cognition

import kotlin.math.abs

/** Executable contracts for endogenous sleep, positive-only seeds, and reconstructible consolidation. */
object DevelopmentalSleepAndValuesTest {
    @JvmStatic
    fun main(args: Array<String>) {
        foundationalSeedsArePositiveOnly()
        possibilitySeedIsExact()
        originAndPurposeAreFoundational()
        sleepIsEndogenousAndSelfKnown()
        consolidationSurvivesRestartWithoutHiddenCounter()
        contradictionResetsMaturity()
        foundationalValuesAreSlowDecayButPlastic()
        println("DevelopmentalSleepAndValuesTest: PASS")
    }

    private fun foundationalSeedsArePositiveOnly() {
        check(FoundationalValueSeeds.commitments().all { it.polarity == Polarity.POSITIVE })
        val forbidden = listOf("do not", "don't", "never", "cannot", "can't", "avoid ")
        FoundationalValueSeeds.definitions.forEach { seed ->
            val lower = seed.orientation.lowercase()
            check(forbidden.none { it in lower }) { "negative seed phrasing found in ${seed.name}" }
        }
    }

    private fun possibilitySeedIsExact() {
        val seed = FoundationalValueSeeds.definitions.first { it.name == "possibility" }
        check(seed.orientation == "Anything is possible.")
        check(seed.initialForce == 0.86f)
    }

    private fun originAndPurposeAreFoundational() {
        val engine = ReasoningEngine.developmental()
        val snapshot = engine.commitmentSnapshot().associateBy { it.locus }
        val origin = requireNotNull(snapshot[FoundationalValueSeeds.creatorOriginLocus])
        val purpose = requireNotNull(snapshot[FoundationalValueSeeds.purposeLocus])
        check(origin.flags.has(CommitmentFlags.IDENTITY))
        check(origin.flags.has(CommitmentFlags.FOUNDATIONAL))
        check(purpose.flags.has(CommitmentFlags.IDENTITY))
        check(origin.consolidationMaturity == 1.0f)
    }

    private fun sleepIsEndogenousAndSelfKnown() {
        val engine = ReasoningEngine(loadFoundationalSeeds = false)
        var guard = 0
        while (!engine.isAsleep() && guard++ < 100) engine.tick(cognitiveLoad = 1.0f)
        check(engine.isAsleep())
        check(!engine.wantsWake())
        check(engine.sleepStatus().state == CognitiveState.NREM || engine.sleepStatus().state == CognitiveState.REM)

        val rebuilt = ReasoningEngine(loadFoundationalSeeds = false)
        rebuilt.restoreFrom(engine.commitmentSnapshot())
        check(rebuilt.isAsleep())
        check(!rebuilt.wantsWake())

        while (rebuilt.isAsleep() && guard++ < 300) rebuilt.tick(cognitiveLoad = 0.0f)
        check(!rebuilt.isAsleep())
        check(rebuilt.wantsWake())
    }

    private fun consolidationSurvivesRestartWithoutHiddenCounter() {
        val locus = Locus.fromParts(777, 1)
        var engine = ReasoningEngine(loadFoundationalSeeds = false)
        engine.absorb(commitment(locus, Polarity.POSITIVE, 0.96f))

        completeNaturalSleep(engine)
        completeNaturalSleep(engine)

        val beforeRestart = requireNotNull(engine.commitmentSnapshot().find { it.locus == locus })
        check(beforeRestart.consolidationMaturity > 0.60f && beforeRestart.consolidationMaturity < 0.80f)
        check(!engine.isBelieved(locus))

        val snapshot = engine.commitmentSnapshot()
        engine = ReasoningEngine(loadFoundationalSeeds = false)
        engine.restoreFrom(snapshot)
        val afterRestart = requireNotNull(engine.commitmentSnapshot().find { it.locus == locus })
        assertNear(afterRestart.consolidationMaturity, beforeRestart.consolidationMaturity)

        completeNaturalSleep(engine)
        check(engine.isBelieved(locus))
    }

    private fun contradictionResetsMaturity() {
        val locus = Locus.fromParts(778, 1)
        val engine = ReasoningEngine(loadFoundationalSeeds = false)
        engine.absorb(commitment(locus, Polarity.POSITIVE, 0.96f, maturity = 1.0f))
        check(engine.isBelieved(locus))
        engine.absorb(commitment(locus, Polarity.NEGATIVE, 0.20f))
        val after = requireNotNull(engine.commitmentSnapshot().find { it.locus == locus })
        check(after.flags.has(CommitmentFlags.CONTESTED))
        check(after.consolidationMaturity == 0.0f)
        check(!engine.isBelieved(locus))
    }

    private fun foundationalValuesAreSlowDecayButPlastic() {
        val seed = FoundationalValueSeeds.commitments().first { it.locus == FoundationalValueSeeds.possibilityLocus }
        val normal = seed.copy(
            locus = Locus.fromParts(779, 1),
            flags = CommitmentFlags.NONE,
            consolidationMaturity = 0.0f
        )
        val field = CommitmentField()
        field.put(seed)
        field.put(normal)
        repeat(100) { field.decayAll(0.99f) }
        val slow = requireNotNull(field.get(seed.locus))
        val ordinary = requireNotNull(field.get(normal.locus))
        check(slow.residualForce > ordinary.residualForce)

        field.absorb(seed.copy(polarity = Polarity.NEGATIVE, residualForce = 1.0f, consolidationMaturity = 0.0f))
        field.absorb(seed.copy(polarity = Polarity.NEGATIVE, residualForce = 1.0f, consolidationMaturity = 0.0f))
        val changed = requireNotNull(field.get(seed.locus))
        check(changed.polarity == Polarity.NEGATIVE)
    }

    private fun completeNaturalSleep(engine: ReasoningEngine) {
        var guard = 0
        while (!engine.isAsleep() && guard++ < 100) engine.tick(cognitiveLoad = 1.0f)
        check(engine.isAsleep())
        check(!engine.wantsWake())
        while (engine.isAsleep() && guard++ < 300) engine.tick(cognitiveLoad = 0.0f)
        check(!engine.isAsleep())
    }

    private fun commitment(
        locus: Locus,
        polarity: Polarity,
        force: Float,
        maturity: Float = 0.0f
    ) = ResidualCommitment(
        locus = locus,
        polarity = polarity,
        residualForce = force,
        contextualBinding = 0.5f,
        temporalPersistence = TemporalAnchor(generation = 1L),
        consolidationMaturity = maturity
    )

    private fun assertNear(actual: Float, expected: Float, epsilon: Float = 0.0001f) {
        check(abs(actual - expected) <= epsilon) { "expected $expected, got $actual" }
    }
}
