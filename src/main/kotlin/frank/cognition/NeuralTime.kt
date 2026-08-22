package frank.cognition

/**
 * Canonical simulated neural time.
 *
 * Hardware speed controls how quickly the simulation completes. It must not change
 * how much developmental time has elapsed inside the model.
 */
object NeuralTime {
    /** One canonical integration tick represents one millisecond of neural time. */
    const val DEFAULT_TICK_SECONDS: Float = 0.001f

    const val MILLIS_PER_SECOND: Float = 1000f

    fun milliseconds(value: Float): Float {
        require(value >= 0f)
        return value / MILLIS_PER_SECOND
    }

    fun requireDuration(seconds: Float) {
        require(seconds >= 0f) { "neural time cannot move backward" }
    }
}
