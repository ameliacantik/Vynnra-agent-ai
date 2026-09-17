package lol.vynnra.agent.platform

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent

/**
 * User-authorized Android control facade. It never bypasses Android security boundaries.
 */
class AndroidController(private val context: Context) {

    fun back(): AndroidControlResult = serviceOrBlocked {
        it.globalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun home(): AndroidControlResult = serviceOrBlocked {
        it.globalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    fun recents(): AndroidControlResult = serviceOrBlocked {
        it.globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    fun notifications(): AndroidControlResult = serviceOrBlocked {
        it.globalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun launchPackage(packageName: String): AndroidControlResult {
        if (packageName.isBlank()) return AndroidControlResult(false, "Package name is empty")
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return AndroidControlResult(false, "No launchable activity for: $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            AndroidControlResult(true, "Launched: $packageName")
        } catch (t: Throwable) {
            AndroidControlResult(false, "Launch failed: ${t.message ?: "unknown error"}")
        }
    }

    fun tap(x: Float, y: Float): AndroidControlResult = serviceOrBlocked { it.tap(x, y) }

    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 400L
    ): AndroidControlResult = serviceOrBlocked {
        it.swipe(startX, startY, endX, endY, durationMs)
    }

    fun typeText(text: String): AndroidControlResult = serviceOrBlocked { it.typeText(text) }

    fun clickText(text: String): AndroidControlResult = serviceOrBlocked { it.clickText(text) }

    fun findText(text: String): List<UiElementInfo> =
        VynnraAccessibilityService.current()?.findText(text).orEmpty()

    private fun serviceOrBlocked(
        action: (VynnraAccessibilityService) -> AndroidControlResult
    ): AndroidControlResult {
        val service = VynnraAccessibilityService.current()
            ?: return AndroidControlResult(false, "Accessibility control is not enabled")
        return action(service)
    }
}
