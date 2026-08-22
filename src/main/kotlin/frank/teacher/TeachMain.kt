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
    println("sleep=${session.sleepStatus().state} pressure=${"%.3f".format(session.sleepStatus().sleepPressure)}")
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
                line == "/sleep" -> println(session.sleepStatus())
                line == "/history" -> println("events=${session.eventCount()}")
                line == "/field" -> printField(session)
                line == "/brain" -> printBrain(session)
                line == "/gaps" -> printGaps(session)
                line == "/ask" -> printQuestionIntent(session)
                line.startsWith("/observe ") -> {
                    val observation = line.substringAfter(' ').trim()
                    session.stage(observation)
                    println("staged; choose /commit, /reinforce, or /contradict")
                }
                line == "/commit" -> {
                    val locus = session.commit()
                    println("committed locus=$locus sleep=${session.sleepStatus().state}")
                }
                line == "/reinforce" || line.startsWith("/reinforce ") -> {
                    val strength = optionalStrength(line, "/reinforce")
                    val locus = session.reinforce(strength)
                    println("reinforced locus=$locus strength=$strength sleep=${session.sleepStatus().state}")
                }
                line == "/contradict" || line.startsWith("/contradict ") -> {
                    val strength = optionalStrength(line, "/contradict")
                    val locus = session.contradict(strength)
                    println("contradicted locus=$locus strength=$strength sleep=${session.sleepStatus().state}")
                }
                line.startsWith("/recover ") -> {
                    val amount = line.substringAfter(' ').trim().toFloat()
                    session.recover(amount)
                    val sleep = session.sleepStatus()
                    println("offline recovery advanced amount=$amount state=${sleep.state} pressure=${"%.3f".format(sleep.sleepPressure)} wantsWake=${sleep.wantsWake}")
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
        println("legacy newborn field is empty")
        return
    }
    field.toSortedMap().forEach { (locus, force) ->
        println("$locus\t$force")
    }
}

private fun printBrain(session: TeacherSession) {
    val snapshot = session.brainSnapshot()
    println("commitments=${snapshot.size} sleep=${session.sleepStatus().state}")
    snapshot
        .sortedByDescending { it.residualForce }
        .forEach { commitment ->
            println(
                "locus=${commitment.locus.raw} polarity=${commitment.polarity}" +
                    " force=${"%.3f".format(commitment.residualForce)}" +
                    " maturity=${"%.3f".format(commitment.consolidationMaturity)}" +
                    " flags=${commitment.flags.bits}"
            )
        }
}

private fun printGaps(session: TeacherSession) {
    val gaps = session.learningGaps()
    if (gaps.isEmpty()) {
        println("no unresolved observed loci")
        return
    }

    gaps.forEachIndexed { index, intent ->
        val gap = intent.gap
        println(
            "${index + 1}. locus=${gap.locus.raw}" +
                " pressure=${"%.3f".format(gap.pressure)}" +
                " force=${"%.3f".format(gap.residualForce)}" +
                " maturity=${"%.3f".format(gap.consolidationMaturity)}" +
                " contested=${gap.contested}" +
                (intent.sourcePreview?.let { " source=\"$it\"" } ?: "")
        )
    }
}

private fun printQuestionIntent(session: TeacherSession) {
    val intent = session.nextQuestionIntent()
    if (intent == null) {
        if (session.isAsleep()) println("Frank is asleep; no waking question is emitted")
        else println("no unresolved observed locus selected")
        return
    }

    val gap = intent.gap
    println(
        "frank-question-intent locus=${gap.locus.raw}" +
            " pressure=${"%.3f".format(gap.pressure)}" +
            " force=${"%.3f".format(gap.residualForce)}" +
            " maturity=${"%.3f".format(gap.consolidationMaturity)}" +
            " contested=${gap.contested}"
    )
    intent.sourcePreview?.let { println("teacher-preview: $it") }
}

private fun printHelp() {
    println(
        """
        commands:
          /observe <raw text>       stage an experience without learning it yet
          /commit                   commit the staged experience neutrally
          /reinforce [0..1]         add positive evidence to the staged experience
          /contradict [0..1]        add negative evidence to the staged experience
          /recover <0..1>           advance offline recovery; Frank decides when wake is available
          /state                    inspect reconstructed newborn state
          /sleep                    inspect endogenous sleep/homeostatic state
          /field                    inspect the legacy newborn scalar field
          /brain                    inspect the actual Residual Commitment Field
          /gaps                     inspect Frank's ranked unresolved observed loci
          /ask                      let Frank select his strongest current question intent
          /history                  show persisted teaching-event count
          /help                     show this help
          /quit                     exit; the journal survives restart
        """.trimIndent()
    )
}
