package frank.cognition

import kotlin.math.abs

object PhiMemoryLearningTests {
    @JvmStatic
    fun main(args: Array<String>) {
        inversePhiPowersAreCanonical()
        repeatedPairingCreatesRecall()
        passiveAgingCannotInvertResidualPolarity()
        phiAndControlProduceDifferentRetention()
        println("PhiMemoryLearningTests: PASS")
    }

    private fun inversePhiPowersAreCanonical() {
        check(abs(GoldenRatioExperiment.inversePower(1) - 0.6180339887498948) < 1e-12)
        check(abs(GoldenRatioExperiment.inversePower(2) - 0.38196601125010515) < 1e-12)
    }

    private fun repeatedPairingCreatesRecall() {
        val loop = NewbornLearningLoop(memoryProfile = MemoryDynamicsProfile.PHI)
        val cue = "round-red-pattern".encodeToByteArray()
        val symbol = "ba".encodeToByteArray()

        repeat(4) {
            loop.observe(cue, DevelopmentalSignal(novelty = if (it == 0) 1f else 0.2f, familiarity = if (it == 0) 0f else 0.8f))
            loop.observe(symbol, DevelopmentalSignal(novelty = if (it == 0) 1f else 0.2f, familiarity = if (it == 0) 0f else 0.8f))
            loop.associate(cue, symbol)
        }

        val expectedSymbolLocus = loop.locusOf(symbol)
        check(loop.recall(cue).any { it.locus == expectedSymbolLocus }) {
            "repeated co-occurrence should make the paired opaque locus retrievable"
        }
    }

    private fun passiveAgingCannotInvertResidualPolarity() {
        val loop = NewbornLearningLoop(memoryProfile = MemoryDynamicsProfile.PHI)
        val raw = "safe-pattern".encodeToByteArray()
        val locus = loop.observe(raw, DevelopmentalSignal(reward = 1f))
        repeat(4) {
            loop.ageMemory()
            val value = loop.residualField[locus]
            if (value != null) check(value > 0f) {
                "passive aging must weaken toward zero without flipping polarity"
            }
        }
    }

    private fun phiAndControlProduceDifferentRetention() {
        val results = PhiMemoryProbe.compare(exposures = 4)
        val control = results.single { it.profileName == "control" }
        val phi = results.single { it.profileName == "phi" }

        check(control.initialAssociationStrength > 0f)
        check(phi.initialAssociationStrength > 0f)
        check(control.retrievableAgingSteps > phi.retrievableAgingSteps) {
            "the current phi hypothesis should be observably different from the control, not silently equivalent"
        }
    }
}
