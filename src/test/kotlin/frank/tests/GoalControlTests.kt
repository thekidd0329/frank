package frank.tests

import frank.autonomy.AutonomyMode
import frank.autonomy.ConsequenceReversibility
import frank.autonomy.ConsequenceRisk
import frank.autonomy.RiskDimension
import frank.goal.Goal
import frank.goal.GoalControlLoop
import frank.goal.GoalEvaluator
import frank.goal.GoalStatus
import frank.goal.ProposedAction
import frank.goal.WorldState

private var goalPassed = 0
private var goalFailed = 0

private fun goalTest(name: String, block: () -> Unit) {
    try {
        block()
        goalPassed++
        println("PASS  $name")
    } catch (t: Throwable) {
        goalFailed++
        println("FAIL  $name -> ${t.message}")
    }
}

fun main() {
    goalTest("goal evaluator measures achieved outcome instead of action count") {
        val goal = Goal(
            intent = "Get Christian home",
            desiredState = "Christian is home",
            successCriteria = setOf("route.selected", "arrival.verified")
        )
        val assessment = GoalEvaluator().assess(
            goal,
            before = WorldState(satisfiedCriteria = setOf("route.selected")),
            after = WorldState(satisfiedCriteria = setOf("route.selected", "arrival.verified"))
        )
        check(assessment.status == GoalStatus.SATISFIED)
        check(assessment.progress == 1f)
        check(assessment.newlySatisfied == setOf("arrival.verified"))
    }

    goalTest("material reversible consequence warns but does not force a question") {
        val goal = Goal(
            intent = "Handle the session cleanup",
            desiredState = "session cleaned",
            successCriteria = setOf("session.cleaned")
        )
        val action = ProposedAction(
            capabilityId = "session.cleanup",
            summary = "Clear the stale session",
            expectedEffects = setOf("session.cleaned"),
            certaintyDimensions = listOf(RiskDimension("target", 0.99f), RiskDimension("intent", 0.99f)),
            consequences = listOf(
                ConsequenceRisk(
                    name = "relogin",
                    probability = 0.9f,
                    severity = 0.3f,
                    reversibility = ConsequenceReversibility.REVERSIBLE,
                    description = "This may require signing in again."
                )
            )
        )
        val decision = GoalControlLoop().decide(goal, null, WorldState(availableCapabilities = setOf("session.cleanup")), listOf(action))
        check(decision.chosen != null)
        check(decision.chosen!!.mode == AutonomyMode.WARN_AND_ACT)
        check(decision.chosen!!.autonomy.warnings.single() == "This may require signing in again.")
    }

    goalTest("severe irreversible consequence asks instead of silently executing") {
        val goal = Goal(
            intent = "Fix storage",
            desiredState = "storage healthy",
            successCriteria = setOf("storage.healthy")
        )
        val action = ProposedAction(
            capabilityId = "storage.erase",
            summary = "Erase data",
            expectedEffects = setOf("storage.healthy"),
            certaintyDimensions = listOf(RiskDimension("target", 0.99f), RiskDimension("intent", 0.99f)),
            consequences = listOf(
                ConsequenceRisk(
                    name = "data_loss",
                    probability = 0.8f,
                    severity = 1f,
                    reversibility = ConsequenceReversibility.IRREVERSIBLE,
                    description = "Local data would be permanently lost."
                )
            )
        )
        val decision = GoalControlLoop().decide(goal, null, WorldState(availableCapabilities = setOf("storage.erase")), listOf(action))
        check(decision.chosen == null)
        check(decision.rejected.single().mode == AutonomyMode.ASK)
    }

    goalTest("control loop prefers action that advances more success criteria") {
        val goal = Goal(
            intent = "Get home",
            desiredState = "arrived home",
            successCriteria = setOf("route.selected", "navigation.started", "arrival.verified")
        )
        val weak = ProposedAction(
            capabilityId = "maps.inspect",
            summary = "Inspect route",
            expectedEffects = setOf("route.selected"),
            certaintyDimensions = listOf(RiskDimension("route", 0.99f))
        )
        val strong = ProposedAction(
            capabilityId = "maps.navigate",
            summary = "Start navigation",
            expectedEffects = setOf("route.selected", "navigation.started"),
            certaintyDimensions = listOf(RiskDimension("route", 0.99f))
        )
        val decision = GoalControlLoop().decide(
            goal,
            previousWorld = null,
            currentWorld = WorldState(availableCapabilities = setOf("maps.inspect", "maps.navigate")),
            candidates = listOf(weak, strong)
        )
        check(decision.chosen?.action?.capabilityId == "maps.navigate")
    }

    println("\nGOAL RESULT  $goalPassed passed, $goalFailed failed")
    check(goalFailed == 0) { "$goalFailed goal-control test(s) failed" }
}
