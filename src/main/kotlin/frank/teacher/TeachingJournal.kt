package frank.teacher

import frank.cognition.DevelopmentalSignal
import frank.cognition.NewbornLearningLoop
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Base64

/**
 * Append-only teaching journal for newborn Frank.
 *
 * The journal stores actual developmental inputs and recovery events. All
 * rebuildable teaching/cognitive surfaces must derive from these events rather
 * than from a second hand-authored semantic database.
 */
class TeachingJournal(private val path: Path) {
    /** Decode the complete developmental history in journal order. */
    fun events(): List<TeachingEvent> {
        if (!Files.exists(path)) return emptyList()
        return Files.readAllLines(path, StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
            .mapIndexed { index, line ->
                decode(line) ?: error("invalid teaching journal entry at line ${index + 1}")
            }
    }

    fun replayInto(loop: NewbornLearningLoop) {
        events().forEach { event ->
            when (event) {
                is TeachingEvent.Experience -> loop.observe(event.raw, event.signal)
                is TeachingEvent.Recovery -> loop.recover(event.amount)
            }
        }
    }

    fun appendExperience(raw: ByteArray, signal: DevelopmentalSignal) {
        require(raw.isNotEmpty()) { "teaching input cannot be empty" }
        append(encode(TeachingEvent.Experience(raw.copyOf(), signal)))
    }

    fun appendRecovery(amount: Float) {
        require(amount in 0f..1f)
        append(encode(TeachingEvent.Recovery(amount)))
    }

    fun eventCount(): Int = events().size

    private fun append(line: String) {
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(
            path,
            "$line\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }

    private fun encode(event: TeachingEvent): String = when (event) {
        is TeachingEvent.Recovery -> "R\t${event.amount}"
        is TeachingEvent.Experience -> {
            val s = event.signal
            listOf(
                "E",
                Base64.getEncoder().encodeToString(event.raw),
                s.novelty,
                s.familiarity,
                s.processingCost,
                s.unresolvedPressure,
                s.resolvedPressure,
                s.reward,
                s.threat,
                s.internalError,
                s.externalPredictionError,
                s.externalThreshold,
                s.sleepGate,
                s.wakeRate,
                s.internalGain,
                s.externalGain,
                s.dampingRate,
                s.sleepDamping,
                s.reliabilityDamping
            ).joinToString("\t")
        }
    }

    private fun decode(line: String): TeachingEvent? {
        val parts = line.split('\t')
        return when (parts.firstOrNull()) {
            "R" -> if (parts.size == 2) {
                TeachingEvent.Recovery(parts[1].toFloat())
            } else null
            "E" -> if (parts.size == 19) {
                TeachingEvent.Experience(
                    raw = Base64.getDecoder().decode(parts[1]),
                    signal = DevelopmentalSignal(
                        novelty = parts[2].toFloat(),
                        familiarity = parts[3].toFloat(),
                        processingCost = parts[4].toFloat(),
                        unresolvedPressure = parts[5].toFloat(),
                        resolvedPressure = parts[6].toFloat(),
                        reward = parts[7].toFloat(),
                        threat = parts[8].toFloat(),
                        internalError = parts[9].toFloat(),
                        externalPredictionError = parts[10].toFloat(),
                        externalThreshold = parts[11].toFloat(),
                        sleepGate = parts[12].toFloat(),
                        wakeRate = parts[13].toFloat(),
                        internalGain = parts[14].toFloat(),
                        externalGain = parts[15].toFloat(),
                        dampingRate = parts[16].toFloat(),
                        sleepDamping = parts[17].toFloat(),
                        reliabilityDamping = parts[18].toFloat()
                    )
                )
            } else null
            else -> null
        }
    }

    companion object {
        fun defaultPath(): Path = Path.of(
            System.getProperty("user.home"),
            ".frank",
            "newborn",
            "teaching.log"
        )
    }
}

sealed class TeachingEvent {
    data class Experience(
        val raw: ByteArray,
        val signal: DevelopmentalSignal
    ) : TeachingEvent()

    data class Recovery(val amount: Float) : TeachingEvent()
}
