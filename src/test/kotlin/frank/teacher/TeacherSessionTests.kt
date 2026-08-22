package frank.teacher

import frank.cognition.Polarity
import java.nio.file.Files

object TeacherSessionTests {
    @JvmStatic
    fun main(args: Array<String>) {
        teachingReconstructsIntoDevelopmentalBrain()
        selfQuestionComesFromOwnUnresolvedExperience()
        teachingLoadHonorsEndogenousSleep()
        println("TeacherSessionTests PASS")
    }

    private fun teachingReconstructsIntoDevelopmentalBrain() {
        val dir = Files.createTempDirectory("frank-teacher-test")
        val path = dir.resolve("teaching.log")

        val first = TeacherSession(TeachingJournal(path))
        first.stage("warm hand")
        val positiveLocus = first.reinforce(0.75f)
        first.stage("sharp noise")
        val negativeLocus = first.contradict(0.50f)
        first.recover(0.25f)

        check(first.residualField()[positiveLocus] == 0.75f)
        check(first.residualField()[negativeLocus] == -0.50f)
        check(first.eventCount() == 3)

        val brainPositive = requireNotNull(first.brainCommitment(positiveLocus))
        val brainNegative = requireNotNull(first.brainCommitment(negativeLocus))
        check(brainPositive.polarity == Polarity.POSITIVE)
        check(brainNegative.polarity == Polarity.NEGATIVE)
        check(brainPositive.residualForce > 0.70f)
        check(brainNegative.residualForce > 0.45f)

        val expectedField = first.residualField()
        val expectedState = first.state()
        val expectedBrain = first.brainSnapshot()
        val expectedSleep = first.sleepStatus()

        val reconstructed = TeacherSession(TeachingJournal(path))
        check(reconstructed.residualField() == expectedField) {
            "replayed teaching journal must reconstruct the same newborn residual field"
        }
        check(reconstructed.state() == expectedState) {
            "replayed teaching journal must reconstruct the same newborn state"
        }
        check(reconstructed.brainSnapshot() == expectedBrain) {
            "replayed teaching journal must reconstruct the same developmental cognition field"
        }
        check(reconstructed.sleepStatus() == expectedSleep) {
            "sleep/homeostatic state must reconstruct from the same teaching history"
        }
        check(reconstructed.eventCount() == 3)
        check(!reconstructed.hasPendingObservation())

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
    }

    private fun selfQuestionComesFromOwnUnresolvedExperience() {
        val dir = Files.createTempDirectory("frank-teacher-question-test")
        val path = dir.resolve("teaching.log")
        val session = TeacherSession(TeachingJournal(path))

        session.stage("warm hand")
        val knownLocus = session.reinforce(0.90f)

        session.stage("flickering shape")
        val unresolvedLocus = session.commit()

        val gaps = session.learningGaps()
        check(gaps.isNotEmpty())
        check(gaps.first().gap.locus.raw == unresolvedLocus) {
            "the most unresolved actually observed experience should rank first"
        }
        check(gaps.first().gap.pressure > gaps.firstOrNull { it.gap.locus.raw == knownLocus }!!.gap.pressure)

        val question = requireNotNull(session.nextQuestionIntent())
        check(question.gap.locus.raw == unresolvedLocus) {
            "Frank must choose the question subject from his own unresolved field"
        }
        check(question.sourcePreview == "flickering shape") {
            "preview may expose the originating experience to the teacher UI"
        }

        val reconstructed = TeacherSession(TeachingJournal(path))
        check(reconstructed.learningGaps() == gaps) {
            "learning gaps must reconstruct from persisted experience and cognitive state"
        }
        check(reconstructed.nextQuestionIntent() == question) {
            "Frank's next question intent must survive restart without a scripted question list"
        }

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
    }

    private fun teachingLoadHonorsEndogenousSleep() {
        val dir = Files.createTempDirectory("frank-teacher-sleep-test")
        val path = dir.resolve("teaching.log")
        val session = TeacherSession(TeachingJournal(path))

        var observations = 0
        while (!session.isAsleep() && observations < 25) {
            session.stage("novel experience $observations")
            session.commit()
            observations++
        }

        check(session.isAsleep()) { "teaching load should eventually trigger endogenous sleep" }
        check(!session.wantsWake()) { "Frank must not want to wake while homeostatic recovery is incomplete" }
        check(session.nextQuestionIntent() == null) {
            "Frank must not emit a waking question while he knows he is asleep"
        }

        session.stage("experience presented while asleep")
        val blocked = runCatching { session.commit() }.isFailure
        check(blocked) { "ordinary teaching must not be processed while Frank is asleep" }
        check(session.hasPendingObservation()) { "blocked input may remain staged until natural wake" }

        session.recover(1.0f)
        check(!session.isAsleep()) { "recovery harness should advance sleep until the internal wake threshold is reached" }
        check(session.wantsWake()) { "once rested, the internal state should permit wake" }
        check(session.nextQuestionIntent() != null) {
            "once awake, unresolved observed experience may again produce a question intent"
        }

        // The previously staged experience can now be learned without restaging it.
        session.reinforce(0.60f)
        check(!session.hasPendingObservation())

        val expectedBrain = session.brainSnapshot()
        val expectedSleep = session.sleepStatus()
        val reconstructed = TeacherSession(TeachingJournal(path))
        check(reconstructed.brainSnapshot() == expectedBrain)
        check(reconstructed.sleepStatus() == expectedSleep)

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
    }
}
