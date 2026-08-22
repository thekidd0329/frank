package frank.teacher

import frank.cognition.MemoryDynamicsProfile
import java.nio.file.Path

fun main(args: Array<String>) {
    var journalPath = TeachingJournal.defaultPath()
    var profile = MemoryDynamicsProfile.CONTROL

    var index = 0
    while (index < args.size) {
        when (args[index]) {
            "--state" -> {
                require(index + 1 < args.size) { "--state requires a path" }
                journalPath = Path.of(args[index + 1])
                index += 2
            }
            "--profile" -> {
                require(index + 1 < args.size) { "--profile requires control or phi" }
                profile = when (args[index + 1].lowercase()) {
                    "control" -> MemoryDynamicsProfile.CONTROL
                    "phi" -> MemoryDynamicsProfile.PHI
                    else -> error("profile must be control or phi")
                }
                index += 2
            }
            else -> error("usage: TeachMainKt [--state /path/to/teaching.log] [--profile control|phi]")
        }
    }

    val session = TeacherSession(TeachingJournal(journalPath), profile)
    println("Frank newborn teaching terminal")
    println("profile=${if (profile == MemoryDynamicsProfile.PHI) "phi" else "control"}")
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
                line == "/associations" -> printAssociations(session)
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
                line.startsWith("/pair ") -> {
                    val payload = line.substringAfter(' ').trim()
                    val parts = payload.split("::", limit = 2).map { it.trim() }
                    require(parts.size == 2 && parts.all { it.isNotEmpty() }) {
                        "usage: /pair <experience A> :: <experience B>"
                    }
                    session.associate(parts[0], parts[1])
                    println("associated ${session.locusOf(parts[0])} <-> ${session.locusOf(parts[1])}")
                }
                line.startsWith("/recall ") -> {
                    val cue = line.substringAfter(' ').trim()
                    val recalls = session.recall(cue)
                    if (recalls.isEmpty()) println("no retrievable association")
                    else recalls.forEach { println("locus=${it.locus}\tstrength=${it.strength}") }
                }
                line.startsWith("/age ") -> {
                    val steps = line.substringAfter(' ').trim().toInt()
                    session.ageMemory(steps)
                    println("memory aged steps=$steps")
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

private fun printAssociations(session: TeacherSession) {
    val field = session.associationField()
    if (field.isEmpty()) {
        println("association field is empty")
        return
    }
    field.entries.sortedWith(compareBy({ it.key.from }, { it.key.to })).forEach { (edge, strength) ->
        println("${edge.from}\t${edge.to}\t$strength")
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
          /pair A :: B              learn repeated co-occurrence between two opaque experiences
          /recall <cue>             recall currently retrievable associated loci
          /age <steps>              apply passive memory aging
          /recover <0..1>           apply a recovery/sleep-like newborn step
          /state                    inspect reconstructed newborn state
          /field                    inspect the opaque residual field
          /associations             inspect learned opaque associations
          /history                  show persisted teaching-event count
          /help                     show this help
          /quit                     exit; the journal survives restart

        launch with --profile phi to run the golden-ratio-derived memory experiment.
        """.trimIndent()
    )
}
