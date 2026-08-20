package frank.cognition

import kotlin.math.abs

/**
 * Canonical geometric organization for the field.
 *
 * Geometry determines ordering and spacing only. It does not assign semantic
 * labels, emotions, relationships, or self-model content.
 */
object GoldenRatioFieldGeometry {
    data class Placement(
        val locus: Locus,
        val phase: Double,
        val structuralWeight: Double
    )

    fun placements(field: CommitmentField): List<Placement> =
        field.all()
            .mapIndexed { index, commitment ->
                val phase = GoldenRatioExperiment.goldenAnglePhase(
                    index + stableOffset(commitment.locus)
                )
                Placement(
                    locus = commitment.locus,
                    phase = phase,
                    structuralWeight = GoldenRatioExperiment.PHI.pow(
                        -index.toDouble() / GoldenRatioExperiment.PHI
                    )
                )
            }
            .sortedWith(compareByDescending<Placement> { it.structuralWeight }.thenBy { it.phase })

    fun orderedActivation(
        field: CommitmentField,
        minForce: Float = 0.25f,
        maxCount: Int = 64
    ): List<ResidualCommitment> {
        require(maxCount >= 0) { "maxCount must not be negative" }
        val allowed = field.all()
            .filter { it.residualForce >= minForce }
            .associateBy { it.locus }

        return placements(field)
            .mapNotNull { allowed[it.locus] }
            .take(maxCount)
    }

    private fun stableOffset(locus: Locus): Int =
        abs(locus.hashCode()) % 10_000
}

private fun Double.pow(exponent: Double): Double =
    kotlin.math.exp(kotlin.math.ln(this) * exponent)
