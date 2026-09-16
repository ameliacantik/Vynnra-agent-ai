package lol.vynnra.agent.core.security

import lol.vynnra.agent.core.tool.Capability

class CapabilityGate(
    private val grantedCapabilities: () -> Set<Capability>
) {
    fun check(required: Set<Capability>): GateResult {
        val missing = required - grantedCapabilities()
        return if (missing.isEmpty()) {
            GateResult.Allowed
        } else {
            GateResult.Blocked(missing)
        }
    }
}

sealed interface GateResult {
    data object Allowed : GateResult
    data class Blocked(val missing: Set<Capability>) : GateResult
}
