package frank.memory

import frank.entity.CompactClaim
import frank.entity.CompactMemoryReader
import frank.entity.EntityAlias
import frank.entity.EntityId
import java.util.UUID
import kotlin.math.max

class MutableCompactMemory : CompactMemoryReader {
    private val claims = linkedMapOf<String, CompactClaim>()
    private val aliases = linkedMapOf<Pair<String, EntityId>, EntityAlias>()
    private val displayNames = linkedMapOf<EntityId, String>()

    override fun allClaims(): List<CompactClaim> = claims.values.toList()
    override fun aliases(): List<EntityAlias> = aliases.values.toList()
    override fun entityDisplayName(entityId: EntityId): String? = displayNames[entityId]

    fun upsertClaim(claim: CompactClaim) { claims[claim.claimId] = claim }
    fun claim(claimId: String): CompactClaim? = claims[claimId]
    fun setDisplayName(entityId: EntityId, displayName: String) { displayNames[entityId] = displayName }
    fun putAlias(alias: EntityAlias) { aliases[normalize(alias.normalizedAlias) to alias.entityId] = alias.copy(normalizedAlias = normalize(alias.normalizedAlias)) }

    fun replaceClaimConfidence(claimId: String, confidence: Float): CompactClaim? {
        val existing = claims[claimId] ?: return null
        val updated = existing.copy(confidence = confidence.coerceIn(0f, 1f))
        claims[claimId] = updated
        return updated
    }

    private fun normalize(value: String) = value.trim().lowercase()
}

class AdaptiveOntology {
    data class Proposal(
        val axisToken: String,
        val valueToken: String,
        val suggestedAxisId: Int?,
        val similarValueId: Int?,
        val similarity: Float
    )

    private val axisIds = linkedMapOf<String, Int>()
    private val axisNames = linkedMapOf<Int, String>()
    private val valueIds = linkedMapOf<Int, LinkedHashMap<String, Int>>()
    private val valueNames = linkedMapOf<Int, LinkedHashMap<Int, String>>()
    private val aliasMap = linkedMapOf<Pair<Int, String>, Int>()

    fun getOrCreateAxis(concept: String): Int {
        val token = normalize(concept)
        require(token.isNotBlank())
        return axisIds[token] ?: (axisNames.keys.maxOrNull() ?: 0).plus(1).also { id ->
            require(id <= 0xFFFF)
            axisIds[token] = id
            axisNames[id] = token
            valueIds[id] = linkedMapOf()
            valueNames[id] = linkedMapOf()
        }
    }

    fun getOrCreateValue(axisId: Int, concept: String): Int {
        require(axisId in axisNames)
        val token = normalize(concept)
        aliasMap[axisId to token]?.let { return it }
        valueIds.getValue(axisId)[token]?.let { return it }
        val id = (valueNames.getValue(axisId).keys.maxOrNull() ?: 0) + 1
        require(id <= 0xFFFF)
        valueIds.getValue(axisId)[token] = id
        valueNames.getValue(axisId)[id] = token
        return id
    }

    fun propose(axisConcept: String, valueConcept: String): Proposal {
        val axisToken = normalize(axisConcept)
        val valueToken = normalize(valueConcept)
        val axisId = axisIds[axisToken]
        if (axisId == null) return Proposal(axisToken, valueToken, null, null, 0f)

        aliasMap[axisId to valueToken]?.let { return Proposal(axisToken, valueToken, axisId, it, 1f) }
        valueIds.getValue(axisId)[valueToken]?.let { return Proposal(axisToken, valueToken, axisId, it, 1f) }

        val best = valueNames.getValue(axisId)
            .map { (id, existing) -> id to trigramJaccard(valueToken, existing) }
            .maxByOrNull { it.second }
        return Proposal(axisToken, valueToken, axisId, best?.first, best?.second ?: 0f)
    }

    fun canonicalize(proposal: Proposal): Pair<Int, Int> {
        val axisId = proposal.suggestedAxisId ?: getOrCreateAxis(proposal.axisToken)
        val existing = proposal.similarValueId
        if (existing != null && proposal.similarity >= 0.82f) {
            aliasMap[axisId to proposal.valueToken] = existing
            return axisId to existing
        }
        return axisId to getOrCreateValue(axisId, proposal.valueToken)
    }

