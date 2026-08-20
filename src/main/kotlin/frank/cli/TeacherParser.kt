package frank.cli

sealed interface TeacherCommand {
    data class Observe(val axis: String, val value: String, val force: Float, val positive: Boolean) : TeacherCommand
    data class Say(val text: String) : TeacherCommand
    data class Reinforce(val axis: String, val value: String, val force: Float) : TeacherCommand
    data class Contradict(val axis: String, val value: String, val force: Float) : TeacherCommand
    data class Tick(val count: Int) : TeacherCommand
    data object Field : TeacherCommand
    data object Project : TeacherCommand
    data object Gaps : TeacherCommand
    data object Ask : TeacherCommand
    data object Restore : TeacherCommand
    data object Help : TeacherCommand
    data object Quit : TeacherCommand
    data class Invalid(val reason: String) : TeacherCommand
}

object TeacherParser {
    fun parse(line: String): TeacherCommand {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return TeacherCommand.Invalid("empty input")
        if (!trimmed.startsWith('/')) return TeacherCommand.Say(trimmed)

        val parts = trimmed.split(Regex("\\s+"))
        return when (parts[0].lowercase()) {
            "/observe" -> parseObserve(parts)
            "/say" -> TeacherCommand.Say(trimmed.removePrefix(parts[0]).trim())
            "/reinforce" -> parsePressure(parts, positive = true)
            "/contradict" -> parsePressure(parts, positive = false)
            "/tick" -> TeacherCommand.Tick(parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
            "/field" -> TeacherCommand.Field
            "/project" -> TeacherCommand.Project
            "/gaps" -> TeacherCommand.Gaps
            "/ask" -> TeacherCommand.Ask
            "/restore" -> TeacherCommand.Restore
            "/help" -> TeacherCommand.Help
            "/quit", "/exit" -> TeacherCommand.Quit
            else -> TeacherCommand.Invalid("unknown command: ${parts[0]}")
        }
    }

    private fun parseObserve(parts: List<String>): TeacherCommand {
        if (parts.size < 3) return TeacherCommand.Invalid("usage: /observe <axis> <value> [force] [polarity]")
        val force = parts.getOrNull(3)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.70f
        val positive = when (parts.getOrNull(4)?.lowercase()) {
            "-", "negative", "neg" -> false
            else -> true
        }
        return TeacherCommand.Observe(parts[1], parts[2], force, positive)
    }

    private fun parsePressure(parts: List<String>, positive: Boolean): TeacherCommand {
        if (parts.size < 3) {
            val name = if (positive) "/reinforce" else "/contradict"
            return TeacherCommand.Invalid("usage: $name <axis> <value> [force]")
        }
        val force = parts.getOrNull(3)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.10f
        return if (positive) {
            TeacherCommand.Reinforce(parts[1], parts[2], force)
        } else {
            TeacherCommand.Contradict(parts[1], parts[2], force)
        }
    }
}
