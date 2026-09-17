package lol.vynnra.agent.core.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
            PermissionState(Capability.FILE_READ, PermissionStatus.REQUIRES_USER_ACTION, "File Access", "Uses Android document APIs and user-selected files."),
            PermissionState(Capability.FILE_WRITE, PermissionStatus.REQUIRES_USER_ACTION, "File Write", "Uses Android document APIs for user-authorized destinations."),
            PermissionState(Capability.BROWSER_CONTROL, PermissionStatus.REQUIRES_SETTINGS, "Browser Control", "Requires supported Accessibility/browser integration."),
            PermissionState(Capability.WEB_SEARCH, PermissionStatus.NOT_GRANTED, "Web Search", "Uses the configured web-search provider."),
            PermissionState(Capability.MICROPHONE, PermissionStatus.REQUIRES_USER_ACTION, "Microphone", "Required only for voice input.", dangerous = true),
            PermissionState(Capability.NOTIFICATION_ACCESS, PermissionStatus.REQUIRES_SETTINGS, "Notification Access", "Allows reading supported notifications.", dangerous = true),
            PermissionState(Capability.OVERLAY, PermissionStatus.REQUIRES_SETTINGS, "Floating Assistant", "Allows Vynnra's floating assistant surface.", dangerous = true),
            PermissionState(Capability.BACKGROUND_EXECUTION, PermissionStatus.REQUIRES_USER_ACTION, "Background Execution", "Requires an appropriate foreground/background execution strategy.", dangerous = true),
        )
    }

    override fun openRequiredSettings(context: Context, capability: Capability): Boolean {
        val intent = when (capability) {
            Capability.ACCESSIBILITY_CONTROL, Capability.SCREEN_INTERACT, Capability.BROWSER_CONTROL ->
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            Capability.NOTIFICATION_ACCESS ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            Capability.OVERLAY ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            else -> return false
        }
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }
}
