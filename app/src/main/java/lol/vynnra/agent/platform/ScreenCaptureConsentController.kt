package lol.vynnra.agent.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

/**
 * Owns the user-consent result for Android screen capture.
 * The projection grant is kept in memory only and is intentionally not persisted.
 */
class ScreenCaptureConsentController(context: Context) {
    private val projectionManager =
        context.applicationContext.getSystemService(MediaProjectionManager::class.java)

    @Volatile
    private var grantedResultCode: Int? = null

    @Volatile
    private var grantedData: Intent? = null

    fun createConsentIntent(): Intent = projectionManager.createScreenCaptureIntent()

    fun handleActivityResult(resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) {
            clear()
            return false
        }

        grantedResultCode = resultCode
        grantedData = Intent(data)
        return true
    }

    fun isGranted(): Boolean =
        grantedResultCode == Activity.RESULT_OK && grantedData != null

    /**
     * Returns a defensive copy for the future screenshot pipeline.
     * The caller owns the returned Intent copy and must not persist the grant.
     */
    fun currentGrant(): ProjectionGrant? {
        val code = grantedResultCode ?: return null
        val data = grantedData ?: return null
        return ProjectionGrant(code, Intent(data))
    }

    fun clear() {
        grantedResultCode = null
        grantedData = null
    }

    data class ProjectionGrant(
        val resultCode: Int,
        val data: Intent
    )
}
