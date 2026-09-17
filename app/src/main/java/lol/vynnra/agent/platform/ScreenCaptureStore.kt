package lol.vynnra.agent.platform

import java.util.concurrent.atomic.AtomicReference

/** In-memory store for the latest screen frame. Projection data is never persisted. */
object ScreenCaptureStore {
    private val latest = AtomicReference<ScreenFrame?>(null)
    private val active = AtomicReference(false)

    fun publish(frame: ScreenFrame) {
        latest.set(frame)
    }

    fun latestFrame(): ScreenFrame? = latest.get()

    fun setActive(value: Boolean) {
        active.set(value)
        if (!value) latest.set(null)
    }

    fun isActive(): Boolean = active.get()

    fun clear() {
        latest.set(null)
        active.set(false)
    }
}

data class ScreenFrame(
    val capturedAtEpochMs: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val pngBytes: ByteArray
)
