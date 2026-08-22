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
 * The journal stores the actual developmental inputs, their simulated exposure
 * durations, and recovery events, then reconstructs state by replaying them.
 * It deliberately avoids persisting a separate hand-authored semantic model.
 */
class TeachingJournal(private val path: Path) {
    fun replayInto(loop: NewbornLearningLoop) {
        if (!Files.exists(path)) return
        Files.readAllLines(path, StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
            .forEachIndexed { index, line ->
                when (val event = decode(line)) {
                    is TeachingEvent.Experience -> loop.observe(
                        event.raw,
                        event.signal,
                        durationSeconds = event.durationSeconds
                    )
                    is TeachingEvent.Recovery -> loop.recover(event.amount)
                    null -> error("invalid teaching journal entry at line ${index + 1}")
                }
            }
    }

    fun appendExperience(
        raw: ByteArray,
        signal: DevelopmentalSignal,
        durationSeconds: Float
    ) {
        require(raw.isNotEmpty()) { "teaching input cannot be empty" }
        require(durationSeconds > 0f) { "teaching exposure duration must be positive" }
        append(encode(TeachingEvent.Experience(raw.copyOf(), signal, durationSeconds)))
    }

    fun appendRecovery(amount: Float) {
        require(amount in 0f..1f)
        append(encode(TeachingEvent.Recovery(amount)))
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
        is TeachingEvent.Experience -> {
            val s = event.signal
            listOf(
                "E2",
                Base64.getEncoder().encodeToString(event.raw),
                event.durationSeconds,
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

            // Legacy journals did not record exposure duration. A one-second
            // exposure preserves the old unit-strength residual semantics while
            // making the assumption explicit during migration.
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
                    ),
                    durationSeconds = LEGACY_EXPOSURE_SECONDS
                )
            } else null

            "E2" -> if (parts.size == 20) {
                TeachingEvent.Experience(
                    raw = Base64.getDecoder().decode(parts[1]),
                    durationSeconds = parts[2].toFloat(),
                    signal = DevelopmentalSignal(
                        novelty = parts[3].toFloat(),
                        familiarity = parts[4].toFloat(),
                        processingCost = parts[5].toFloat(),
                        unresolvedPressure = parts[6].toFloat(),
                        resolvedPressure = parts[7].toFloat(),
                        reward = parts[8].toFloat(),
                        threat = parts[9].toFloat(),
                        internalError = parts[10].toFloat(),
                        externalPredictionError = parts[11].toFloat(),
                        externalThreshold = parts[12].toFloat(),
                        sleepGate = parts[13].toFloat(),
                        wakeRate = parts[14].toFloat(),
                        internalGain = parts[15].toFloat(),
                        externalGain = parts[16].toFloat(),
                        dampingRate = parts[17].toFloat(),
                        sleepDamping = parts[18].toFloat(),
                        reliabilityDamping = parts[19].toFloat()
                    )
                )
            } else null

            else -> null
        }
    }

    companion object {
        private const val LEGACY_EXPOSURE_SECONDS = 1.0f

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
        val signal: DevelopmentalSignal,
        val durationSeconds: Float
    ) : TeachingEvent()

    data class Recovery(val amount: Float) : TeachingEvent()
}
