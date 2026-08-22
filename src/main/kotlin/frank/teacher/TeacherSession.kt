package frank.teacher

import frank.cognition.DevelopmentalAssociationField
import frank.cognition.DevelopmentalSignal
import frank.cognition.MemoryDynamicsProfile
import frank.cognition.NewbornLearningLoop
import frank.cognition.NewbornState
import java.nio.charset.StandardCharsets

class TeacherSession(
    private val journal: TeachingJournal = TeachingJournal(TeachingJournal.defaultPath()),
    memoryProfile: MemoryDynamicsProfile = MemoryDynamicsProfile.CONTROL
) {
    private val loop = NewbornLearningLoop(memoryProfile = memoryProfile)
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

    /**
     * Pair two opaque experiences without declaring that either one is the
     * semantic definition of the other. Repetition is what strengthens them.
     */
    fun associate(first: String, second: String, salience: Float = 1f) {
        require(first.isNotBlank() && second.isNotBlank())
        require(salience in 0f..1f)
        val firstRaw = first.toByteArray(StandardCharsets.UTF_8)
        val secondRaw = second.toByteArray(StandardCharsets.UTF_8)
        loop.associate(firstRaw, secondRaw, salience)
        journal.appendAssociation(firstRaw, secondRaw, salience)
    }

    fun recall(text: String, limit: Int = 8): List<DevelopmentalAssociationField.Recall> {
        require(text.isNotBlank())
        return loop.recall(text.toByteArray(StandardCharsets.UTF_8), limit)
    }

    fun locusOf(text: String): Long = loop.locusOf(text.toByteArray(StandardCharsets.UTF_8))

    fun ageMemory(steps: Int = 1) {
        require(steps >= 0)
        loop.ageMemory(steps)
        journal.appendMemoryAge(steps)
    }

    fun recover(amount: Float) {
        require(amount in 0f..1f)
        loop.recover(amount)
        journal.appendRecovery(amount)
    }

    fun state(): NewbornState = loop.reconstructState()

    fun residualField(): Map<Long, Float> = loop.residualField

    fun associationField(): Map<DevelopmentalAssociationField.Edge, Float> = loop.associationField

    fun eventCount(): Int = journal.eventCount()

    private fun applyPending(signal: DevelopmentalSignal): Long {
        val raw = pending ?: error("no staged observation; use /observe first")
        val locus = loop.observe(raw, signal)
        journal.appendExperience(raw, signal)
        pending = null
        return locus
    }
}
