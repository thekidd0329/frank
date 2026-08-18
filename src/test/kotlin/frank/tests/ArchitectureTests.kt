package frank.tests

import frank.autonomy.AutonomyEvaluator
import frank.autonomy.RiskDimension
import frank.autonomy.SourceChannel
import frank.capability.Capability
import frank.capability.CapabilityRegistry
import frank.entity.*
import frank.memory.*
import frank.provenance.*
import java.util.UUID
import kotlin.math.abs

private var passed = 0
private var failed = 0

private fun test(name: String, block: () -> Unit) {
    try {
        block()
        passed++
        println("PASS  $name")
    } catch (t: Throwable) {
        failed++
        println("FAIL  $name -> ${t.message}")
    }
}

private fun assertNear(actual: Float, expected: Float, epsilon: Float = 0.001f) {
    check(abs(actual - expected) <= epsilon) { "expected $expected, got $actual" }
}

private fun claim(
    id: String,
    entity: EntityId,
    axis: Int,
    value: Int,
    confidence: Float = 0.9f,
    recency: Float = 0.9f,
    polarity: Int = 1,
    scopeId: Int = 0,
    observations: Int = 1
) = CompactClaim(
    claimId = id,
    entityId = entity,
    axis = axis,
    value = value,
    confidence = confidence,
    recency = recency,
    polarity = polarity,
    scopeId = scopeId,
    observationCount = observations
)

