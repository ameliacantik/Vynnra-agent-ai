package lol.vynnra.agent.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

class VynnraAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: VynnraAccessibilityService? = null

        fun current(): VynnraAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun globalAction(action: Int): AndroidControlResult {
        val ok = performGlobalAction(action)
        return AndroidControlResult(ok, if (ok) "Global action executed" else "Global action rejected")
    }

    fun tap(x: Float, y: Float, durationMs: Long = 50L): AndroidControlResult {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(1L, 10_000L)))
            .build()
        return dispatch(gesture, "Tap")
    }

    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 400L
    ): AndroidControlResult {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(1L, 10_000L)))
            .build()
        return dispatch(gesture, "Swipe")
    }

    fun typeText(text: String): AndroidControlResult {
        val root = rootInActiveWindow
            ?: return AndroidControlResult(false, "Active accessibility window is unavailable")
        val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findEditable(root)
            ?: return AndroidControlResult(false, "No editable field is focused")

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val ok = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return AndroidControlResult(ok, if (ok) "Text entered" else "Text input rejected")
    }

    fun findText(text: String, ignoreCase: Boolean = true): List<UiElementInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByText(text)
            .mapNotNull { node ->
                val value = node.text?.toString() ?: node.contentDescription?.toString()
                value?.let {
                    if (ignoreCase && !it.contains(text, ignoreCase = true)) return@mapNotNull null
                    UiElementInfo(it, node.className?.toString(), node.isClickable, node.isEditable)
                }
            }
    }

    fun clickText(text: String): AndroidControlResult {
        val root = rootInActiveWindow
            ?: return AndroidControlResult(false, "Active accessibility window is unavailable")
        val node = root.findAccessibilityNodeInfosByText(text).firstOrNull()
            ?: return AndroidControlResult(false, "Text not found: $text")
        val clickable = findClickable(node)
            ?: return AndroidControlResult(false, "No clickable parent found for: $text")
        val ok = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return AndroidControlResult(ok, if (ok) "Clicked: $text" else "Click rejected: $text")
    }

    fun inspectScreen(maxNodes: Int = 300): ScreenSnapshot? =
        ScreenInspector.capture(rootInActiveWindow, maxNodes)

    private fun dispatch(gesture: GestureDescription, name: String): AndroidControlResult {
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) = Unit
            override fun onCancelled(gestureDescription: GestureDescription?) = Unit
        }
        val ok = dispatchGesture(gesture, callback, null)
        return AndroidControlResult(ok, if (ok) "$name dispatched" else "$name rejected")
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                findEditable(child)?.let { return it }
            }
        }
        return null
    }

    private fun findClickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable) return node
        return node.parent?.let { findClickable(it) }
    }
}

data class UiElementInfo(
    val text: String,
    val className: String?,
    val clickable: Boolean,
    val editable: Boolean
)
