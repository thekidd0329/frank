package frank.provenance

import frank.entity.EntityProvenanceFragment
import frank.memory.ClaimConflictTracker
import java.util.UUID

enum class ActionOutcome { PROPOSED, EXECUTED, VERIFIED, CORRECTED, UNDONE, FAILED }

data class ActionRecord(
    val actionId: String = UUID.randomUUID().toString(),
    val capabilityId: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val entityFragments: List<EntityProvenanceFragment> = emptyList(),
    val supportingClaimIds: Set<String> = emptySet(),
    val decisionConfidence: Float,
    val outcome: ActionOutcome = ActionOutcome.PROPOSED,
    val parentActionId: String? = null
) {
    fun allSupportingClaimIds(): Set<String> = buildSet {
        addAll(supportingClaimIds)
        entityFragments.forEach { addAll(it.supportingClaimIds) }
    }
}

class ActionProvenanceLog {
    private val records = linkedMapOf<String, ActionRecord>()

    fun append(record: ActionRecord): ActionRecord {
        records[record.actionId] = record
        return record
    }

    fun get(actionId: String): ActionRecord? = records[actionId]

    fun markOutcome(actionId: String, outcome: ActionOutcome): ActionRecord? {
        val record = records[actionId] ?: return null
        val updated = record.copy(outcome = outcome)
        records[actionId] = updated
        return updated
    }

    fun descendantsOf(actionId: String): List<ActionRecord> =
        records.values.filter { it.parentActionId == actionId }
}

class CorrectionDemoter(
    private val log: ActionProvenanceLog,
    private val conflicts: ClaimConflictTracker,
    private val correctionStrength: Float = 0.95f
) {
    fun correct(actionId: String): Set<String> {
        val record = log.get(actionId) ?: return emptySet()
        val affected = record.allSupportingClaimIds()
        affected.forEach { claimId -> conflicts.applyEvidence(claimId, correctionStrength, supports = false) }
        log.markOutcome(actionId, ActionOutcome.CORRECTED)
        return affected
    }
}
