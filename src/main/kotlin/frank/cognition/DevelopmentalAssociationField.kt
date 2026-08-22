package frank.cognition

/**
 * Learns relationships between already-observed opaque loci.
 *
 * The field has no words, labels, objects, emotions, or ontology. A relation
 * exists only because two loci repeatedly occurred together. This is intended
 * to be the first bridge from persistence into learned structure.
 */
class DevelopmentalAssociationField(
    private val profile: MemoryDynamicsProfile = MemoryDynamicsProfile.CONTROL
) {
    private val edges = linkedMapOf<Edge, Float>()

    data class Edge(val from: Long, val to: Long)
    data class Recall(val locus: Long, val strength: Float)

    val size: Int get() = edges.size

    /**
     * Couple two experiences. The coupling is bidirectional but the two edge
     * strengths are stored independently so future asymmetric learning remains
     * possible without changing the persistence format.
     */
    fun associate(first: Long, second: Long, salience: Float = 1f) {
        require(first != second) { "association requires two distinct loci" }
        require(salience in 0f..1f) { "salience must be in 0..1" }
        strengthen(Edge(first, second), salience)
        strengthen(Edge(second, first), salience)
    }

    fun strength(from: Long, to: Long): Float = edges[Edge(from, to)] ?: 0f

    fun recall(from: Long, limit: Int = 8): List<Recall> {
        require(limit > 0)
        return edges.asSequence()
            .filter { (edge, strength) ->
                edge.from == from && strength >= profile.retrievalThreshold
            }
            .map { (edge, strength) -> Recall(edge.to, strength) }
            .sortedByDescending { it.strength }
            .take(limit)
            .toList()
    }

    /** Passive time reduces force but never creates or reverses an association. */
    fun decay(steps: Int = 1) {
        require(steps >= 0)
        repeat(steps) {
            val iterator = edges.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val next = entry.value * profile.passiveRetention
                if (next <= profile.pruneThreshold) iterator.remove()
                else entry.setValue(next)
            }
        }
    }

    fun snapshot(): Map<Edge, Float> = edges.toMap()

    private fun strengthen(edge: Edge, salience: Float) {
        val current = edges[edge] ?: 0f
        val available = 1f - current
        val delta = available * profile.associationGain * salience
        edges[edge] = (current + delta).coerceIn(0f, 1f)
    }
}