    fun resolveAxis(axisId: Int): String = axisNames.getValue(axisId)
    fun resolveValue(axisId: Int, valueId: Int): String = valueNames.getValue(axisId).getValue(valueId)

    private fun normalize(value: String): String = value.trim().lowercase()
        .replace(Regex("[\\s-]+"), "_")
        .replace(Regex("[^a-z0-9_]+"), "")
        .removeSuffix("_style")
        .removeSuffix("_aesthetic")

    private fun trigramJaccard(a: String, b: String): Float {
        fun grams(s: String): Set<String> = when {
            s.length < 3 -> setOf(s)
            else -> (0..s.length - 3).map { s.substring(it, it + 3) }.toSet()
        }
        val ga = grams(a); val gb = grams(b)
        if (ga.isEmpty() && gb.isEmpty()) return 1f
        val union = ga union gb
        return if (union.isEmpty()) 0f else (ga intersect gb).size.toFloat() / union.size.toFloat()
    }
}

enum class ObservationSource { OWNER_EXPLICIT, DIRECT_QUOTE, OUTBOUND_MESSAGE, INBOUND_MESSAGE, PASSIVE_FILE, DRAFT_OR_CLIPPING }

data class MemoryObservation(
    val personId: EntityId,
    val axis: String,
    val value: String,
    val confidence: Float,
    val recency: Float,
    val polarity: Int = 1,
    val scopeId: Int = 0,
    val source: ObservationSource,
    val timestampMillis: Long
)

class MemoryPromotionGate {
    private data class Key(val personId: EntityId, val axis: String, val value: String, val polarity: Int, val scopeId: Int)
    private val seen = linkedMapOf<Key, MutableSet<Long>>()

    fun observe(observation: MemoryObservation): Boolean {
        if (observation.source == ObservationSource.DRAFT_OR_CLIPPING) return false
        if (observation.source == ObservationSource.OWNER_EXPLICIT) return true
        if (observation.source == ObservationSource.DIRECT_QUOTE && observation.confidence >= 0.90f) return true

        val key = Key(observation.personId, observation.axis, observation.value, observation.polarity, observation.scopeId)
        val timestamps = seen.getOrPut(key) { linkedSetOf() }
        timestamps += observation.timestampMillis
        val required = when (observation.source) {
            ObservationSource.OUTBOUND_MESSAGE -> 2
            ObservationSource.INBOUND_MESSAGE, ObservationSource.PASSIVE_FILE -> 3
            else -> 2
        }
        return timestamps.size >= required
    }

    fun mayCanonicalizeOntology(observation: MemoryObservation): Boolean = observe(observation)
}

enum class ClaimStatus { ACTIVE, CONTESTED }

data class ClaimState(val claimId: String, val status: ClaimStatus, val confidence: Float)

class ClaimConflictTracker(
    private val memory: MutableCompactMemory,
    private val contestedLow: Float = 0.40f,
    private val contestedHigh: Float = 0.60f
) {
    private val statusByClaim = mutableMapOf<String, ClaimStatus>()

    fun applyEvidence(claimId: String, evidenceConfidence: Float, supports: Boolean): ClaimState? {
        val claim = memory.claim(claimId) ?: return null
        val prior = claim.confidence.coerceIn(0.001f, 0.999f).toDouble()
        val ev = evidenceConfidence.coerceIn(0.001f, 0.999f).toDouble()
        val priorOdds = prior / (1.0 - prior)
        val likelihood = if (supports) ev / (1.0 - ev) else (1.0 - ev) / ev
        val posteriorOdds = priorOdds * likelihood
        val posterior = (posteriorOdds / (1.0 + posteriorOdds)).toFloat().coerceIn(0f, 1f)
        memory.replaceClaimConfidence(claimId, posterior)
        val status = if (posterior in contestedLow..contestedHigh) ClaimStatus.CONTESTED else ClaimStatus.ACTIVE
        statusByClaim[claimId] = status
        return ClaimState(claimId, status, posterior)
    }

    fun status(claimId: String): ClaimStatus = statusByClaim[claimId] ?: ClaimStatus.ACTIVE
}
