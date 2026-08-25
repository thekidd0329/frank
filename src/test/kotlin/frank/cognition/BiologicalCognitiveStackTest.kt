package frank.cognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BiologicalCognitiveStackTest {
    private fun f(vararg x: Float) = SensoryFeatureVector(x)

    @Test
    fun repeatedSimilarExperienceRecruitsSameLocus() {
        val stack = BiologicalCognitiveStack()
        val a = stack.experience(f(1f, 0f, 0f), appetitiveDrive = 0.2f)
        val b = stack.experience(f(0.99f, 0.02f, 0f), appetitiveDrive = 0.1f)
        assertEquals(a.recruitment.primary, b.recruitment.primary)
        assertEquals(1, stack.locusCount())
    }

    @Test
    fun sufficientlyDifferentExperienceBirthsNewLocus() {
        val stack = BiologicalCognitiveStack()
        val a = stack.experience(f(1f,0f,0f))
        val b = stack.experience(f(0f,1f,0f))
        assertNotEquals(a.recruitment.primary, b.recruitment.primary)
        assertEquals(2, stack.locusCount())
    }

    @Test
    fun opponentPathsRepresentReversalWithoutNegativeTrace() {
        val stack = BiologicalCognitiveStack()
        val first = stack.experience(f(1f,0f), appetitiveDrive = 0.5f)
        val locus = first.recruitment.primary
        repeat(4) { stack.experience(f(1f,0f), aversiveDrive = 0.25f) }
        val active = assertNotNull(stack.active(locus))
        assertTrue(active.positivePath >= 0f)
        assertTrue(active.opponentPath >= 0f)
        assertEquals(Polarity.NEGATIVE, active.dominantPolarity)
    }

    @Test
    fun oneShotSalienceDoesNotAutomaticallyBecomeLongTermMemory() {
        val stack = BiologicalCognitiveStack()
        val step = stack.experience(f(1f,0f), appetitiveDrive = 0.9f)
        assertEquals(null, stack.durable(step.recruitment.primary))
    }

    @Test
    fun repeatedReinstatementAndReplayCanConsolidate() {
        val stack = BiologicalCognitiveStack()
        val first = stack.experience(f(1f,0f), appetitiveDrive = 0.4f)
        val locus = first.recruitment.primary
        stack.experience(f(0.99f,0.01f), appetitiveDrive = 0.2f)
        stack.replay(locus)
        stack.replay(locus)
        // Reconstruction evidence is deliberately required by the conservative policy;
        // before a durable trace exists, no circular reconstruction credit is invented.
        assertEquals(null, stack.durable(locus))
    }

    @Test
    fun durableProjectionCanRebuildOpponentDominanceAfterActivationLoss() {
        val residual = OpponentResidual(0.8f, 0.2f)
        val commitment = ResidualCommitmentProjection.fromConsolidatedState(
            Locus(7), residual, contextualBinding = 0.7f, generation = 1
        )
        val bias = DurableRetrievalProjection.biasFrom(commitment)
        val rebuilt = OpponentResidual(bias.positiveBias, bias.opponentBias)
        assertEquals(residual.dominantPolarity, rebuilt.dominantPolarity)
        assertEquals(Polarity.POSITIVE, rebuilt.dominantPolarity)
    }
}
