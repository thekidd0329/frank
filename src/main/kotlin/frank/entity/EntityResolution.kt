package frank.entity

import java.util.UUID
import kotlin.math.max
import kotlin.math.min

typealias EntityId = UUID

data class EntityHypothesis(
    val entityId: EntityId,
    val confidence: Float,
    val evidenceIds: List<String> = emptyList(),
    val sourceTypes: Set<String> = emptySet(),
    val displayName: String? = null
)

data class EntityHypothesisSet(
    val rawToken: String,
    val hypotheses: List<EntityHypothesis>,
    val resolvedAt: Long = System.currentTimeMillis()
) {
    val top: EntityHypothesis? get() = hypotheses.firstOrNull()
    val second: EntityHypothesis? get() = hypotheses.getOrNull(1)

    fun ambiguityCost(thresholdGap: Float = 0.15f): Float {
        val topConf = top?.confidence ?: 0f
        val secondConf = second?.confidence ?: 0f
        val gap = topConf - secondConf
        return max(0f, thresholdGap - gap)
    }

    fun effectiveConfidence(): Float {
        val topConf = top?.confidence ?: 0f
        return topConf * (1f - ambiguityCost())
    }

    fun isHighConfidence(minConf: Float = 0.92f, minGap: Float = 0.15f): Boolean {
        val topConf = top?.confidence ?: return false
        val gap = topConf - (second?.confidence ?: 0f)
        return topConf >= minConf && gap >= minGap
    }
}

class SessionEntityBuffer(
    private val maxSize: Int = 12
) {
    private val recent = ArrayDeque<Pair<String, EntityId>>()

    fun record(token: String, entityId: EntityId) {
        recent.removeAll { it.first.equals(token, ignoreCase = true) }
        recent.addFirst(token.lowercase() to entityId)
        while (recent.size > maxSize) recent.removeLast()
    }

    fun lookup(token: String): EntityId? =
        recent.firstOrNull { it.first == token.lowercase() }?.second

    fun clear() = recent.clear()
}

data class ScoringWeights(
    val exactName: Float = 0.45f,
    val alias: Float = 0.30f,
    val relation: Float = 0.35f,
    val recency: Float = 0.15f,
    val frequency: Float = 0.10f,
    val session: Float = 0.40f
)

class EntityScorer(
    private val weights: ScoringWeights = ScoringWeights()
) {
    fun score(
        token: String,
        candidates: Map<EntityId, CandidateFeatures>
    ): Map<EntityId, Float> {
        return candidates.mapValues { (_, f) ->
            var s = 0f
            if (f.exactNameMatch) s += weights.exactName
            if (f.aliasMatch) s += weights.alias
            s += f.relationStrength * weights.relation
            s += f.recencyScore * weights.recency
            s += f.frequencyScore * weights.frequency
            if (f.fromSession) s += weights.session
            s.coerceIn(0f, 1.5f)
        }
    }
}

data class CandidateFeatures(
    val exactNameMatch: Boolean = false,
    val aliasMatch: Boolean = false,
    val relationStrength: Float = 0f,
    val recencyScore: Float = 0f,
    val frequencyScore: Float = 0f,
    val fromSession: Boolean = false,
    val evidenceIds: List<String> = emptyList(),
    val sourceTypes: Set<String> = emptySet(),
    val displayName: String? = null
)

interface EntityStore {
    fun findCandidates(token: String, roleHints: Set<String> = emptySet()): Map<EntityId, CandidateFeatures>
}

class EntityResolver(
    private val store: EntityStore,
    private val sessionBuffer: SessionEntityBuffer = SessionEntityBuffer(),
    private val scorer: EntityScorer = EntityScorer()
) {
    fun resolve(
        token: String,
        roleHints: Set<String> = emptySet(),
        contextEntityIds: Set<EntityId> = emptySet()
    ): EntityHypothesisSet {
        val normalized = token.trim().lowercase()

        sessionBuffer.lookup(normalized)?.let { sid ->
            return EntityHypothesisSet(
                rawToken = token,
                hypotheses = listOf(
                    EntityHypothesis(
                        entityId = sid,
                        confidence = 0.97f,
                        sourceTypes = setOf("session"),
                        displayName = token
                    )
                )
            )
        }

        val candidates = store.findCandidates(normalized, roleHints)
        if (candidates.isEmpty()) {
            return EntityHypothesisSet(token, emptyList())
        }

        val rawScores = scorer.score(normalized, candidates)
        val maxScore = rawScores.values.maxOrNull() ?: 1f
        val expScores = rawScores.mapValues { (_, v) -> kotlin.math.exp((v - maxScore) * 3.0).toFloat() }
        val sum = expScores.values.sum().coerceAtLeast(1e-6f)
        val probs = expScores.mapValues { it.value / sum }

        val hypotheses = probs.entries
            .sortedByDescending { it.value }
            .map { (id, conf) ->
                val feat = candidates.getValue(id)
                EntityHypothesis(
                    entityId = id,
                    confidence = conf,
                    evidenceIds = feat.evidenceIds,
                    sourceTypes = feat.sourceTypes,
                    displayName = feat.displayName
                )
            }

        return EntityHypothesisSet(token, hypotheses)
    }

    fun recordSuccessfulResolution(token: String, entityId: EntityId) {
        sessionBuffer.record(token, entityId)
    }
}

fun EntityHypothesisSet.identityConfidenceForAutonomy(): Float =
    effectiveConfidence()

data class EntityProvenanceFragment(
    val rawToken: String,
    val chosenEntityId: EntityId?,
    val topConfidence: Float,
    val ambiguityCost: Float,
    val supportingClaimIds: List<String>
)

fun EntityHypothesisSet.toProvenanceFragment(chosen: EntityId? = top?.entityId): EntityProvenanceFragment {
    val hyp = hypotheses.firstOrNull { it.entityId == chosen } ?: top
    return EntityProvenanceFragment(
        rawToken = rawToken,
        chosenEntityId = chosen,
        topConfidence = hyp?.confidence ?: 0f,
        ambiguityCost = ambiguityCost(),
        supportingClaimIds = hyp?.evidenceIds ?: emptyList()
    )
}
