package lol.vynnra.agent.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** App-facing controller for the user-approved screen capture service. */
class ScreenCaptureController(context: Context) {
    private val appContext = context.applicationContext

    fun start(grant: ScreenCaptureConsentController.ProjectionGrant): Boolean {
        val intent = Intent(appContext, VynnraScreenCaptureService::class.java).apply {
            action = VynnraScreenCaptureService.ACTION_START
            putExtra("resultCode", grant.resultCode)
            putExtra("resultData", grant.data)
        }
        return try {
            ContextCompat.startForegroundService(appContext, intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun stop() {
        appContext.startService(
            Intent(appContext, VynnraScreenCaptureService::class.java).apply {
                action = VynnraScreenCaptureService.ACTION_STOP
            }
        )
    }

    fun isActive(): Boolean = ScreenCaptureStore.isActive()

    fun latestFrame(): ScreenFrame? = ScreenCaptureStore.latestFrame()
}
