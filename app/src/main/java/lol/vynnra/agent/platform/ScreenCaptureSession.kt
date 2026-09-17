package lol.vynnra.agent.platform

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager

/** Owns one user-consented MediaProjection capture session. */
class ScreenCaptureSession(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(MediaProjectionManager::class.java)
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val pipeline = ScreenCapturePipeline(appContext)
    private var projection: MediaProjection? = null

    fun start(grant: ScreenCaptureConsentController.ProjectionGrant): Boolean {
        stop()
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        if (width <= 0 || height <= 0 || density <= 0) return false
        val mediaProjection = manager?.getMediaProjection(grant.resultCode, grant.data) ?: return false
        var attached = false
        val started = pipeline.start(width, height, density) { surface ->
            val display = mediaProjection.createVirtualDisplay(
                "VynnraScreenCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null
            )
            if (display == null) pipeline.stop() else {
                pipeline.attachVirtualDisplay(display)
                attached = true
            }
        }
        if (!started || !attached) mediaProjection.stop() else projection = mediaProjection
        return started && attached && pipeline.isRunning()
    }

    fun latestFrame() = pipeline.latestFrame()
    fun isRunning(): Boolean = pipeline.isRunning()

    fun stop() {
        pipeline.stop()
        projection?.stop()
        projection = null
    }
}
