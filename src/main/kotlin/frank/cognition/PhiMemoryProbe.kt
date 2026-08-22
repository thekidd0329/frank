package frank.cognition

/**
 * Deterministic probe for comparing memory laws under identical experience.
 * This does not declare a winner; it exposes measurable consequences.
 */
object PhiMemoryProbe {
    data class Result(
        val profileName: String,
        val exposureCount: Int,
        val initialAssociationStrength: Float,
        val retrievableAgingSteps: Int,
        val finalAssociationStrength: Float
    )

    fun compare(exposures: Int = 4, maxAgingSteps: Int = 64): List<Result> {
        require(exposures > 0)
        require(maxAgingSteps > 0)
        return listOf(
            run("control", MemoryDynamicsProfile.CONTROL, exposures, maxAgingSteps),
            run("phi", MemoryDynamicsProfile.PHI, exposures, maxAgingSteps)
        )
    }

    private fun run(
        name: String,
        profile: MemoryDynamicsProfile,
        exposures: Int,
        maxAgingSteps: Int
    ): Result {
        val field = DevelopmentalAssociationField(profile)
        val cue = 101L
        val paired = 202L

        repeat(exposures) { field.associate(cue, paired) }
        val initial = field.strength(cue, paired)

        var retrievableSteps = 0
        while (retrievableSteps < maxAgingSteps && field.recall(cue).isNotEmpty()) {
            field.decay()
            retrievableSteps += 1
        }

        return Result(
            profileName = name,
            exposureCount = exposures,
            initialAssociationStrength = initial,
            retrievableAgingSteps = retrievableSteps,
            finalAssociationStrength = field.strength(cue, paired)
        )
    }
}

fun main() {
    PhiMemoryProbe.compare().forEach { result ->
        println(
            "${result.profileName}\t" +
                "exposures=${result.exposureCount}\t" +
                "initial=${result.initialAssociationStrength}\t" +
                "retrievableSteps=${result.retrievableAgingSteps}\t" +
                "final=${result.finalAssociationStrength}"
        )
    }
}
