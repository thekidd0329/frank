package frank.cognition

/** Measures how often field updates cause significant single-step residual degradation. */
class DegradationMonitor(
    private val significantDelta: Float = 0.08f
) {
    data class TickSample(
        val tickIndex: Long,
        val liveCount: Int,
        val meanForce: Float,
        val significantDrops: Int,
        val maxDrop: Float
    )

    private val samples = mutableListOf<TickSample>()
    private var lastSnapshot: Map<Locus, Float> = emptyMap()
    private var tickCounter = 0L

    fun onFieldState(field: CommitmentField) {
        val current = field.snapshot()
            .filterNot { it.flags.has(CommitmentFlags.HOMEOSTATIC) }
            .associate { it.locus to it.residualForce }
        var significantDrops = 0
        var maxDrop = 0f

        if (lastSnapshot.isNotEmpty()) {
            current.forEach { (locus, force) ->
                val prev = lastSnapshot[locus]
                if (prev != null) {
                    val drop = prev - force
                    if (drop > maxDrop) maxDrop = drop
                    if (drop > significantDelta) significantDrops++
                }
            }
            lastSnapshot.keys.forEach { locus ->
                if (!current.containsKey(locus)) {
                    val prev = lastSnapshot[locus] ?: 0f
                    if (prev > significantDelta) significantDrops++
                    if (prev > maxDrop) maxDrop = prev
                }
            }
        }

        val mean = if (current.isEmpty()) 0f else current.values.average().toFloat()
        samples += TickSample(
            tickIndex = tickCounter++,
            liveCount = current.size,
            meanForce = mean,
            significantDrops = significantDrops,
            maxDrop = maxDrop
        )
        lastSnapshot = current
    }

    fun rateOfSignificantTicks(): Float =
        if (samples.isEmpty()) 0f else samples.count { it.significantDrops > 0 }.toFloat() / samples.size

    fun summary(): String {
        if (samples.isEmpty()) return "No samples yet."
        val ticksWithSignificant = samples.count { it.significantDrops > 0 }
        return "ticks=${samples.size} significantTicks=$ticksWithSignificant rate=${"%.3f".format(rateOfSignificantTicks())}"
    }
}
