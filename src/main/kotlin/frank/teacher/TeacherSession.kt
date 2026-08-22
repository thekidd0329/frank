package frank.teacher

import frank.cognition.CommitmentFlags
import frank.cognition.DevelopmentalSignal
import frank.cognition.Locus
import frank.cognition.NewbornLearningLoop
import frank.cognition.NewbornState
import frank.cognition.Polarity
import frank.cognition.ReasoningEngine
import frank.cognition.ResidualCommitment
import frank.cognition.SleepStatus
import frank.cognition.TemporalAnchor
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.ceil

class TeacherSession(
    private val journal: TeachingJournal = TeachingJournal(TeachingJournal.defaultPath())
) {
    private val loop = NewbornLearningLoop()
    private val brain = ReasoningEngine.developmental()
    private var pending: ByteArray? = null

    init {
        journal.events().forEachIndexed { index, event ->
            when (event) {
                is TeachingEvent.Experience -> {
                    loop.observe(event.raw, event.signal)
                    replayExperienceIntoBrain(event.raw, event.signal, generation = index.toLong() + 1L)
                }
                is TeachingEvent.Recovery -> {
                    loop.recover(event.amount)
                    replayRecoveryIntoBrain(event.amount)
                }
            }
        }
    }

    fun stage(text: String) {
        require(text.isNotBlank()) { "observation cannot be blank" }
        pending = text.toByteArray(StandardCharsets.UTF_8)
    }

    fun hasPendingObservation(): Boolean = pending != null

    fun commit(): Long = applyPending(DevelopmentalSignal(novelty = 1f))

    fun reinforce(strength: Float = 1f): Long {
        require(strength in 0f..1f)
        return applyPending(DevelopmentalSignal(novelty = 1f, reward = strength))
    }

    fun contradict(strength: Float = 1f): Long {
        require(strength in 0f..1f)
        return applyPending(DevelopmentalSignal(novelty = 1f, threat = strength))
    }

    /**
     * Test-harness recovery step. This never commands Frank to wake.
     * If the developmental engine is asleep, the amount advances offline time;
     * the engine wakes only when its own homeostatic threshold says it is rested.
     */
    fun recover(amount: Float) {
        require(amount in 0f..1f)
        journal.appendRecovery(amount)
        loop.recover(amount)
        replayRecoveryIntoBrain(amount)
    }

    fun state(): NewbornState = loop.reconstructState()

    /** Legacy scalar newborn field retained while callers migrate to brainSnapshot(). */
    fun residualField(): Map<Long, Float> = loop.residualField

    /** Actual developmental cognition field used by sleep, consolidation, values, and projections. */
    fun brainSnapshot(): List<ResidualCommitment> = brain.commitmentSnapshot()

    fun brainCommitment(locus: Long): ResidualCommitment? =
        brain.commitmentSnapshot().firstOrNull { it.locus.raw == locus }

    fun sleepStatus(): SleepStatus = brain.sleepStatus()

    fun isAsleep(): Boolean = brain.isAsleep()

    fun wantsWake(): Boolean = brain.wantsWake()

    fun eventCount(): Int = journal.eventCount()

    private fun applyPending(signal: DevelopmentalSignal): Long {
        val raw = pending ?: error("no staged observation; use /observe first")
        check(!brain.isAsleep()) { "Frank is asleep; teaching waits until natural wake" }

        // Journal first so a crash can only lose volatile in-memory work, never the teaching event.
        val generation = journal.eventCount().toLong() + 1L
        journal.appendExperience(raw, signal)

        val locus = loop.observe(raw, signal)
        replayExperienceIntoBrain(raw, signal, generation)
        pending = null
        return locus
    }

    private fun replayExperienceIntoBrain(raw: ByteArray, signal: DevelopmentalSignal, generation: Long) {
        val signedEvidence = signal.reward - signal.threat
        if (abs(signedEvidence) > 0.0001f) {
            val polarity = if (signedEvidence > 0f) Polarity.POSITIVE else Polarity.NEGATIVE
            brain.absorb(
                ResidualCommitment(
                    locus = Locus(loop.locusFor(raw)),
                    polarity = polarity,
                    residualForce = abs(signedEvidence).coerceIn(0f, 1f),
                    contextualBinding = 0.5f,
                    temporalPersistence = TemporalAnchor(generation = generation),
                    flags = CommitmentFlags.EXPLICIT
                )
            )
        }

        // Cognitive work contributes to endogenous sleep pressure even when the
        // observation itself was neutral and remains only in the episodic journal.
        brain.tick(cognitiveLoad = signal.processingCost.coerceIn(0f, 1f))
    }

    private fun replayRecoveryIntoBrain(amount: Float) {
        if (!brain.isAsleep()) return

        // SleepHomeostasis currently recovers 0.08 pressure per offline step.
        // Map the harness recovery amount to deterministic offline steps while
        // leaving the wake decision entirely inside ReasoningEngine.
        val steps = maxOf(1, ceil(amount / 0.08f).toInt())
        repeat(steps) {
            if (brain.isAsleep()) brain.tick(cognitiveLoad = 0f)
        }
    }
}
