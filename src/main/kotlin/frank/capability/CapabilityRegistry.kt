package frank.capability

enum class CapabilitySideEffect { NONE, EXTERNAL }

data class Capability(
    val id: String,
    val provider: String,
    val observableAxes: Set<String> = emptySet(),
    val resolvesEntities: Set<String> = emptySet(),
    val reducesUncertaintyOn: Set<String> = emptySet(),
    val requiredScopes: Set<String> = emptySet(),
    val costScore: Float = 1f,
    val latencyScore: Float = 1f,
    val riskScore: Float = 0f,
    val sideEffect: CapabilitySideEffect = CapabilitySideEffect.NONE,
    val available: Boolean = true
)

class CapabilityRegistry {
    private val capabilities = linkedMapOf<String, Capability>()

    fun register(capability: Capability) {
        capabilities[capability.id] = capability
    }

    fun unregister(id: String) {
        capabilities.remove(id)
    }

    fun get(id: String): Capability? = capabilities[id]

    fun candidatesFor(missingUncertainty: Set<String>): List<Capability> =
        capabilities.values
            .asSequence()
            .filter { it.available }
            .filter { it.reducesUncertaintyOn.any(missingUncertainty::contains) }
            .sortedByDescending { informationValue(it, missingUncertainty) }
            .toList()

    fun informationValue(capability: Capability, missingUncertainty: Set<String>): Float {
        val hits = capability.reducesUncertaintyOn.count(missingUncertainty::contains).toFloat()
        if (hits == 0f) return 0f
        val cost = capability.costScore.coerceAtLeast(0.05f)
        val latency = capability.latencyScore.coerceAtLeast(0.05f)
        return hits / (cost * latency * (1f + capability.riskScore.coerceAtLeast(0f)))
    }
}
