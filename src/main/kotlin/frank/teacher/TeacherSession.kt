package frank.teacher

import frank.cognition.DevelopmentalSignal
import frank.cognition.NewbornLearningLoop
import frank.cognition.NewbornState
import java.nio.charset.StandardCharsets

class TeacherSession(
    private val journal: TeachingJournal = TeachingJournal(TeachingJournal.defaultPath())
) {
    private val loop = NewbornLearningLoop()
    private var pending: ByteArray? = null

    init {
        journal.replayInto(loop)
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

    fun recover(amount: Float) {
        require(amount in 0f..1f)
        loop.recover(amount)
        journal.appendRecovery(amount)
    }

    fun state(): NewbornState = loop.reconstructState()

    fun residualField(): Map<Long, Float> = loop.residualField

    fun eventCount(): Int = journal.eventCount()

    private fun applyPending(signal: DevelopmentalSignal): Long {
        val raw = pending ?: error("no staged observation; use /observe first")
        val locus = loop.observe(raw, signal)
        journal.appendExperience(raw, signal)
        pending = null
        return locus
    }
}
