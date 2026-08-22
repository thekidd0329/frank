package frank.teacher

import frank.model.LocalModelPort
import frank.model.QuestionProjectionRequest
import java.nio.file.Files

object LearningQuestionProjectorTests {
    @JvmStatic
    fun main(args: Array<String>) {
        modelVerbalizesButCannotChooseQuestionSubject()
        sleepingBrainEmitsNoModelQuestion()
        println("LearningQuestionProjectorTests PASS")
    }

    private fun modelVerbalizesButCannotChooseQuestionSubject() {
        val dir = Files.createTempDirectory("frank-question-projector-test")
        val path = dir.resolve("teaching.log")
        val session = TeacherSession(TeachingJournal(path))

        session.stage("known warm object")
        session.reinforce(0.95f)
        session.stage("unresolved flickering shape")
        val unresolvedLocus = session.commit()

        var received: QuestionProjectionRequest? = null
        val model = object : LocalModelPort {
            override fun verbalizeQuestion(request: QuestionProjectionRequest): String {
                received = request
                return "What should I understand about the flickering shape?"
            }
        }

        val projected = requireNotNull(LearningQuestionProjector(model).project(session))
        val request = requireNotNull(received)

        check(request.targetLocus.raw == unresolvedLocus) {
            "frank-brain must select the target before the model is called"
        }
        check(projected.targetLocus == request.targetLocus) {
            "model output must not be able to replace the brain-selected target"
        }
        check(projected.text == "What should I understand about the flickering shape?")
        check(request.context.isNotEmpty())

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
    }

    private fun sleepingBrainEmitsNoModelQuestion() {
        val dir = Files.createTempDirectory("frank-question-projector-sleep-test")
        val path = dir.resolve("teaching.log")
        val session = TeacherSession(TeachingJournal(path))

        var index = 0
        while (!session.isAsleep() && index < 25) {
            session.stage("novel sleep-driving experience $index")
            session.commit()
            index++
        }
        check(session.isAsleep())

        var calls = 0
        val model = object : LocalModelPort {
            override fun verbalizeQuestion(request: QuestionProjectionRequest): String {
                calls++
                return "This should never be emitted while asleep"
            }
        }

        check(LearningQuestionProjector(model).project(session) == null)
        check(calls == 0) { "local model must not be invoked for a waking question while Frank is asleep" }

        Files.deleteIfExists(path)
        Files.deleteIfExists(dir)
    }
}
