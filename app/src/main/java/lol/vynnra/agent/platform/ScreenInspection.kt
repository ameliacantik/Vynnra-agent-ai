package lol.vynnra.agent.platform

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** A bounded, privacy-conscious snapshot of the active Accessibility tree. */
data class ScreenSnapshot(
    val capturedAtEpochMs: Long,
    val packageName: String?,
    val rootClassName: String?,
    val nodes: List<ScreenNodeSnapshot>
)

data class ScreenNodeSnapshot(
    val index: Int,
    val depth: Int,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val viewIdResourceName: String?,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val clickable: Boolean,
    val editable: Boolean,
    val enabled: Boolean,
    val focusable: Boolean,
    val focused: Boolean,
    val selected: Boolean,
    val scrollable: Boolean,
    val visibleToUser: Boolean,
    val actions: List<Int>
)

object ScreenInspector {
    private const val DEFAULT_MAX_NODES = 300
    private const val MAX_DEPTH = 40

    fun capture(root: AccessibilityNodeInfo?, maxNodes: Int = DEFAULT_MAX_NODES): ScreenSnapshot? {
        root ?: return null
        val boundedMax = maxNodes.coerceIn(1, 500)
        val nodes = ArrayList<ScreenNodeSnapshot>(minOf(boundedMax, 128))
        var nextIndex = 0

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (nodes.size >= boundedMax || depth > MAX_DEPTH) return

            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            nodes += ScreenNodeSnapshot(
                index = nextIndex++,
                depth = depth,
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                className = node.className?.toString(),
                viewIdResourceName = node.viewIdResourceName,
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom,
                clickable = node.isClickable,
                editable = node.isEditable,
                enabled = node.isEnabled,
                focusable = node.isFocusable,
                focused = node.isFocused,
                selected = node.isSelected,
                scrollable = node.isScrollable,
                visibleToUser = node.isVisibleToUser,
                actions = node.actionList.map { it.id }
            )

            for (childIndex in 0 until node.childCount) {
                if (nodes.size >= boundedMax) break
                val child = node.getChild(childIndex) ?: continue
                visit(child, depth + 1)
            }
        }

        visit(root, 0)
        return ScreenSnapshot(
            capturedAtEpochMs = System.currentTimeMillis(),
            packageName = root.packageName?.toString(),
            rootClassName = root.className?.toString(),
            nodes = nodes
        )
    }
}
