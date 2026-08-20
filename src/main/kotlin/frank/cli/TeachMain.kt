package frank.cli

import frank.cognition.CommitmentFlags
import frank.cognition.EvidenceToCommitment
import frank.cognition.IncomingEvidence
import frank.cognition.Locus
import frank.cognition.LocusAddressing
import frank.cognition.Polarity
import frank.cognition.ReasoningEngine
import frank.cognition.ResidualCommitment
import frank.cognition.TemporalAnchor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object TeachMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val fieldPath = parseFieldPath(args)
        val registry = LabelRegistry(fieldPath.resolveSibling(fieldPath.fileName.toString() + ".labels"))
        val journal = TeacherJournal(fieldPath.resolveSibling(fieldPath.fileName.toString() + ".journal"))
        val store = FieldStore(fieldPath)
        val engine = ReasoningEngine()
        val loaded = store.load()
        engine.restoreFrom(loaded)

        println("Frank newborn teacher")
        println("field: $fieldPath")
        println(if (loaded.isEmpty()) "Frank field: empty" else "Frank field: ${loaded.size} commitments restored")
        println("projections rebuilt from field: beliefs=${engine.currentBeliefs(0.05f).size} goals=${engine.currentGoals().size}")
        println("Type /help for teacher commands. Plain text is journaled as evidence only; it does not alter the field.\n")

        while (true) {
            print("you> ")
            val line = readLine() ?: break
            when (val command = TeacherParser.parse(line)) {
                is TeacherCommand.Observe -> {
                    val locus = locusFor(command.axis, command.value)
                    registry.remember(locus, command.axis, command.value)
                    val commitment = EvidenceToCommitment().convert(
                        IncomingEvidence(
                            axisId = stableId(command.axis),
                            valueId = stableId(command.value),
                            polarityPositive = command.positive,
                            confidence = command.force,
                            recency = System.currentTimeMillis(),
                            isExplicit = true
                        )
                    )
                    engine.absorb(commitment)
                    store.save(engine.commitmentSnapshot())
                    println("absorbed ${registry.label(locus.raw)} force=${signed(commitment)}")
                    maybeAsk(engine, registry)
                }
                is TeacherCommand.Say -> {
                    if (command.text.isNotBlank()) {
                        journal.append(command.text)
                        println("journaled USERASSERTED utterance; field unchanged")
                    }
                }
                is TeacherCommand.Reinforce -> {
                    val locus = locusFor(command.axis, command.value)
                    registry.remember(locus, command.axis, command.value)
                    val before = engine.commitmentSnapshot().firstOrNull { it.locus == locus }
                    if (before == null) {
                        println("no existing locus; use /observe first")
                    } else {
                        engine.reinforce(locus, command.force)
                        store.save(engine.commitmentSnapshot())
                        val after = engine.commitmentSnapshot().first { it.locus == locus }
                        println("reinforced ${registry.label(locus.raw)} ${fmt(before.residualForce)} -> ${fmt(after.residualForce)}")
                        maybeAsk(engine, registry)
                    }
                }
                is TeacherCommand.Contradict -> {
                    val locus = locusFor(command.axis, command.value)
                    registry.remember(locus, command.axis, command.value)
                    val existing = engine.commitmentSnapshot().firstOrNull { it.locus == locus }
                    if (existing == null) {
                        println("no existing locus; contradiction requires an existing commitment")
                    } else {
                        val opposite = when (existing.polarity) {
                            Polarity.POSITIVE -> false
                            Polarity.NEGATIVE -> true
                            Polarity.NEUTRAL -> false
                        }
                        val incoming = EvidenceToCommitment().convert(
                            IncomingEvidence(
                                axisId = stableId(command.axis),
                                valueId = stableId(command.value),
                                polarityPositive = opposite,
                                confidence = command.force,
                                recency = System.currentTimeMillis(),
                                isExplicit = true
                            )
                        )
                        engine.absorb(incoming)
                        store.save(engine.commitmentSnapshot())
                        val after = engine.commitmentSnapshot().first { it.locus == locus }
                        println("opposing pressure ${registry.label(locus.raw)} -> ${after.polarity} ${fmt(after.residualForce)}")
                        maybeAsk(engine, registry)
                    }
                }
                is TeacherCommand.Tick -> {
                    repeat(command.count) { engine.tickDecay() }
                    store.save(engine.commitmentSnapshot())
                    println("decay applied ${command.count} time(s)")
                    maybeAsk(engine, registry)
                }
                TeacherCommand.Field -> print(FieldPrinter.raw(engine.commitmentSnapshot(), registry::label))
                TeacherCommand.Project -> print(FieldPrinter.projections(engine, registry::label))
                TeacherCommand.Gaps -> print(FieldPrinter.gapText(engine.commitmentSnapshot(), registry::label))
                TeacherCommand.Ask -> maybeAsk(engine, registry, explicit = true)
                TeacherCommand.Restore -> {
                    val before = normalizedActivation(engine)
                    val snapshot = engine.commitmentSnapshot()
                    engine.restoreFrom(snapshot)
                    val after = normalizedActivation(engine)
                    check(before == after) { "RECONSTRUCTION INVARIANT FAILED: activation set changed after projection rebuild" }
                    println("restore PASS: projections rebuilt from Residual Commitment Field with identical activation set")
                }
                TeacherCommand.Help -> printHelp()
                TeacherCommand.Quit -> {
                    store.save(engine.commitmentSnapshot())
                    println("field saved")
                    return
                }
                is TeacherCommand.Invalid -> println(command.reason)
            }
        }
        store.save(engine.commitmentSnapshot())
    }

    private fun maybeAsk(engine: ReasoningEngine, registry: LabelRegistry, explicit: Boolean = false) {
        val gap = FieldPrinter.gaps(engine.commitmentSnapshot(), registry::label).firstOrNull()
        if (gap == null) {
            if (explicit) println("no unresolved field target strong enough to ask about")
            return
        }
        val label = registry.label(gap.locus.raw) ?: "locus ${gap.locus.raw}"
        val contested = gap.flags.has(CommitmentFlags.CONTESTED)
        println("QUESTION_TARGET locus=${gap.locus.raw} label=$label")
        println(if (contested) "Frank: What would resolve the conflict around $label?" else "Frank: What else should I know about $label?")
    }

    private fun normalizedActivation(engine: ReasoningEngine): List<String> =
        engine.neuralContext(0.05f, Int.MAX_VALUE)
            .sortedBy { it.locus.raw }
            .map { "${it.locus.raw}:${it.polarity}:${fmt(it.residualForce)}:${fmt(it.contextualBinding)}" }

    private fun parseFieldPath(args: Array<String>): Path {
        val idx = args.indexOf("--field")
        if (idx >= 0 && idx + 1 < args.size) return Paths.get(args[idx + 1]).toAbsolutePath()
        val home = System.getenv("HOME") ?: "."
        return Paths.get(home, ".frank", "newborn.cog").toAbsolutePath()
    }

    private fun locusFor(axis: String, value: String): Locus = LocusAddressing.fromAxisValue(stableId(axis), stableId(value))

    private fun stableId(text: String): Int = normalizeToken(text).hashCode()

    private fun normalizeToken(text: String): String = text.trim().lowercase().replace(Regex("[\\s-]+"), "_")

    private fun signed(c: ResidualCommitment): String = when (c.polarity) {
        Polarity.POSITIVE -> "+${fmt(c.residualForce)}"
        Polarity.NEGATIVE -> "-${fmt(c.residualForce)}"
        Polarity.NEUTRAL -> "0.000"
    }

    private fun fmt(value: Float): String = "%.3f".format(value)

    private fun printHelp() {
        println("""
/observe <axis> <value> [force] [polarity]   explicit teacher evidence -> field
/say <free text>                              journal only; never changes field
/reinforce <axis> <value> [force]            strengthen existing residual
/contradict <axis> <value> [force]           opposing pressure on same locus
/tick [n]                                     apply decay n times
/field                                        raw Residual Commitment Field
/project                                      rebuild/show projections
/gaps                                         weak or contested loci
/ask                                          ask from the field-selected gap
/restore                                      verify reconstruction invariant
/quit                                         persist field and exit
        """.trimIndent())
    }
}

