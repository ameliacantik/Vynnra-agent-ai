package lol.vynnra.agent.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Foreground service that owns exactly one MediaProjection session and continuously keeps
 * the newest screen frame in memory for the agent. The session is user-consented and is
 * intentionally not persisted across process death.
 */
class VynnraScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "lol.vynnra.agent.action.START_SCREEN_CAPTURE"
        const val ACTION_STOP = "lol.vynnra.agent.action.STOP_SCREEN_CAPTURE"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_RESULT_DATA = "resultData"
        private const val CHANNEL_ID = "vynnra_screen_capture"
        private const val NOTIFICATION_ID = 4106
        private const val MAX_FRAME_WIDTH = 1440
        private const val MIN_CAPTURE_INTERVAL_MS = 250L
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var lastPublishedAtMs = 0L
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopCaptureInternal()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("VynnraScreenCapture").also { it.start() }
        workerHandler = Handler(workerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCaptureInternal()
            stopSelf()
            return START_NOT_STICKY
        }

        if (mediaProjection != null) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultData == null || resultCode == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat()
        startProjection(resultCode, resultData)
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        createNotificationChannel()
        val stopIntent = Intent(this, VynnraScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4107,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Vynnra screen access active")
            .setContentText("Vynnra is using the user-approved screen capture session.")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(resultCode: Int, resultData: Intent) {
        val manager = getSystemService(MediaProjectionManager::class.java) ?: run {
            stopSelf()
            return
        }

        val projection = try {
            manager.getMediaProjection(resultCode, resultData)
        } catch (_: SecurityException) {
            stopSelf()
            return
        } ?: run {
            stopSelf()
            return
        }

        mediaProjection = projection
        projection.registerCallback(projectionCallback, workerHandler)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi.coerceAtLeast(1)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader -> captureLatest(reader) }, workerHandler)

        virtualDisplay = projection.createVirtualDisplay(
            "VynnraAgentScreen",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            workerHandler
        )
        ScreenCaptureStore.setActive(true)
    }

    private fun captureLatest(reader: ImageReader) {
        val now = System.currentTimeMillis()
        if (now - lastPublishedAtMs < MIN_CAPTURE_INTERVAL_MS) {
            reader.acquireLatestImage()?.close()
            return
        }

        val image = reader.acquireLatestImage() ?: return
        try {
            val plane = image.planes.firstOrNull() ?: return
            val width = image.width
            val height = image.height
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            if (pixelStride <= 0 || rowStride <= 0) return

            val rowPadding = rowStride - pixelStride * width
            val paddedWidth = width + (rowPadding / pixelStride).coerceAtLeast(0)
            val bitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            plane.buffer.rewind()
            bitmap.copyPixelsFromBuffer(plane.buffer)

            val cropped = if (paddedWidth != width) {
                Bitmap.createBitmap(bitmap, 0, 0, width, height).also { bitmap.recycle() }
            } else {
                bitmap
            }

            val outputWidth = min(MAX_FRAME_WIDTH, cropped.width)
            val output = if (outputWidth < cropped.width) {
                val ratio = outputWidth.toFloat() / cropped.width.toFloat()
                Bitmap.createScaledBitmap(cropped, outputWidth, (cropped.height * ratio).toInt(), true)
                    .also { cropped.recycle() }
            } else {
                cropped
            }

            val bytes = ByteArrayOutputStream().use { stream ->
                output.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            output.recycle()

            ScreenCaptureStore.publish(
                ScreenFrame(
                    capturedAtEpochMs = now,
                    width = outputWidth,
                    height = if (outputWidth == width) height else (height * outputWidth / width),
                    mimeType = "image/png",
                    pngBytes = bytes
                )
            )
            lastPublishedAtMs = now
        } finally {
            image.close()
        }
    }

    private fun stopCaptureInternal() {
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        ScreenCaptureStore.clear()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Vynnra Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Vynnra is actively using the user-approved screen capture session."
            }
        )
    }

    override fun onDestroy() {
        stopCaptureInternal()
        workerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
