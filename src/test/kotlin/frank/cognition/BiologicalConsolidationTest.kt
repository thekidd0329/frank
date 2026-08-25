package frank.cognition

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiologicalConsolidationTest {
    @Test
    fun partialCuePatternCompletionCanEarnConsolidationBeforeLtmExists() {
        val c = BiologicalConsolidator()
        val locus = Locus(99)
        val residual = OpponentResidual(0.8f, 0.2f)
        c.tag(locus, residual, 1)
        c.noteCompetitionSurvival(locus)
        c.noteContextReinstatement(locus)
        assertFalse(c.eligible(locus, residual))
        c.replayWithPartialCue(locus, OpponentResidual(0.55f,0.2f), 0.45f)
        c.replayWithPartialCue(locus, OpponentResidual(0.50f,0.2f), 0.40f)
        assertTrue(c.eligible(locus, residual))
    }

    @Test
    fun wrongPolarityReplayDoesNotEarnReconstructionCredit() {
        val c = BiologicalConsolidator()
        val locus = Locus(100)
        c.tag(locus, OpponentResidual(0.8f,0.2f), 1)
        c.noteCompetitionSurvival(locus)
        c.noteContextReinstatement(locus)
        repeat(3) { c.replayWithPartialCue(locus, OpponentResidual(0.1f,0.7f), 0.4f) }
        assertFalse(c.eligible(locus, OpponentResidual(0.8f,0.2f)))
    }
}
