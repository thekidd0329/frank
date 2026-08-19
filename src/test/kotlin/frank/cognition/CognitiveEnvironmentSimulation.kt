package frank.cognition

import frank.memory.AdaptiveOntology
import frank.memory.CombingEngine
import frank.memory.MemoryObservation
import frank.memory.MutableCompactMemory
import frank.memory.ObservationSource
import java.util.UUID
import kotlin.math.abs

/**
 * 180-second LOGICAL environment simulation for Frank's cognition.
 *
 * This intentionally does not sleep for three real minutes. The logical clock
 * advances through 0..180s so CI can deterministically replay the exact same
 * environment and inspect how memory changes under reinforcement, silence,
 * contradiction, corrections, and reconstruction.
 *
 * The production system currently promotes some passive memories after three
 * observations. This simulation also models the proposed stricter future
 * consolidation agent: three DISTINCT evaluation passes must agree on the
 * same value + polarity + scope before a candidate is eligible for durable
 * memory. Repeated samples inside one pass do not count three times, and a
 * contradiction resets the streak.
 */
object CognitiveEnvironmentSimulation {
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== Frank cognitive environment: 180 logical seconds ===")
        newbornAndThreePassStability()
        contradictionResetsConsolidation()
        draftsNeverBecomeEvidence()
        degradationAndResidualPressure()
        predictCompareRevise()
        reconstructionAfterProjectionLoss()
        println("CognitiveEnvironmentSimulation: PASS")
    }

    private fun newbornAndThreePassStability() {
        println("\n[0s..130s] newborn -> tentative distinction -> consolidation")
        val person = UUID.randomUUID()
        val observation = MemoryObservation(
            personId = person,
            axis = "sensory_preference",
            value = "low_light",
            confidence = 0.93f,
            recency = 1.0f,
            polarity = 1,
            scopeId = 1,
            source = ObservationSource.PASSIVE_FILE,
            timestampMillis = 10_000L
        )

        // Newborn semantics: before a promoted observation, durable memory is empty.
        val durableMemory = MutableCompactMemory()
        val engine = CombingEngine(durableMemory, AdaptiveOntology())
        check(durableMemory.allClaims().isEmpty())
        event(10, "environment repeatedly suggests low light is preferred")
        check(engine.observe(observation.copy(timestampMillis = 10_000L)) == null)
        check(durableMemory.allClaims().isEmpty())
        event(70, "same preference appears again after other activity")
        check(engine.observe(observation.copy(timestampMillis = 70_000L)) == null)
        check(durableMemory.allClaims().isEmpty())
        event(130, "third separated observation agrees")
        check(engine.observe(observation.copy(timestampMillis = 130_000L)) != null)
        check(durableMemory.allClaims().size == 1)

        // Proposed executive/consolidation agent: DISTINCT passes, not raw sample count.
        val gate = ThreePassStabilityGate()
        check(!gate.evaluate(passId = 1, observation))
        check(!gate.evaluate(passId = 1, observation.copy(timestampMillis = 11_000L)))
        check(!gate.evaluate(passId = 1, observation.copy(timestampMillis = 12_000L)))
        check(!gate.evaluate(passId = 2, observation.copy(timestampMillis = 70_000L)))
        check(gate.evaluate(passId = 3, observation.copy(timestampMillis = 130_000L)))

        // Surface the CURRENT implementation gap without making CI depend on it forever:
        // MemoryPromotionGate counts distinct timestamps, so three samples in one pass
        // can currently look like three confirmations. The future agent must sit above
        // that boundary or the production gate must become pass-aware.
        val legacyMemory = MutableCompactMemory()
        val legacyEngine = CombingEngine(legacyMemory, AdaptiveOntology())
        legacyEngine.observe(observation.copy(timestampMillis = 1L))
        legacyEngine.observe(observation.copy(timestampMillis = 2L))
        val samePassThird = legacyEngine.observe(observation.copy(timestampMillis = 3L))
        if (samePassThird != null) {
            println("KNOWN_GAP: current passive gate counts observations, not independent passes")
        } else {
            println("PASS-AWARE: production gate already rejects same-pass repetition")
        }
    }

    private fun contradictionResetsConsolidation() {
        println("\n[20s..160s] contradictory social/sensory information")
        val person = UUID.randomUUID()
        val gate = ThreePassStabilityGate()
        fun obs(polarity: Int, second: Int) = MemoryObservation(
            personId = person,
            axis = "crowd_preference",
            value = "busy_room",
            confidence = 0.92f,
            recency = 1.0f,
            polarity = polarity,
            scopeId = 2,
            source = ObservationSource.INBOUND_MESSAGE,
            timestampMillis = second * 1_000L
        )

        event(20, "signal suggests busy rooms are liked")
        check(!gate.evaluate(1, obs(+1, 20)))
        event(50, "second pass agrees")
        check(!gate.evaluate(2, obs(+1, 50)))
        event(80, "new evidence contradicts the earlier preference")
        check(!gate.evaluate(3, obs(-1, 80))) // contradiction resets to streak 1
        event(120, "next pass supports the correction")
        check(!gate.evaluate(4, obs(-1, 120)))
        event(160, "third stable pass supports the corrected polarity")
        check(gate.evaluate(5, obs(-1, 160)))

        // Low-confidence ambiguity must not count as a solid pass.
        val uncertain = obs(-1, 170).copy(confidence = 0.52f)
        check(!gate.evaluate(6, uncertain))
    }

    private fun draftsNeverBecomeEvidence() {
        println("\n[30s] source-authority check")
        val person = UUID.randomUUID()
        val memory = MutableCompactMemory()
        val engine = CombingEngine(memory, AdaptiveOntology())
        repeat(5) { index ->
            val draft = MemoryObservation(
                personId = person,
                axis = "relationship_claim",
                value = "trusted",
                confidence = 0.99f,
                recency = 1.0f,
                polarity = 1,
                scopeId = 0,
                source = ObservationSource.DRAFT_OR_CLIPPING,
                timestampMillis = (30_000L + index)
            )
            check(engine.observe(draft) == null)
        }
        check(memory.allClaims().isEmpty())
        event(30, "five repeated draft/clipping samples correctly carry zero promotion authority")
    }

    private fun degradationAndResidualPressure() {
        println("\n[40s..110s] reinforcement, silence, decay, contradiction")
        val locus = Locus.fromParts(40, 1)
        val field = CommitmentField()

        event(40, "tentative positive commitment arrives with force +0.40")
        field.absorb(commitment(locus, Polarity.POSITIVE, 0.40f, generation = 1))
        event(75, "silence: residual force decays toward neutral, never through it")
        field.decayAll(0.50f)
        var state = requireNotNull(field.get(locus))
        check(state.polarity == Polarity.POSITIVE)
        assertNear(state.residualForce, 0.20f)

        event(90, "intermittent reinforcement restores some force")
        field.absorb(commitment(locus, Polarity.POSITIVE, 0.40f, generation = 2))
        state = requireNotNull(field.get(locus))
        assertNear(state.residualForce, 0.40f) // 0.20 + half of 0.40

        // Grok pressure test: +0.40 followed by -0.90 must leave -0.50.
        event(110, "strong contradictory evidence applies -0.90 pressure")
        field.absorb(commitment(locus, Polarity.NEGATIVE, 0.90f, generation = 3))
        state = requireNotNull(field.get(locus))
        check(state.polarity == Polarity.NEGATIVE)
        assertNear(state.residualForce, 0.50f)
        check(state.flags.has(CommitmentFlags.CONTESTED))
    }

    private fun predictCompareRevise() {
        println("\n[55s..100s] raw observation -> commit -> predict -> compare -> revise")
        val engine = ReasoningEngine()
        val locus = Locus.fromParts(55, 7)

        event(55, "tentative internal distinction commits positive at 0.55")
        engine.absorb(commitment(locus, Polarity.POSITIVE, 0.55f, generation = 1))
        val predictedPolarity = engine.commitmentSnapshot().single().polarity
        check(predictedPolarity == Polarity.POSITIVE)

        event(100, "comparison evidence is strongly negative at 0.80")
        engine.absorb(commitment(locus, Polarity.NEGATIVE, 0.80f, generation = 2))
        val revised = engine.commitmentSnapshot().single()
        check(revised.polarity == Polarity.NEGATIVE)
        assertNear(revised.residualForce, 0.25f)
        check(revised.flags.has(CommitmentFlags.CONTESTED))
        check(engine.decisionConfidence(locus) == 0.0f) // contested belief cannot autonomously drive action
    }

    private fun reconstructionAfterProjectionLoss() {
        println("\n[180s] reconstruction benchmark")
        val original = ReasoningEngine()
        original.absorb(commitment(Locus.fromParts(7, 3), Polarity.POSITIVE, 0.81f, generation = 1))
        original.absorb(commitment(Locus.fromParts(8, 4), Polarity.NEGATIVE, 0.62f, generation = 2))

        val beforeBeliefs = original.currentBeliefs()
        val beforeContext = original.neuralContext()
        val residualOnly = original.commitmentSnapshot()

        // Simulate losing every rebuildable projection / model and retaining only
        // the residual commitment field.
        val rebuilt = ReasoningEngine()
        rebuilt.restoreFrom(residualOnly)
        check(rebuilt.currentBeliefs() == beforeBeliefs)
        check(rebuilt.neuralContext() == beforeContext)
        event(180, "derived projections discarded; residual commitments reconstruct the same cognitive projection")
    }

    private fun event(second: Int, text: String) {
        println("t=${second.toString().padStart(3, '0')}s  input> $text")
    }

    private fun commitment(
        locus: Locus,
        polarity: Polarity,
        force: Float,
        generation: Long
    ) = ResidualCommitment(
        locus = locus,
        polarity = polarity,
        residualForce = force,
        contextualBinding = 0.5f,
        temporalPersistence = TemporalAnchor(generation = generation)
    )

    private fun assertNear(actual: Float, expected: Float, epsilon: Float = 0.0001f) {
        check(abs(actual - expected) <= epsilon) { "expected $expected, got $actual" }
    }
}

