package frank.model

import frank.cognition.Locus
import frank.cognition.Polarity
import frank.cognition.ResidualCommitment

/**
 * Replaceable local inference organ.
 *
 * The model may verbalize a cognition decision; it does not choose the decision,
 * mutate memory, authorize actions, or become a second cognitive authority.
 */
interface LocalModelPort {
    fun verbalizeQuestion(request: QuestionProjectionRequest): String
}

data class QuestionProjectionRequest(
    /** Subject already selected by frank-brain. The model cannot replace it. */
    val targetLocus: Locus,

    /** Optional raw/teacher-facing context that helps a language model phrase the question. */
    val sourcePreview: String?,

    /** Bounded projection of relevant cognitive state, never writable by the model. */
    val context: List<ModelContextAtom>
)

data class ModelContextAtom(
    val locus: Locus,
    val polarity: Polarity,
    val residualForce: Float,
    val consolidationMaturity: Float,
    val contested: Boolean
) {
    init {
        require(residualForce in 0f..1f)
        require(consolidationMaturity in 0f..1f)
    }

    companion object {
        fun from(commitment: ResidualCommitment): ModelContextAtom = ModelContextAtom(
            locus = commitment.locus,
            polarity = commitment.polarity,
            residualForce = commitment.residualForce,
            consolidationMaturity = commitment.consolidationMaturity,
            contested = commitment.flags.has(frank.cognition.CommitmentFlags.CONTESTED)
        )
    }
}

data class VerbalizedQuestion(
    /** Preserved from the brain-selected intent, not returned by the model. */
    val targetLocus: Locus,
    val text: String
) {
    init {
        require(text.isNotBlank()) { "model question text cannot be blank" }
    }
}
