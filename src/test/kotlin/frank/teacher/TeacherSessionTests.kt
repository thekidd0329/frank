package frank.teacher

import frank.cognition.MemoryDynamicsProfile
import java.nio.file.Files

object TeacherSessionTests {
    @JvmStatic
    fun main(args: Array<String>) {
        val dir = Files.createTempDirectory("frank-teacher-test")
        val path = dir.resolve("teaching.log")

        val first = TeacherSession(TeachingJournal(path), MemoryDynamicsProfile.PHI)
        first.stage("warm hand")
        val positiveLocus = first.reinforce(0.75f)
        first.stage("sharp noise")
        val negativeLocus = first.contradict(0.50f)
        first.recover(0.25f)

        repeat(4) {
            first.associate("round-red-pattern", "ba")
        }
        val symbolLocus = first.locusOf("ba")
        check(first.recall("round-red-pattern").any { it.locus == symbolLocus }) {
            "repeated pairing should be retrievable before restart"
        }

        first.ageMemory(1)

        check(first.residualField()[positiveLocus] != null)
        check(first.residualField()[positiveLocus]!! > 0f)
        check(first.residualField()[negativeLocus] != null)
        check(first.residualField()[negativeLocus]!! < 0f)
        check(first.eventCount() == 8)

        val expectedField = first.residualField()
        val expectedAssociations = first.associationField()
        val expectedState = first.state()

        val reconstructed = TeacherSession(TeachingJournal(path), MemoryDynamicsProfile.PHI)
        check(reconstructed.residualField() == expectedField) {
            "replayed teaching journal must reconstruct the same residual field"
        }
        check(reconstructed.associationField() == expectedAssociations) {
            "replayed teaching journal must reconstruct learned associations"
        }
        check(reconstructed.recall("round-red-pattern").any { it.locus == symbolLocus }) {
            "learned association must remain retrievable after restart"
        }
        check(reconstructed.state() == expectedState) {
            "replayed teaching journal must reconstruct the same newborn state"
        }
        check(reconstructed.eventCount() == 8)
        check(!reconstructed.hasPendingObservation())

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
        println("TeacherSessionTests PASS")
    }
}
