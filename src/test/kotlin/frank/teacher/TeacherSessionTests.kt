package frank.teacher

import java.nio.file.Files

object TeacherSessionTests {
    @JvmStatic
    fun main(args: Array<String>) {
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

        val expectedField = first.residualField()
        val expectedState = first.state()

        val reconstructed = TeacherSession(TeachingJournal(path))
        check(reconstructed.residualField() == expectedField) {
            "replayed teaching journal must reconstruct the same residual field"
        }
        check(reconstructed.state() == expectedState) {
            "replayed teaching journal must reconstruct the same newborn state"
        }
        check(reconstructed.eventCount() == 3)
        check(!reconstructed.hasPendingObservation())

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
        println("TeacherSessionTests PASS")
    }
}
