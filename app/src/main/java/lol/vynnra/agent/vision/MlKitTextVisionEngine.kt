package lol.vynnra.agent.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device visual understanding for text targets. OCR is deliberately exposed as detections
 * with bounds and confidence; hidden model reasoning is never surfaced to the UI.
 */
class MlKitTextVisionEngine : VisualUnderstandingEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyze(frame: VisualFrame): List<VisualDetection> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(frame.bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val detections = result.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line ->
                            val text = line.text.trim()
                            val bounds = line.boundingBox
                            if (text.isEmpty() || bounds == null) null
                            else VisualDetection(
                                label = text,
                                bounds = Rect(bounds),
                                confidence = 0.90f,
                                source = DetectionSource.VISION_MODEL
                            )
                        }
                    }
                    if (continuation.isActive) continuation.resume(detections)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
        }

    fun close() {
        recognizer.close()
    }
}