fun main() {
    test("single Dad relationship resolves") {
        val dad = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            setDisplayName(dad, "David")
            upsertClaim(claim("dad-rel", dad, RelationAxes.RELATIONSHIP, RelationValues.FATHER, confidence = 0.96f))
        }
        val result = EntityResolver(EntityStoreIntegration(memory)).resolve("Dad", setOf("father"))
        check(result.top?.entityId == dad)
        check(result.hypotheses.size == 1)
    }

    test("two Dad candidates remain ambiguous") {
        val dadA = UUID.randomUUID(); val dadB = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            setDisplayName(dadA, "David")
            setDisplayName(dadB, "Daniel")
            upsertClaim(claim("dad-a", dadA, RelationAxes.RELATIONSHIP, RelationValues.FATHER, confidence = 0.82f))
            upsertClaim(claim("dad-b", dadB, RelationAxes.RELATIONSHIP, RelationValues.FATHER, confidence = 0.80f))
        }
        val result = EntityResolver(EntityStoreIntegration(memory)).resolve("Dad", setOf("father"))
        check(result.hypotheses.size == 2)
        check(!result.isHighConfidence())
        check(result.ambiguityCost() > 0f)
    }

    test("negative relationship claim is filtered") {
        val wrong = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            upsertClaim(claim("not-dad", wrong, RelationAxes.RELATIONSHIP, RelationValues.FATHER, polarity = -1))
        }
        val result = EntityResolver(EntityStoreIntegration(memory)).resolve("Dad", setOf("father"))
        check(result.hypotheses.isEmpty())
    }

    test("alias resolution feeds scorer without requiring NAME claim") {
        val dad = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            setDisplayName(dad, "David")
            putAlias(EntityAlias("dad", dad, 1f))
        }
        val result = EntityResolver(EntityStoreIntegration(memory)).resolve("Dad")
        check(result.top?.entityId == dad)
    }

    test("session successful resolution fast-path") {
        val dad = UUID.randomUUID()
        val store = object : EntityStore {
            override fun findCandidates(token: String, roleHints: Set<String>) = emptyMap<EntityId, CandidateFeatures>()
        }
        val resolver = EntityResolver(store)
        resolver.recordSuccessfulResolution("him", dad)
        val result = resolver.resolve("him")
        check(result.top?.entityId == dad)
        assertNear(result.top!!.confidence, 0.97f)
    }

    test("provenance fragment carries selected claim IDs") {
        val dad = UUID.randomUUID()
        val set = EntityHypothesisSet(
            "Dad",
            listOf(EntityHypothesis(dad, 0.95f, evidenceIds = listOf("r1", "n1")))
        )
        val p = set.toProvenanceFragment()
        check(p.chosenEntityId == dad)
        check(p.supportingClaimIds == listOf("r1", "n1"))
    }

    test("empty memory returns no hypotheses") {
        val result = EntityResolver(EntityStoreIntegration(MutableCompactMemory())).resolve("Dad", setOf("father"))
        check(result.hypotheses.isEmpty())
    }

    test("unknown token does not match unrelated positive NAME claims") {
        val alice = UUID.randomUUID(); val bob = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            setDisplayName(alice, "Alice")
            setDisplayName(bob, "Bob")
            upsertClaim(claim("alice-name", alice, RelationAxes.NAME, 101))
            upsertClaim(claim("bob-name", bob, RelationAxes.NAME, 102))
        }
        val result = EntityResolver(EntityStoreIntegration(memory)).resolve("completely_unknown")
        check(result.hypotheses.isEmpty())
    }

    test("positive NAME claim gates exact display-name match") {
        val alice = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            setDisplayName(alice, "Alice")
            upsertClaim(claim("alice-name", alice, RelationAxes.NAME, 101))
        }
        val features = EntityStoreIntegration(memory).findCandidates("alice")
        check(features[alice]?.exactNameMatch == true)
    }

    test("parent role expands to father and mother values") {
        val dad = UUID.randomUUID(); val mom = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            upsertClaim(claim("father", dad, RelationAxes.RELATIONSHIP, RelationValues.FATHER, 0.9f))
            upsertClaim(claim("mother", mom, RelationAxes.RELATIONSHIP, RelationValues.MOTHER, 0.9f))
        }
        val features = EntityStoreIntegration(memory).findCandidates("parent", setOf("parent"))
        check(features.keys == setOf(dad, mom))
    }

    test("ontology IDs are stable and values remain axis-local") {
        val ontology = AdaptiveOntology()
        val aesthetic = ontology.getOrCreateAxis("aesthetic preference")
        val food = ontology.getOrCreateAxis("food preference")
        val gothic1 = ontology.getOrCreateValue(aesthetic, "gothic")
        val gothic2 = ontology.getOrCreateValue(aesthetic, "Gothic")
        val foodGothic = ontology.getOrCreateValue(food, "gothic")
        check(gothic1 == gothic2)
        check(foodGothic == 1)
        check(ontology.resolveValue(aesthetic, gothic1) == "gothic")
    }

    test("ontology proposal does not canonicalize until caller promotes") {
        val ontology = AdaptiveOntology()
        ontology.getOrCreateAxis("aesthetic")
        val proposal = ontology.propose("aesthetic", "brutalist")
        check(proposal.similarValueId == null)
        val (axis, value) = ontology.canonicalize(proposal)
        check(ontology.resolveValue(axis, value) == "brutalist")
    }

    test("explicit owner observation promotes immediately and stores polarity/scope") {
        val person = UUID.randomUUID()
        val memory = MutableCompactMemory()
        val engine = CombingEngine(memory, AdaptiveOntology())
        val durable = engine.observe(
            MemoryObservation(person, "aesthetic", "gothic", 0.96f, 1f, polarity = -1, scopeId = 7,
                source = ObservationSource.OWNER_EXPLICIT, timestampMillis = 100L)
        )
        check(durable != null)
        check(durable!!.polarity == -1)
        check(durable.scopeId == 7)
    }

    test("passive combing requires three observations") {
        val person = UUID.randomUUID()
        val memory = MutableCompactMemory()
        val engine = CombingEngine(memory, AdaptiveOntology())
        fun obs(ts: Long) = MemoryObservation(person, "hobby", "drawing", 0.8f, 0.9f,
            source = ObservationSource.PASSIVE_FILE, timestampMillis = ts)
        check(engine.observe(obs(1L)) == null)
        check(engine.observe(obs(2L)) == null)
        check(engine.observe(obs(3L)) != null)
        check(memory.allClaims().size == 1)
    }

    test("drafts have zero promotion authority") {
        val person = UUID.randomUUID()
        val engine = CombingEngine(MutableCompactMemory(), AdaptiveOntology())
        repeat(5) { i ->
            val result = engine.observe(MemoryObservation(person, "aesthetic", "gothic", 0.99f, 1f,
                source = ObservationSource.DRAFT_OR_CLIPPING, timestampMillis = i.toLong()))
            check(result == null)
        }
    }

    test("contradictory evidence can quarantine claim as contested") {
        val person = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            upsertClaim(claim("pref", person, 1, 1, confidence = 0.90f))
        }
        val tracker = ClaimConflictTracker(memory)
        val state = tracker.applyEvidence("pref", 0.90f, supports = false)
        check(state != null)
        check(state!!.status == ClaimStatus.CONTESTED)
        assertNear(state.confidence, 0.5f, 0.01f)
    }

    test("capability registry ranks uncertainty-reducing tools") {
        val registry = CapabilityRegistry().apply {
            register(Capability("social.responses.read", "mock-social",
                reducesUncertaintyOn = setOf("post.reply_status"), costScore = 1f))
            register(Capability("calendar.inspect", "calendar",
                reducesUncertaintyOn = setOf("calendar.availability"), costScore = 0.5f))
        }
        val matches = registry.candidatesFor(setOf("post.reply_status"))
        check(matches.map { it.id } == listOf("social.responses.read"))
    }

    test("decision-relevant probabilities multiply instead of averaging") {
        val decision = AutonomyEvaluator().evaluate(
            listOf(
                RiskDimension("identity", 0.99f),
                RiskDimension("target", 0.91f),
                RiskDimension("reply_status", 0.99f),
                RiskDimension("intent", 0.96f)
            )
        )
        assertNear(decision.finalConfidence, 0.856f, 0.002f)
        check(!decision.autoExecute)
        check(decision.weakestRelevantDimension?.name == "target")
    }

    test("non-decision-relevant certainty does not suppress autonomy") {
        val decision = AutonomyEvaluator().evaluate(
            listOf(
                RiskDimension("identity", 0.99f),
                RiskDimension("target", 0.99f),
                RiskDimension("irrelevant_debug", 0.10f, decisionRelevant = false)
            )
        )
        check(decision.autoExecute)
    }

    test("source exhaustiveness prevents stale-zero false certainty") {
        val decision = AutonomyEvaluator().evaluate(
            listOf(RiskDimension("no_reply", 0.99f)),
            listOf(
                SourceChannel("facebook", checked = true),
                SourceChannel("sms", checked = false),
                SourceChannel("whatsapp", checked = false)
            )
        )
        assertNear(decision.exhaustiveness, 1f / 3f, 0.001f)
        check(decision.finalConfidence < 0.90f)
        check(!decision.autoExecute)
    }

    test("correction demotes only claims that justified the action") {
        val person = UUID.randomUUID()
        val memory = MutableCompactMemory().apply {
            upsertClaim(claim("wrong-dad", person, RelationAxes.RELATIONSHIP, RelationValues.FATHER, confidence = 0.95f))
            upsertClaim(claim("unrelated", person, 9, 9, confidence = 0.95f))
        }
        val tracker = ClaimConflictTracker(memory)
        val log = ActionProvenanceLog()
        val action = log.append(ActionRecord(
            capabilityId = "social.person.mention",
            entityFragments = listOf(EntityProvenanceFragment("Dad", person, 0.96f, 0f, listOf("wrong-dad"))),
            decisionConfidence = 0.93f,
            outcome = ActionOutcome.EXECUTED
        ))
        val beforeUnrelated = memory.claim("unrelated")!!.confidence
        val affected = CorrectionDemoter(log, tracker).correct(action.actionId)
        check(affected == setOf("wrong-dad"))
        check(memory.claim("wrong-dad")!!.confidence <= 0.5f)
        check(tracker.status("wrong-dad") == ClaimStatus.CONTESTED)
        assertNear(memory.claim("unrelated")!!.confidence, beforeUnrelated)
        check(log.get(action.actionId)?.outcome == ActionOutcome.CORRECTED)
    }

    println("\nRESULT  $passed passed, $failed failed")
    check(failed == 0) { "$failed architecture test(s) failed" }
}
