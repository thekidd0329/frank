package frank.entity

/**
 * Integration-layer guard around the locked CompactMemoryEntityStore contract.
 * It preserves alias and relationship behavior while preventing positive NAME
 * claim presence from becoming a blanket exact-name match.
 */
class EntityStoreIntegration(
    private val reader: CompactMemoryReader,
    private val delegate: EntityStore = CompactMemoryEntityStore(reader)
) : EntityStore {

    private val aliasesByEntity: Map<EntityId, List<EntityAlias>> by lazy {
        reader.aliases().groupBy { it.entityId }
    }

    override fun findCandidates(
        token: String,
        roleHints: Set<String>
    ): Map<EntityId, CandidateFeatures> {
        val normalized = normalize(token)
        if (normalized.isBlank()) return emptyMap()

        val expandedHints = buildSet {
            roleHints.forEach { hint ->
                val h = hint.lowercase()
                add(h)
                if (h == "parent") {
                    add("father")
                    add("mother")
                }
            }
        }

        val positiveNameEntities = reader.allClaims()
            .asSequence()
            .filter { it.axis == RelationAxes.NAME && it.polarity > 0 }
            .map { it.entityId }
            .toSet()

        return delegate.findCandidates(normalized, expandedHints)
            .mapNotNull { (entityId, features) ->
                val displayMatch = reader.entityDisplayName(entityId)
                    ?.let(::normalize) == normalized
                val aliasMatch = aliasesByEntity[entityId]
                    ?.any { normalize(it.normalizedAlias) == normalized } == true

                val validExactName = features.exactNameMatch &&
                    entityId in positiveNameEntities &&
                    (displayMatch || aliasMatch)

                val keep = validExactName || aliasMatch || features.relationStrength > 0f
                if (!keep) return@mapNotNull null

                entityId to features.copy(
                    exactNameMatch = validExactName,
                    aliasMatch = aliasMatch,
                    displayName = features.displayName ?: reader.entityDisplayName(entityId)
                )
            }
            .toMap()
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}
