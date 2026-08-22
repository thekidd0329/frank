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
 * The journal stores developmental inputs, learned pairings, passive memory
 * aging, and recovery events, then reconstructs state by replaying them. It
 * deliberately avoids persisting a separate hand-authored semantic model.
 */
class TeachingJournal(private val path: Path) {
    fun replayInto(loop: NewbornLearningLoop) {
        if (!Files.exists(path)) return
        Files.readAllLines(path, StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
            .forEachIndexed { index, line ->
                when (val event = decode(line)) {
                    is TeachingEvent.Experience -> loop.observe(event.raw, event.signal)
                    is TeachingEvent.Recovery -> loop.recover(event.amount)
                    is TeachingEvent.Association -> loop.associate(event.first, event.second, event.salience)
                    is TeachingEvent.MemoryAge -> loop.ageMemory(event.steps)
                    null -> error("invalid teaching journal entry at line ${index + 1}")
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

    fun appendAssociation(first: ByteArray, second: ByteArray, salience: Float) {
        require(first.isNotEmpty() && second.isNotEmpty())
        require(salience in 0f..1f)
        append(encode(TeachingEvent.Association(first.copyOf(), second.copyOf(), salience)))
    }

    fun appendMemoryAge(steps: Int) {
        require(steps >= 0)
        append(encode(TeachingEvent.MemoryAge(steps)))
    }

    fun eventCount(): Int {
        if (!Files.exists(path)) return 0
        return Files.readAllLines(path, StandardCharsets.UTF_8).count { it.isNotBlank() }
    }

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
        is TeachingEvent.MemoryAge -> "M\t${event.steps}"
        is TeachingEvent.Association -> listOf(
            "A",
            Base64.getEncoder().encodeToString(event.first),
            Base64.getEncoder().encodeToString(event.second),
            event.salience
        ).joinToString("\t")
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
            "R" -> if (parts.size == 2) TeachingEvent.Recovery(parts[1].toFloat()) else null
            "M" -> if (parts.size == 2) TeachingEvent.MemoryAge(parts[1].toInt()) else null
            "A" -> if (parts.size == 4) {
                TeachingEvent.Association(
                    first = Base64.getDecoder().decode(parts[1]),
                    second = Base64.getDecoder().decode(parts[2]),
                    salience = parts[3].toFloat()
                )
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
    data class Association(val first: ByteArray, val second: ByteArray, val salience: Float) : TeachingEvent()
    data class MemoryAge(val steps: Int) : TeachingEvent()
}
