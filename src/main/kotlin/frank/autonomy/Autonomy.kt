package frank.autonomy

data class RiskDimension(
    val name: String,
    val probability: Float,
    val decisionRelevant: Boolean = true
)

data class SourceChannel(
    val id: String,
    val highProbability: Boolean = true,
    val checked: Boolean = false
)

enum class ConsequenceReversibility { REVERSIBLE, PARTIAL, IRREVERSIBLE }

data class ConsequenceRisk(
    val name: String,
    val probability: Float,
    val severity: Float,
    val reversibility: ConsequenceReversibility = ConsequenceReversibility.REVERSIBLE,
    val decisionRelevant: Boolean = true,
    val description: String = name
) {
    val exposure: Float
        get() = probability.coerceIn(0f, 1f) * severity.coerceIn(0f, 1f)
}

enum class AutonomyMode {
    ACT,
    WARN_AND_ACT,
    ASK
}

data class AutonomyDecision(
    val jointConfidence: Float,
    val exhaustiveness: Float,
    val finalConfidence: Float,
    val autoExecute: Boolean,
    val weakestRelevantDimension: RiskDimension?,
    val mode: AutonomyMode = if (autoExecute) AutonomyMode.ACT else AutonomyMode.ASK,
    val consequenceExposure: Float = 0f,
    val warnings: List<String> = emptyList()
)

class AutonomyEvaluator(
    private val threshold: Float = 0.90f,
    private val warnExposure: Float = 0.20f,
    private val irreversibleSeverity: Float = 0.80f,
    private val irreversibleProbability: Float = 0.35f
) {
    fun evaluate(
        dimensions: List<RiskDimension>,
        channels: List<SourceChannel> = emptyList(),
        consequences: List<ConsequenceRisk> = emptyList()
    ): AutonomyDecision {
        val relevant = dimensions.filter { it.decisionRelevant }
        val joint = relevant.fold(1.0) { acc, d ->
            acc * d.probability.coerceIn(0f, 1f).toDouble()
        }.toFloat()

        val highProbability = channels.filter { it.highProbability }
        val exhaustiveness = if (highProbability.isEmpty()) {
            1f
        } else {
            highProbability.count { it.checked }.toFloat() / highProbability.size.toFloat()
        }

        val final = (joint * exhaustiveness).coerceIn(0f, 1f)
        val relevantConsequences = consequences.filter { it.decisionRelevant }
        val exposure = relevantConsequences.maxOfOrNull { it.exposure } ?: 0f
        val hardConsequence = relevantConsequences.any {
            it.reversibility == ConsequenceReversibility.IRREVERSIBLE &&
                it.severity.coerceIn(0f, 1f) >= irreversibleSeverity &&
                it.probability.coerceIn(0f, 1f) >= irreversibleProbability
        }

        val mode = when {
            final < threshold -> AutonomyMode.ASK
            hardConsequence -> AutonomyMode.ASK
            exposure >= warnExposure -> AutonomyMode.WARN_AND_ACT
            else -> AutonomyMode.ACT
        }

        return AutonomyDecision(
            jointConfidence = joint,
            exhaustiveness = exhaustiveness,
            finalConfidence = final,
            autoExecute = mode != AutonomyMode.ASK,
            weakestRelevantDimension = relevant.minByOrNull { it.probability },
            mode = mode,
            consequenceExposure = exposure,
            warnings = if (mode == AutonomyMode.ACT) emptyList() else relevantConsequences
                .sortedByDescending { it.exposure }
                .map { it.description }
        )
    }
}
