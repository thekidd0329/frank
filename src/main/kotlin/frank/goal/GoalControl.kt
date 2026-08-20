package frank.goal

import frank.autonomy.AutonomyDecision
import frank.autonomy.AutonomyEvaluator
import frank.autonomy.AutonomyMode
import frank.autonomy.ConsequenceRisk
import frank.autonomy.RiskDimension
import frank.autonomy.SourceChannel
import java.util.UUID

private fun goalId(): String = UUID.randomUUID().toString()

data class Goal(
    val id: String = goalId(),
    val intent: String,
    val desiredState: String,
    val successCriteria: Set<String>,
    val constraints: Set<String> = emptySet(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class WorldState(
    val observedAtMillis: Long = System.currentTimeMillis(),
    val facts: Map<String, String> = emptyMap(),
    val satisfiedCriteria: Set<String> = emptySet(),
    val unresolvedUncertainty: Set<String> = emptySet(),
    val availableCapabilities: Set<String> = emptySet()
)

enum class GoalStatus { IN_PROGRESS, SATISFIED, BLOCKED, REGRESSED }

data class GoalAssessment(
    val status: GoalStatus,
    val progress: Float,
    val newlySatisfied: Set<String>,
    val regressedCriteria: Set<String>,
    val unresolvedCriteria: Set<String>
)

class GoalEvaluator {
    fun assess(goal: Goal, before: WorldState?, after: WorldState): GoalAssessment {
        val required = goal.successCriteria
        if (required.isEmpty()) {
            return GoalAssessment(GoalStatus.SATISFIED, 1f, emptySet(), emptySet(), emptySet())
        }

        val beforeSatisfied = before?.satisfiedCriteria.orEmpty().intersect(required)
        val afterSatisfied = after.satisfiedCriteria.intersect(required)
        val newlySatisfied = afterSatisfied - beforeSatisfied
        val regressed = beforeSatisfied - afterSatisfied
        val unresolved = required - afterSatisfied
        val progress = afterSatisfied.size.toFloat() / required.size.toFloat()

        val status = when {
            unresolved.isEmpty() -> GoalStatus.SATISFIED
            regressed.isNotEmpty() && newlySatisfied.isEmpty() -> GoalStatus.REGRESSED
            after.availableCapabilities.isEmpty() && after.unresolvedUncertainty.isNotEmpty() -> GoalStatus.BLOCKED
            else -> GoalStatus.IN_PROGRESS
        }

        return GoalAssessment(status, progress, newlySatisfied, regressed, unresolved)
    }
}

data class ProposedAction(
    val id: String = goalId(),
    val capabilityId: String,
    val summary: String,
    val expectedEffects: Set<String> = emptySet(),
    val certaintyDimensions: List<RiskDimension> = emptyList(),
    val sourceChannels: List<SourceChannel> = emptyList(),
    val consequences: List<ConsequenceRisk> = emptyList()
)

data class ActionJudgment(
    val action: ProposedAction,
    val autonomy: AutonomyDecision,
    val explanation: String
) {
    val mode: AutonomyMode get() = autonomy.mode
    val mayExecuteWithoutQuestion: Boolean get() = autonomy.autoExecute
}

class JudgmentEngine(
    private val autonomyEvaluator: AutonomyEvaluator = AutonomyEvaluator()
) {
    fun judge(action: ProposedAction): ActionJudgment {
        val decision = autonomyEvaluator.evaluate(
            dimensions = action.certaintyDimensions,
            channels = action.sourceChannels,
            consequences = action.consequences
        )
        val explanation = when (decision.mode) {
            AutonomyMode.ACT -> "Enough is known and no material consequence requires interruption."
            AutonomyMode.WARN_AND_ACT -> "The action can proceed, but material consequences should be surfaced."
            AutonomyMode.ASK -> "Uncertainty or irreversible consequence is high enough to require a question."
        }
        return ActionJudgment(action, decision, explanation)
    }
}

data class GoalDecision(
    val goal: Goal,
    val world: WorldState,
    val assessment: GoalAssessment,
    val chosen: ActionJudgment?,
    val rejected: List<ActionJudgment>
)

class GoalControlLoop(
    private val goalEvaluator: GoalEvaluator = GoalEvaluator(),
    private val judgmentEngine: JudgmentEngine = JudgmentEngine()
) {
    fun decide(
        goal: Goal,
        previousWorld: WorldState?,
        currentWorld: WorldState,
        candidates: List<ProposedAction>
    ): GoalDecision {
        val assessment = goalEvaluator.assess(goal, previousWorld, currentWorld)
        if (assessment.status == GoalStatus.SATISFIED) {
            return GoalDecision(goal, currentWorld, assessment, chosen = null, rejected = emptyList())
        }

        val judged = candidates.map(judgmentEngine::judge)
        val executable = judged.filter { it.mayExecuteWithoutQuestion }
        val chosen = executable.maxWithOrNull(
            compareBy<ActionJudgment> { actionGoalCoverage(goal, it.action) }
                .thenBy { it.autonomy.finalConfidence }
                .thenBy { -it.autonomy.consequenceExposure }
        )

        return GoalDecision(
            goal = goal,
            world = currentWorld,
            assessment = assessment,
            chosen = chosen,
            rejected = judged.filterNot { it === chosen }
        )
    }

    private fun actionGoalCoverage(goal: Goal, action: ProposedAction): Int =
        action.expectedEffects.count(goal.successCriteria::contains)
}
