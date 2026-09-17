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

class DefaultPermissionManager(private val context: Context) : PermissionManager {
    private var states: List<PermissionState> = emptyList()

    override fun snapshot(): List<PermissionState> = states

    override fun refresh() {
        val accessibilityGranted = isAccessibilityGranted()
        val overlayGranted = Settings.canDrawOverlays(context)
        val microphoneGranted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val notificationsGranted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED

        states = listOf(
            PermissionState(Capability.ACCESSIBILITY_CONTROL, if (accessibilityGranted) PermissionStatus.GRANTED else PermissionStatus.REQUIRES_SETTINGS, "Accessibility Control", "Allows Vynnra to inspect and interact with supported UI surfaces.", dangerous = true),
            PermissionState(Capability.SCREEN_READ, PermissionStatus.REQUIRES_USER_ACTION, "Screen Capture", "Requires MediaProjection consent before screen capture.", dangerous = true),
            PermissionState(Capability.SCREEN_INTERACT, if (accessibilityGranted) PermissionStatus.GRANTED else PermissionStatus.REQUIRES_SETTINGS, "Screen Interaction", "Uses the Accessibility service for gestures and global actions.", dangerous = true),
            PermissionState(Capability.FILE_READ, PermissionStatus.REQUIRES_USER_ACTION, "File Access", "Uses Android document APIs and user-selected files."),
            PermissionState(Capability.FILE_WRITE, PermissionStatus.REQUIRES_USER_ACTION, "File Write", "Uses Android document APIs for user-authorized destinations."),
            PermissionState(Capability.BROWSER_CONTROL, if (accessibilityGranted) PermissionStatus.GRANTED else PermissionStatus.REQUIRES_SETTINGS, "Browser Control", "Uses the supported Accessibility integration."),
            PermissionState(Capability.WEB_SEARCH, PermissionStatus.NOT_GRANTED, "Web Search", "Uses the configured web-search provider."),
            PermissionState(Capability.MICROPHONE, if (microphoneGranted) PermissionStatus.GRANTED else PermissionStatus.REQUIRES_USER_ACTION, "Microphone", "Required only for voice input.", dangerous = true),
            PermissionState(Capability.NOTIFICATION_ACCESS, if (notificationsGranted) PermissionStatus.GRANTED else PermissionStatus.REQUIRES_USER_ACTION, "Notifications", "Controls whether Vynnra may post notifications.", dangerous = true),
            PermissionState(Capability.OVERLAY, if (overlayGranted) PermissionStatus.GRANTED else PermissionStatus.REQUIRES_SETTINGS, "Floating Assistant", "Allows Vynnra's floating assistant surface.", dangerous = true),
            PermissionState(Capability.BACKGROUND_EXECUTION, PermissionStatus.REQUIRES_USER_ACTION, "Background Execution", "Requires an appropriate foreground-service/task strategy.", dangerous = true),
        )
    }

    override fun openRequiredSettings(context: Context, capability: Capability): Boolean {
        val intent = when (capability) {
            Capability.ACCESSIBILITY_CONTROL, Capability.SCREEN_INTERACT, Capability.BROWSER_CONTROL -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            Capability.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            Capability.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            Capability.FILE_READ, Capability.FILE_WRITE -> Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
            else -> return false
        }
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun isAccessibilityGranted(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = "${context.packageName}/${context.packageName}.platform.VynnraAccessibilityService"
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
