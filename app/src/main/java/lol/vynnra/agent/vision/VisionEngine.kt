package lol.vynnra.agent.vision

import android.graphics.Bitmap
import android.graphics.Rect
import lol.vynnra.agent.platform.ScreenNodeSnapshot
import lol.vynnra.agent.platform.ScreenSnapshot

data class VisualFrame(val bitmap: Bitmap, val capturedAtEpochMs: Long)
data class VisualDetection(val label: String, val bounds: Rect, val confidence: Float, val source: DetectionSource)
enum class DetectionSource { ACCESSIBILITY, VISION_MODEL, HEURISTIC }
data class TargetCandidate(val label: String, val bounds: Rect, val confidence: Float, val source: DetectionSource)

interface VisualUnderstandingEngine {
    suspend fun analyze(frame: VisualFrame): List<VisualDetection>
}

/** Safe default: never invents visual detections before a vision provider is configured. */
class NoOpVisualUnderstandingEngine : VisualUnderstandingEngine {
    override suspend fun analyze(frame: VisualFrame): List<VisualDetection> = emptyList()
}

/** Fuses accessibility targets with vision-model detections without exposing model reasoning. */
class FusedTargetResolver {
    fun resolve(query: String, screen: ScreenSnapshot?, visualDetections: List<VisualDetection> = emptyList()): List<TargetCandidate> {
        if (query.isBlank()) return emptyList()
        val candidates = mutableListOf<TargetCandidate>()
        screen?.nodes.orEmpty().forEach { node ->
            val label = node.text ?: node.contentDescription ?: return@forEach
            if (!label.contains(query.trim(), ignoreCase = true)) return@forEach
            candidates += TargetCandidate(label, Rect(node.left, node.top, node.right, node.bottom), confidence(node), DetectionSource.ACCESSIBILITY)
        }
        visualDetections.filter { it.label.contains(query.trim(), ignoreCase = true) }.forEach { detection ->
            candidates += TargetCandidate(detection.label, Rect(detection.bounds), detection.confidence.coerceIn(0f, 1f), detection.source)
        }
        return candidates.sortedByDescending { it.confidence }.distinctBy { "${it.label.lowercase()}@${it.bounds}" }
    }

    private fun confidence(node: ScreenNodeSnapshot): Float = when {
        node.clickable && node.enabled && node.visibleToUser -> 0.98f
        node.visibleToUser && node.enabled -> 0.90f
        node.visibleToUser -> 0.75f
        else -> 0.50f
    }
}
