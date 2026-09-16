package lol.vynnra.agent.core.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentStatus {
    IDLE, UNDERSTANDING, PLANNING, EXECUTING, VERIFYING,
    RECOVERING, COMPLETED, FAILED, CANCELLED, BLOCKED
}

enum class ThinkingLevel { LOW, MEDIUM, HIGH, MAX }

data class AgentState(
    val status: AgentStatus = AgentStatus.IDLE,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.MAX,
    val currentActivity: String? = null,
    val result: String? = null,
    val error: String? = null
)

class AgentStateStore {
    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state.asStateFlow()

    fun update(transform: (AgentState) -> AgentState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = AgentState()
    }
}
