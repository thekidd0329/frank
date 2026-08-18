package frank.memory

import frank.entity.CompactClaim
import java.util.UUID
import kotlin.math.max

/**
 * Converts promoted observations into compact durable claims.
 * Raw source text/path never enters the persistent claim.
 */
class CombingEngine(
    private val memory: MutableCompactMemory,
    private val ontology: AdaptiveOntology,
    private val promotionGate: MemoryPromotionGate = MemoryPromotionGate()
) {
    fun observe(observation: MemoryObservation): CompactClaim? {
        if (!promotionGate.observe(observation)) return null

        val proposal = ontology.propose(observation.axis, observation.value)
        val (axisId, valueId) = ontology.canonicalize(proposal)
        val existing = memory.allClaims().firstOrNull {
            it.entityId == observation.personId &&
                it.axis == axisId &&
                it.value == valueId &&
                it.polarity == observation.polarity &&
                it.scopeId == observation.scopeId
        }

        val claim = CompactClaim(
            claimId = existing?.claimId ?: UUID.randomUUID().toString(),
            entityId = observation.personId,
            axis = axisId,
            value = valueId,
            confidence = max(existing?.confidence ?: 0f, observation.confidence).coerceIn(0f, 1f),
            recency = max(existing?.recency ?: 0f, observation.recency).coerceIn(0f, 1f),
            polarity = observation.polarity,
            scopeId = observation.scopeId,
            lastSeenMillis = max(existing?.lastSeenMillis ?: 0L, observation.timestampMillis),
            observationCount = (existing?.observationCount ?: 0) + 1
        )
        memory.upsertClaim(claim)
        return claim
    }
}
