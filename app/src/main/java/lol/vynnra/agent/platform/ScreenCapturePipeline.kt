package lol.vynnra.agent.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.atomic.AtomicBoolean

/** In-memory screenshot pipeline fed by a user-authorized MediaProjection session. */
class ScreenCapturePipeline(private val context: Context) {
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var worker: HandlerThread? = null
    private var latest: Bitmap? = null
    private val running = AtomicBoolean(false)

    @Synchronized
    fun start(width: Int, height: Int, densityDpi: Int, onSurfaceReady: (android.view.Surface) -> Unit): Boolean {
        if (running.get()) return true
        if (width <= 0 || height <= 0 || densityDpi <= 0) return false
        val manager = context.getSystemService(DisplayManager::class.java) ?: return false
        val thread = HandlerThread("VynnraScreenCapture").also { it.start() }
        val handler = Handler(thread.looper)
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader.setOnImageAvailableListener({ source -> consume(source, width, height) }, handler)
        reader = imageReader
        worker = thread
        running.set(true)
        onSurfaceReady(imageReader.surface)
        return true
    }

    @Synchronized
    fun attachVirtualDisplay(display: VirtualDisplay) { this.display = display }

    @Synchronized
    fun latestFrame(): Bitmap? = latest?.copy(Bitmap.Config.ARGB_8888, false)

    @Synchronized
    fun stop() {
        running.set(false)
        display?.release()
        display = null
        reader?.close()
        reader = null
        worker?.quitSafely()
        worker = null
        latest?.recycle()
        latest = null
    }

    fun isRunning(): Boolean = running.get()

    private fun consume(source: ImageReader, width: Int, height: Int) {
        val image = source.acquireLatestImage() ?: return
        try {
            val plane = image.planes.firstOrNull() ?: return
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            if (pixelStride <= 0 || rowStride < pixelStride * width) return
            val paddedWidth = width + (rowStride - pixelStride * width) / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            padded.copyPixelsFromBuffer(plane.buffer)
            val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
            padded.recycle()
            replaceLatest(cropped)
        } finally { image.close() }
    }

    @Synchronized
    private fun replaceLatest(frame: Bitmap) {
        if (!running.get()) { frame.recycle(); return }
        latest?.recycle()
        latest = frame
    }
}
