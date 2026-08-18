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

data class AutonomyDecision(
    val jointConfidence: Float,
    val exhaustiveness: Float,
    val finalConfidence: Float,
    val autoExecute: Boolean,
    val weakestRelevantDimension: RiskDimension?
)

class AutonomyEvaluator(
    private val threshold: Float = 0.90f
) {
    fun evaluate(
        dimensions: List<RiskDimension>,
        channels: List<SourceChannel> = emptyList()
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
        return AutonomyDecision(
            jointConfidence = joint,
            exhaustiveness = exhaustiveness,
            finalConfidence = final,
            autoExecute = final >= threshold,
            weakestRelevantDimension = relevant.minByOrNull { it.probability }
        )
    }
}
