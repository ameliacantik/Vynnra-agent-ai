package lol.vynnra.agent.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/** Opens the Android-controlled Level 2 all-files access screen without bypassing user consent. */
class StorageAccessController(context: Context) {
    private val appContext = context.applicationContext

    fun isFullStorageGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    fun openFullStorageSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        return try {
            appContext.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            try {
                appContext.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
