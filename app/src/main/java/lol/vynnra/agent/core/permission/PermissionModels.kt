package lol.vynnra.agent.core.permission

import lol.vynnra.agent.core.tool.Capability

enum class PermissionStatus {
    GRANTED,
    NOT_GRANTED,
    REQUIRES_SETTINGS,
    REQUIRES_USER_ACTION,
    UNAVAILABLE
}

data class PermissionState(
    val capability: Capability,
    val status: PermissionStatus,
    val label: String,
    val description: String,
    val dangerous: Boolean = false
)

data class PermissionGroup(
    val id: String,
    val title: String,
    val description: String,
    val permissions: List<PermissionState>
)
