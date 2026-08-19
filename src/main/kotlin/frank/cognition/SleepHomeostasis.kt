package frank.cognition

/**
 * Endogenous sleep pressure and self-state.
 *
 * Sleep is entered because accumulated internal pressure crosses a threshold.
 * While asleep, ordinary wake preference stays false until homeostasis is restored.
 * The sleep pressure and sleeping latch themselves live in the CommitmentField so a
 * reconstructed engine can know that it is asleep from ground state alone.
 */
enum class CognitiveState { AWAKE, NREM, REM }

data class SleepStatus(
    val state: CognitiveState,
    val sleepPressure: Float,
    val isAsleep: Boolean,
    val wantsWake: Boolean
)

data class SleepTransition(
    val before: SleepStatus,
    val after: SleepStatus,
    val fellAsleep: Boolean,
    val woke: Boolean
)

class SleepHomeostasis(
    private val field: CommitmentField,
    private val sleepOnsetThreshold: Float = 0.80f,
    private val remTransitionThreshold: Float = 0.45f,
    private val wakeThreshold: Float = 0.15f,
    private val baseAwakePressure: Float = 0.045f,
    private val loadPressureWeight: Float = 0.055f,
    private val sleepRecoveryPerStep: Float = 0.080f
) {
    private object Loci {
        private const val NAMESPACE: Int = 0x534C4550 // "SLEP"
        val PRESSURE = Locus.fromParts(NAMESPACE, 1)
        val ASLEEP = Locus.fromParts(NAMESPACE, 2)
    }

    fun status(): SleepStatus {
        val pressure = field.get(Loci.PRESSURE)?.residualForce ?: 0.0f
        val asleep = (field.get(Loci.ASLEEP)?.residualForce ?: 0.0f) >= 0.5f
        val state = when {
            !asleep -> CognitiveState.AWAKE
            pressure > remTransitionThreshold -> CognitiveState.NREM
            else -> CognitiveState.REM
        }
        return SleepStatus(
            state = state,
            sleepPressure = pressure,
            isAsleep = asleep,
            wantsWake = !asleep
        )
    }

    fun knowsItIsAsleep(): Boolean = status().isAsleep

    fun awakeStep(cognitiveLoad: Float = 0.5f): SleepTransition {
        require(cognitiveLoad in 0.0f..1.0f)
        val before = status()
        if (before.isAsleep) return SleepTransition(before, before, fellAsleep = false, woke = false)

        val increase = baseAwakePressure + loadPressureWeight * cognitiveLoad
        val nextPressure = (before.sleepPressure + increase).coerceIn(0.0f, 1.0f)
        setPressure(nextPressure)
        if (nextPressure >= sleepOnsetThreshold) setAsleep(true)

        val after = status()
        return SleepTransition(before, after, fellAsleep = !before.isAsleep && after.isAsleep, woke = false)
    }

    fun sleepStep(): SleepTransition {
        val before = status()
        if (!before.isAsleep) return SleepTransition(before, before, fellAsleep = false, woke = false)

        val nextPressure = (before.sleepPressure - sleepRecoveryPerStep).coerceAtLeast(0.0f)
        setPressure(nextPressure)
        if (nextPressure <= wakeThreshold) setAsleep(false)

        val after = status()
        return SleepTransition(before, after, fellAsleep = false, woke = before.isAsleep && !after.isAsleep)
    }

    private fun setPressure(force: Float) {
        field.put(
            ResidualCommitment(
                locus = Loci.PRESSURE,
                polarity = Polarity.POSITIVE,
                residualForce = force.coerceIn(0.0f, 1.0f),
                contextualBinding = 1.0f,
                temporalPersistence = TemporalAnchor(generation = 0L),
                consolidationMaturity = 1.0f,
                flags = CommitmentFlags.HOMEOSTATIC
            )
        )
    }

    private fun setAsleep(asleep: Boolean) {
        if (!asleep) {
            field.remove(Loci.ASLEEP)
            return
        }
        field.put(
            ResidualCommitment(
                locus = Loci.ASLEEP,
                polarity = Polarity.POSITIVE,
                residualForce = 1.0f,
                contextualBinding = 1.0f,
                temporalPersistence = TemporalAnchor(generation = 0L),
                consolidationMaturity = 1.0f,
                flags = CommitmentFlags.HOMEOSTATIC
            )
        )
    }
}
