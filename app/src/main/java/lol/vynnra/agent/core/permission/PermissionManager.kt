package lol.vynnra.agent.core.permission

import android.content.Context
import lol.vynnra.agent.core.tool.Capability

interface PermissionManager {
    fun snapshot(): List<PermissionState>
    fun refresh()
    fun openRequiredSettings(context: Context, capability: Capability): Boolean
}

class DefaultPermissionManager : PermissionManager {
    private var states: List<PermissionState> = emptyList()

    override fun snapshot(): List<PermissionState> = states

    override fun refresh() {
        states = listOf(
            PermissionState(Capability.ACCESSIBILITY_CONTROL, PermissionStatus.REQUIRES_SETTINGS, "Accessibility Control", "Allows Vynnra to inspect and interact with supported UI surfaces.", dangerous = true),
            PermissionState(Capability.SCREEN_READ, PermissionStatus.REQUIRES_USER_ACTION, "Screen Capture", "Requires MediaProjection consent before screen capture.", dangerous = true),
            PermissionState(Capability.SCREEN_INTERACT, PermissionStatus.REQUIRES_SETTINGS, "Screen Interaction", "Uses granted Accessibility capability for gestures and global actions.", dangerous = true),
            PermissionState(Capability.FILE_READ, PermissionStatus.GRANTED, "File Access", "Uses Android storage/document APIs and user-selected files."),
            PermissionState(Capability.FILE_WRITE, PermissionStatus.GRANTED, "File Write", "Uses Android storage/document APIs for user-authorized destinations."),
            PermissionState(Capability.BROWSER_CONTROL, PermissionStatus.NOT_GRANTED, "Browser Control", "Requires supported Accessibility/browser integration."),
            PermissionState(Capability.WEB_SEARCH, PermissionStatus.NOT_GRANTED, "Web Search", "Uses the configured web-search provider."),
            PermissionState(Capability.MICROPHONE, PermissionStatus.NOT_GRANTED, "Microphone", "Required only for voice input.", dangerous = true),
            PermissionState(Capability.NOTIFICATION_ACCESS, PermissionStatus.REQUIRES_SETTINGS, "Notification Access", "Allows reading supported notifications.", dangerous = true),
            PermissionState(Capability.OVERLAY, PermissionStatus.REQUIRES_SETTINGS, "Floating Assistant", "Allows Vynnra's floating assistant surface.", dangerous = true),
            PermissionState(Capability.BACKGROUND_EXECUTION, PermissionStatus.REQUIRES_USER_ACTION, "Background Execution", "Requires an appropriate foreground/background execution strategy.", dangerous = true),
        )
    }

    override fun openRequiredSettings(context: Context, capability: Capability): Boolean = false
}
