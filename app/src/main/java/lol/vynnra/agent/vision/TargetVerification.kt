package lol.vynnra.agent.vision

import android.graphics.Rect
import lol.vynnra.agent.platform.ScreenSnapshot

data class TargetVerificationResult(val verified: Boolean, val reason: String, val matchedTarget: TargetCandidate? = null)

class TargetVerifier {
    fun verify(expectedQuery: String, before: List<TargetCandidate>, after: ScreenSnapshot?, detectionsAfter: List<VisualDetection> = emptyList()): TargetVerificationResult {
        val afterCandidates = FusedTargetResolver().resolve(expectedQuery, after, detectionsAfter)
        if (afterCandidates.isEmpty()) return TargetVerificationResult(false, "Expected target is not visible after the action")
        if (before.isEmpty()) return TargetVerificationResult(true, "Target is visible after the action", afterCandidates.first())
        val previous = before.first()
        val changed = afterCandidates.any { it.bounds != previous.bounds || it.label != previous.label }
        return if (changed) TargetVerificationResult(true, "Target state changed or was replaced", afterCandidates.first())
        else TargetVerificationResult(false, "Target remains unchanged after the action", afterCandidates.first())
    }

    fun verifyBoundsVisible(bounds: Rect, screenWidth: Int, screenHeight: Int): Boolean =
        bounds.width() > 0 && bounds.height() > 0 && bounds.left < screenWidth && bounds.top < screenHeight && bounds.right > 0 && bounds.bottom > 0
}
