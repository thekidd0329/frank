package frank.entity

import java.util.UUID
import kotlin.math.exp
import kotlin.math.ln

data class CompactClaim(
    val claimId: String,
    val entityId: EntityId,
    val axis: Int,
    val value: Int,
    val confidence: Float,
    val recency: Float,
    val polarity: Int = 1,
    val scopeId: Int = 0,
    val lastSeenMillis: Long = 0L,
    val observationCount: Int = 1
)

data class EntityAlias(
    val normalizedAlias: String,
    val entityId: EntityId,
    val strength: Float = 1.0f
)

interface CompactMemoryReader {
    fun allClaims(): List<CompactClaim>
    fun aliases(): List<EntityAlias>
    fun entityDisplayName(entityId: EntityId): String?
}

object RelationAxes {
    const val RELATIONSHIP = 3
    const val NAME = 1
}

object RelationValues {
    const val FATHER = 12
    const val MOTHER = 13
    const val PARENT = 14
    const val PARTNER = 20
}

class CompactMemoryEntityStore(
    private val memory: CompactMemoryReader,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : EntityStore {

    override fun findCandidates(
        token: String,
        roleHints: Set<String>
    ): Map<EntityId, CandidateFeatures> {

        val normalized = token.trim().lowercase()
        val result = mutableMapOf<EntityId, MutableCandidate>()

        memory.aliases()
            .filter { it.normalizedAlias == normalized }
            .forEach { alias ->
                val c = result.getOrPut(alias.entityId) { MutableCandidate() }
                c.aliasMatch = true
                c.displayName = memory.entityDisplayName(alias.entityId)
            }

        memory.allClaims()
            .filter { it.axis == RelationAxes.NAME && it.polarity > 0 }
            .forEach { claim ->
                val c = result.getOrPut(claim.entityId) { MutableCandidate() }
                c.exactNameMatch = true
                c.addEvidence(claim)
            }

        val targetValues = roleHintsToValues(roleHints)
        memory.allClaims()
            .filter { it.axis == RelationAxes.RELATIONSHIP && it.polarity > 0 }
            .filter { it.value in targetValues }
            .forEach { claim ->
                val c = result.getOrPut(claim.entityId) { MutableCandidate() }
                c.relationStrength = maxOf(c.relationStrength, claim.confidence)
                c.addEvidence(claim)
            }

        return result.mapValues { (entityId, mut) ->
            CandidateFeatures(
                exactNameMatch = mut.exactNameMatch,
                aliasMatch = mut.aliasMatch,
                relationStrength = mut.relationStrength.coerceIn(0f, 1f),
                recencyScore = mut.bestRecency(),
                frequencyScore = frequencyScore(mut.observationCount),
                fromSession = false,
                evidenceIds = mut.evidenceIds.toList(),
                sourceTypes = mut.sourceTypes,
                displayName = mut.displayName ?: memory.entityDisplayName(entityId)
            )
        }
    }

    private fun roleHintsToValues(hints: Set<String>): Set<Int> {
        val map = mapOf(
            "father" to RelationValues.FATHER,
            "dad" to RelationValues.FATHER,
            "mother" to RelationValues.MOTHER,
            "mom" to RelationValues.MOTHER,
            "parent" to RelationValues.PARENT,
            "partner" to RelationValues.PARTNER
        )
        return hints.mapNotNull { map[it.lowercase()] }.toSet()
    }

    private fun frequencyScore(count: Int): Float {
        return (1f - exp(-0.4 * count)).coerceIn(0f, 1f)
    }

    private class MutableCandidate {
        var exactNameMatch = false
        var aliasMatch = false
        var relationStrength = 0f
        var displayName: String? = null
        val evidenceIds = mutableSetOf<String>()
        val sourceTypes = mutableSetOf<String>()
        var bestRecencyValue = 0f
        var observationCount = 0
        var latestSeen = 0L

        fun addEvidence(claim: CompactClaim) {
            evidenceIds += claim.claimId
            sourceTypes += "claim"
            observationCount += claim.observationCount
            if (claim.recency > bestRecencyValue) bestRecencyValue = claim.recency
            if (claim.lastSeenMillis > latestSeen) latestSeen = claim.lastSeenMillis
        }

        fun bestRecency(): Float = bestRecencyValue
    }
}