private class LabelRegistry(private val path: Path) {
    private val labels = linkedMapOf<Long, String>()

    init {
        if (Files.exists(path)) {
            Files.readAllLines(path).forEach { line ->
                val parts = line.split('\t', limit = 2)
                if (parts.size == 2) parts[0].toLongOrNull()?.let { labels[it] = parts[1] }
            }
        }
    }

    fun remember(locus: Locus, axis: String, value: String) {
        labels[locus.raw] = "${axis.trim()}=${value.trim()}"
        Files.createDirectories(path.parent)
        Files.write(path, labels.entries.sortedBy { it.key }.map { "${it.key}\t${it.value}" })
    }

    fun label(raw: Long): String? = labels[raw]
}

private class TeacherJournal(private val path: Path) {
    fun append(text: String) {
        Files.createDirectories(path.parent)
        File(path.toUri()).appendText("${System.currentTimeMillis()}\tUSERASSERTED\t${text.replace("\n", " ")}\n")
    }
}

private class FieldStore(private val path: Path) {
    fun load(): List<ResidualCommitment> {
        if (!Files.exists(path)) return emptyList()
        return Files.readAllLines(path)
            .asSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull(::decode)
            .toList()
    }

    fun save(snapshot: List<ResidualCommitment>) {
        Files.createDirectories(path.parent)
        val lines = mutableListOf("# Frank Residual Commitment Field v1")
        snapshot.sortedBy { it.locus.raw }.forEach { c ->
            lines += listOf(
                c.locus.raw,
                c.polarity.name,
                c.residualForce,
                c.contextualBinding,
                c.temporalPersistence.generation,
                c.temporalPersistence.recencyDelta,
                c.consolidationMaturity,
                c.flags.bits
            ).joinToString("\t")
        }
        Files.write(path, lines)
    }

    private fun decode(line: String): ResidualCommitment? {
        val p = line.split('\t')
        if (p.size < 8) return null
        return runCatching {
            ResidualCommitment(
                locus = Locus(p[0].toLong()),
                polarity = Polarity.valueOf(p[1]),
                residualForce = p[2].toFloat(),
                contextualBinding = p[3].toFloat(),
                temporalPersistence = TemporalAnchor(p[4].toLong(), p[5].toInt()),
                consolidationMaturity = p[6].toFloat(),
                flags = CommitmentFlags(p[7].toInt())
            )
        }.getOrNull()
    }
}
