package frank.teacher

import frank.model.LocalModelPort
import frank.model.ModelContextAtom
import frank.model.QuestionProjectionRequest
import frank.model.VerbalizedQuestion

/**
 * Language is a projection over cognition, not cognition itself.
 *
 * Frank's brain chooses the target locus first. The replaceable local model is
 * given that fixed target and bounded context only to turn the information need
 * into human-readable language.
 */
class LearningQuestionProjector(
    private val model: LocalModelPort
) {
    fun project(session: TeacherSession): VerbalizedQuestion? {
        val intent = session.nextQuestionIntent() ?: return null
        val context = session.brainSnapshot()
            .sortedByDescending { it.residualForce }
            .take(32)
            .map(ModelContextAtom::from)

        val text = model.verbalizeQuestion(
            QuestionProjectionRequest(
                targetLocus = intent.gap.locus,
                sourcePreview = intent.sourcePreview,
                context = context
            )
        ).trim()

        return VerbalizedQuestion(
            targetLocus = intent.gap.locus,
            text = text
        )
    }
}
