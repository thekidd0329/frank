package frank.cognition

/**
 * ReasoningEngine — portable cognition with endogenous wake/sleep homeostasis.
 *
 * Belief is a projection. Ground truth remains the Residual Commitment Field.
 * Stable commitments physically mature across independent completed sleep episodes;
 * no external evaluation-count map is required, so restart reconstruction preserves learning.
 */
class ReasoningEngine(
    private val field: CommitmentField = CommitmentField(),
    private val defaultDecayFactor: Float = 0.997f,
    private val significantDelta: Float = 0.08f,
    private val loadFoundationalSeeds: Boolean = false
) {
    companion object {
        /** Production/developmental startup: load the owner-seeded origin, purpose, and values once. */
        fun developmental(): ReasoningEngine = ReasoningEngine(loadFoundationalSeeds = true)
    }

    data class CognitiveTickReport(
        val before: SleepStatus,
        val after: SleepStatus,
        val consolidation: SleepCycle.ConsolidationReport?
    )

    private val projections = ProjectionEngine(field)
    private val sleep = SleepHomeostasis(field)
    private val sleepCycle = SleepCycle(field)
    private val monitor = DegradationMonitor(significantDelta)

    init {
        if (loadFoundationalSeeds) ensureFoundationalSeeds()
        monitor.onFieldState(field)
    }

    private fun ensureFoundationalSeeds() {
        FoundationalValueSeeds.commitments().forEach { seed ->
            if (field.get(seed.locus) == null) field.put(seed)
        }
    }

    fun absorb(commitment: ResidualCommitment) {
        field.absorb(commitment)
        monitor.onFieldState(field)
    }

    fun absorbAll(commitments: Iterable<ResidualCommitment>) {
        commitments.forEach { field.absorb(it) }
        monitor.onFieldState(field)
    }

    /** Legacy/manual decay path retained for deterministic unit tests. */
    fun tickDecay() {
        field.decayAll(defaultDecayFactor)
        monitor.onFieldState(field)
    }

    /**
     * One natural cognitive step.
     * Awake activity accumulates sleep pressure. Once asleep, the engine remains in its
     * offline regime until homeostatic pressure falls to the wake threshold.
     */
    fun tick(cognitiveLoad: Float = 0.5f): CognitiveTickReport {
        require(cognitiveLoad in 0.0f..1.0f)
        val before = sleep.status()
        var consolidation: SleepCycle.ConsolidationReport? = null

        if (before.isAsleep) {
            sleepCycle.offlineStep(before.state)
            val transition = sleep.sleepStep()
            if (transition.woke) consolidation = sleepCycle.completeEpisode()
        } else {
            field.decayAll(defaultDecayFactor)
            sleep.awakeStep(cognitiveLoad)
        }

        monitor.onFieldState(field)
        return CognitiveTickReport(before, sleep.status(), consolidation)
    }

    fun reinforce(locus: Locus, delta: Float = 0.1f) {
        val existing = field.get(locus) ?: return
        field.put(existing.reinforced(delta))
        monitor.onFieldState(field)
    }

    /**
     * Settled belief requires force plus physically stored consolidation maturity.
     * Three completed independent sleep episodes at 1/3 maturity each reach the default threshold.
     */
    fun isBelieved(
        locus: Locus,
        forceThreshold: Float = 0.85f,
        maturityThreshold: Float = 0.99f
    ): Boolean {
        val c = field.get(locus) ?: return false
        if (c.flags.has(CommitmentFlags.HOMEOSTATIC)) return false
        return c.residualForce >= forceThreshold &&
            c.consolidationMaturity >= maturityThreshold &&
            !c.flags.has(CommitmentFlags.CONTESTED)
    }

    fun decisionConfidence(locus: Locus): Float {
        val c = field.get(locus) ?: return 0.0f
        if (c.flags.has(CommitmentFlags.CONTESTED)) return 0.0f
        return c.residualForce
    }

    fun canActAutonomously(locus: Locus, threshold: Float = 0.90f): Boolean =
        isBelieved(locus) && decisionConfidence(locus) >= threshold

    fun currentBeliefs(minForce: Float = 0.1f) = projections.beliefs(minForce)
    fun currentGoals() = projections.goals()
    fun currentIdentity() = projections.identity()
    fun foundationalValues() = projections.foundationalValues()

    fun neuralContext(minForce: Float = 0.25f, maxCount: Int = 48) =
        projections.neuralActivation(minForce, maxCount)

    fun sleepStatus(): SleepStatus = sleep.status()
    fun isAsleep(): Boolean = sleep.knowsItIsAsleep()
    fun wantsWake(): Boolean = sleep.status().wantsWake

    fun commitmentSnapshot(): List<ResidualCommitment> = field.snapshot()

    fun restoreFrom(snapshot: List<ResidualCommitment>) {
        field.clear()
        snapshot.forEach { field.put(it) }
        if (loadFoundationalSeeds) ensureFoundationalSeeds()
        monitor.onFieldState(field)
    }

    fun liveCount(threshold: Float = 0.05f): Int = field.live(threshold).size
    fun significantDegradationRate(): Float = monitor.rateOfSignificantTicks()
    fun degradationSummary(): String = monitor.summary()

    fun debugSummary(): String = buildString {
        val sleepStatus = sleep.status()
        appendLine("CommitmentField size=${field.size}")
        appendLine("Live (≥0.05)=${liveCount()}")
        appendLine("state=${sleepStatus.state} sleepPressure=${"%.3f".format(sleepStatus.sleepPressure)} wantsWake=${sleepStatus.wantsWake}")
        appendLine(degradationSummary())
        appendLine("High-force activation set:")
        neuralContext().forEach { c ->
            appendLine(
                "  locus=${c.locus.raw} pol=${c.polarity} force=${"%.3f".format(c.residualForce)}" +
                    " maturity=${"%.3f".format(c.consolidationMaturity)} believed=${isBelieved(c.locus)}"
            )
        }
    }
}