/**
 * Prototype of the later "little agent" consolidation rule.
 *
 * It is intentionally test-only for this experiment: we first prove the behavior
 * before changing Frank's production persistence contract.
 */
private class ThreePassStabilityGate(
    private val requiredPasses: Int = 3,
    private val minimumSolidConfidence: Float = 0.85f
) {
    private data class Dimension(
        val personId: String,
        val axis: String,
        val scopeId: Int
    )

    private data class Signature(
        val value: String,
        val polarity: Int
    )

    private data class Track(
        var signature: Signature,
        var lastPassId: Int,
        var streak: Int
    )

    private val tracks = mutableMapOf<Dimension, Track>()

    fun evaluate(passId: Int, observation: MemoryObservation): Boolean {
        require(passId > 0)
        val dimension = Dimension(observation.personId.toString(), observation.axis, observation.scopeId)

        if (observation.source == ObservationSource.DRAFT_OR_CLIPPING ||
            observation.confidence < minimumSolidConfidence
        ) {
            tracks.remove(dimension)
            return false
        }

        val signature = Signature(observation.value, observation.polarity)
        val current = tracks[dimension]

        if (current == null || current.signature != signature) {
            tracks[dimension] = Track(signature, passId, 1)
            return requiredPasses <= 1
        }

        // Multiple samples from the same evaluator pass are still one pass.
        if (current.lastPassId == passId) return false

        current.lastPassId = passId
        current.streak += 1
        return current.streak >= requiredPasses
    }
}
