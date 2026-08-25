package frank.cognition

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Biology-first developmental cortex.
 *
 * No semantic label is born here. Novel loci emerge from distributed sensory
 * feature activity. Repeated/co-active ensembles become easier to recruit;
 * related ensembles acquire local association through co-activation.
 *
 * This is intentionally not a hash table, symbol registry, or database key factory.
 */
data class SensoryFeatureVector(val values: FloatArray) {
    init { require(values.isNotEmpty()) }

    fun normalized(): SensoryFeatureVector {
        val norm = sqrt(values.sumOf { (it * it).toDouble() }).toFloat()
        if (norm <= 1e-9f) return this
        return SensoryFeatureVector(FloatArray(values.size) { values[it] / norm })
    }

    fun cosine(other: SensoryFeatureVector): Float {
        require(values.size == other.values.size)
        var dot = 0f
        var aa = 0f
        var bb = 0f
        for (i in values.indices) {
            dot += values[i] * other.values[i]
            aa += values[i] * values[i]
            bb += other.values[i] * other.values[i]
        }
        val denom = sqrt(aa * bb)
        return if (denom <= 1e-9f) 0f else (dot / denom).coerceIn(-1f, 1f)
    }
}

data class DevelopmentalLocus(
    val locus: Locus,
    val prototype: SensoryFeatureVector,
    val exposureCount: Int,
    val recruitmentStrength: Float,
    val lastActiveTick: Long
)

data class AssociationEdge(
    val a: Locus,
    val b: Locus,
    val strength: Float,
    val coactivations: Int,
    val lastCoactiveTick: Long
)

data class LocusRecruitment(
    val primary: Locus,
    val similarity: Float,
    val born: Boolean,
    val coactive: List<Locus>
)

class DevelopmentalCortex(
    private val matchThreshold: Float = 0.86f,
    private val coactiveThreshold: Float = 0.70f,
    private val prototypeLearningRate: Float = 0.08f,
    private val associationGain: Float = 0.08f,
    private val associationDecay: Float = 0.997f
) {
    private val loci = linkedMapOf<Long, DevelopmentalLocus>()
    private val associations = linkedMapOf<Pair<Long, Long>, AssociationEdge>()
    private var nextLocusRaw = 1L

    fun observe(features: SensoryFeatureVector, tick: Long): LocusRecruitment {
        require(tick >= 0)
        val input = features.normalized()
        val ranked = loci.values
            .map { it to it.prototype.cosine(input) }
            .sortedByDescending { it.second }

        val best = ranked.firstOrNull()
        val primary = if (best == null || best.second < matchThreshold) {
            val locus = Locus(nextLocusRaw++)
            loci[locus.raw] = DevelopmentalLocus(locus, input, 1, 0.5f, tick)
            locus
        } else {
            val old = best.first
            val merged = FloatArray(input.values.size) { i ->
                old.prototype.values[i] * (1f - prototypeLearningRate) + input.values[i] * prototypeLearningRate
            }
            loci[old.locus.raw] = old.copy(
                prototype = SensoryFeatureVector(merged).normalized(),
                exposureCount = old.exposureCount + 1,
                recruitmentStrength = min(1f, old.recruitmentStrength + 0.03f),
                lastActiveTick = tick
            )
            old.locus
        }

        val coactive = ranked
            .filter { (candidate, sim) -> candidate.locus != primary && sim >= coactiveThreshold }
            .map { it.first.locus }

        coactive.forEach { bind(primary, it, tick) }
        decayAssociations()

        val sim = if (best == null) 0f else best.second
        return LocusRecruitment(primary, sim, best == null || best.second < matchThreshold, coactive)
    }

    fun association(a: Locus, b: Locus): Float = associations[key(a,b)]?.strength ?: 0f
    fun neighbors(locus: Locus, minStrength: Float = 0.05f): List<Pair<Locus, Float>> =
        associations.values.mapNotNull { e ->
            when {
                e.strength < minStrength -> null
                e.a == locus -> e.b to e.strength
                e.b == locus -> e.a to e.strength
                else -> null
            }
        }.sortedByDescending { it.second }

    fun snapshotLoci(): List<DevelopmentalLocus> = loci.values.toList()
    fun snapshotAssociations(): List<AssociationEdge> = associations.values.toList()

    private fun bind(a: Locus, b: Locus, tick: Long) {
        if (a == b) return
        val k = key(a,b)
        val old = associations[k]
        associations[k] = if (old == null) {
            AssociationEdge(if (a.raw < b.raw) a else b, if (a.raw < b.raw) b else a, associationGain, 1, tick)
        } else old.copy(
            strength = min(1f, old.strength + associationGain * (1f - old.strength)),
            coactivations = old.coactivations + 1,
            lastCoactiveTick = tick
        )
    }

    private fun decayAssociations() {
        associations.replaceAll { _, e -> e.copy(strength = max(0f, e.strength * associationDecay)) }
    }

    private fun key(a: Locus, b: Locus): Pair<Long,Long> =
        if (a.raw <= b.raw) a.raw to b.raw else b.raw to a.raw
}
