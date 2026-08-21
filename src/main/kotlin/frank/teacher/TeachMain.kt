package frank.teacher

import java.nio.file.Path

fun main(args: Array<String>) {
    val journalPath = when {
        args.isEmpty() -> TeachingJournal.defaultPath()
        args.size == 2 && args[0] == "--state" -> Path.of(args[1])
        else -> error("usage: TeachMainKt [--state /path/to/teaching.log]")
    }

    val session = TeacherSession(TeachingJournal(journalPath))
    println("Frank newborn teaching terminal")
    println("reconstructed ${session.eventCount()} persisted teaching events")
    printHelp()

    while (true) {
        print("frank-teach> ")
        val line = readLine()?.trim() ?: break
        if (line.isEmpty()) continue

        try {
            when {
                line == "/help" -> printHelp()
                line == "/quit" || line == "/exit" -> return
                line == "/state" -> println(session.state())
                line == "/history" -> println("events=${session.eventCount()}")
                line == "/field" -> printField(session)
                line.startsWith("/observe ") -> {
                    val observation = line.substringAfter(' ').trim()
                    session.stage(observation)
                    println("staged; choose /commit, /reinforce, or /contradict")
                }
                line == "/commit" -> {
                    val locus = session.commit()
                    println("committed locus=$locus")
                }
                line == "/reinforce" || line.startsWith("/reinforce ") -> {
                    val strength = optionalStrength(line, "/reinforce")
                    val locus = session.reinforce(strength)
                    println("reinforced locus=$locus strength=$strength")
                }
                line == "/contradict" || line.startsWith("/contradict ") -> {
                    val strength = optionalStrength(line, "/contradict")
                    val locus = session.contradict(strength)
                    println("contradicted locus=$locus strength=$strength")
                }
                line.startsWith("/recover ") -> {
                    val amount = line.substringAfter(' ').trim().toFloat()
                    session.recover(amount)
                    println("recovery recorded amount=$amount")
                }
                line.startsWith("/") -> println("unknown command; use /help")
                else -> {
                    session.stage(line)
                    println("staged; choose /commit, /reinforce, or /contradict")
                }
            }
        } catch (t: Throwable) {
            println("error: ${t.message}")
        }
    }
}

private fun optionalStrength(line: String, command: String): Float {
    val remainder = line.removePrefix(command).trim()
    return if (remainder.isEmpty()) 1f else remainder.toFloat()
}

private fun printField(session: TeacherSession) {
    val field = session.residualField()
    if (field.isEmpty()) {
        println("field is empty")
        return
    }
    field.toSortedMap().forEach { (locus, force) ->
        println("$locus\t$force")
    }
}

private fun printHelp() {
    println(
        """
        commands:
          /observe <raw text>       stage an experience without learning it yet
          /commit                   commit the staged experience neutrally
          /reinforce [0..1]         add positive evidence to the staged experience
          /contradict [0..1]        add negative evidence to the staged experience
          /recover <0..1>           apply a recovery/sleep-like newborn step
          /state                    inspect reconstructed newborn state
          /field                    inspect the opaque residual field
          /history                  show persisted teaching-event count
          /help                     show this help
          /quit                     exit; the journal survives restart
        """.trimIndent()
    )
}
