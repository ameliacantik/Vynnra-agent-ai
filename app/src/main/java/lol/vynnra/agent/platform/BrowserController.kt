package lol.vynnra.agent.platform

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Chrome-focused controller built on Android intents plus the existing Accessibility service.
 * UI actions are gated on Chrome being the current foreground package.
 */
class BrowserController(context: Context) {
    private val appContext = context.applicationContext

    fun openUrl(url: String): BrowserActionResult {
        val safeUrl = normalizeHttpUrl(url) ?: return BrowserActionResult(false, "Only http/https URLs are allowed")
        val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            setPackage(CHROME_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            appContext.startActivity(chromeIntent)
            BrowserActionResult(true, "Chrome opened", mapOf("url" to safeUrl))
        } catch (_: Exception) {
            try {
                appContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                BrowserActionResult(true, "Default browser opened", mapOf("url" to safeUrl))
            } catch (error: Exception) {
                BrowserActionResult(false, "Unable to open browser: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun search(query: String): BrowserActionResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return BrowserActionResult(false, "Search query is blank")
        val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        return openUrl("https://www.google.com/search?q=$encoded")
    }

    fun back(): BrowserActionResult {
        if (!isChromeForeground()) return chromeUnavailable()
        return VynnraAccessibilityService.current()?.globalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
        )?.let { BrowserActionResult(it.success, it.message) }
            ?: BrowserActionResult(false, "Accessibility service is unavailable")
    }

    fun clickText(text: String): BrowserActionResult {
        if (!isChromeForeground()) return chromeUnavailable()
        return VynnraAccessibilityService.current()?.clickText(text)
            ?.let { BrowserActionResult(it.success, it.message) }
            ?: BrowserActionResult(false, "Accessibility service is unavailable")
    }

    fun typeText(text: String): BrowserActionResult {
        if (!isChromeForeground()) return chromeUnavailable()
        return VynnraAccessibilityService.current()?.typeText(text)
            ?.let { BrowserActionResult(it.success, it.message) }
            ?: BrowserActionResult(false, "Accessibility service is unavailable")
    }

    fun scrollDown(): BrowserActionResult {
        if (!isChromeForeground()) return chromeUnavailable()
        return dispatchScroll(0.78f, 0.42f, 0.78f, 0.18f)
    }

    fun scrollUp(): BrowserActionResult {
        if (!isChromeForeground()) return chromeUnavailable()
        return dispatchScroll(0.42f, 0.20f, 0.42f, 0.76f)
    }

    fun currentPage(): BrowserPageSnapshot? {
        val service = VynnraAccessibilityService.current() ?: return null
        val snapshot = service.inspectScreen(maxNodes = 500) ?: return null
        if (snapshot.packageName != CHROME_PACKAGE) return null
        return BrowserPageSnapshot(
            packageName = snapshot.packageName,
            capturedAtEpochMs = snapshot.capturedAtEpochMs,
            texts = snapshot.nodes.mapNotNull { node ->
                (node.text ?: node.contentDescription)?.trim()?.takeIf { it.isNotEmpty() }
            }.distinct()
        )
    }

    fun extractText(maxItems: Int = 200): List<String> =
        currentPage()?.texts.orEmpty().take(maxItems.coerceIn(1, 500))

    fun download(url: String, fileName: String? = null): BrowserActionResult {
        val safeUrl = normalizeHttpUrl(url) ?: return BrowserActionResult(false, "Only http/https URLs are allowed")
        val guessedName = fileName?.takeIf { it.isNotBlank() }
            ?: Uri.parse(safeUrl).lastPathSegment?.takeIf { it.isNotBlank() }
            ?: "vynnra-download"
        val request = DownloadManager.Request(Uri.parse(safeUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(guessedName)
            .setDescription("Downloaded by Vynnra")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessedName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val manager = appContext.getSystemService(DownloadManager::class.java)
            ?: return BrowserActionResult(false, "DownloadManager unavailable")
        return try {
            val id = manager.enqueue(request)
            BrowserActionResult(true, "Download queued", mapOf("downloadId" to id, "fileName" to guessedName))
        } catch (error: Exception) {
            BrowserActionResult(false, "Download failed: ${error.message ?: "unknown error"}")
        }
    }

    fun openUploadPicker(): BrowserActionResult {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            appContext.startActivity(Intent.createChooser(intent, "Select file for Vynnra upload"))
            BrowserActionResult(true, "File picker opened for upload")
        } catch (error: Exception) {
            BrowserActionResult(false, "Unable to open file picker: ${error.message ?: "unknown error"}")
        }
    }

    fun openAccessibilitySettings(): BrowserActionResult {
        return try {
            appContext.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            BrowserActionResult(true, "Accessibility settings opened")
        } catch (error: Exception) {
            BrowserActionResult(false, "Unable to open accessibility settings: ${error.message ?: "unknown error"}")
        }
    }

    private fun isChromeForeground(): Boolean =
        VynnraAccessibilityService.current()?.inspectScreen(maxNodes = 1)?.packageName == CHROME_PACKAGE

    private fun chromeUnavailable(): BrowserActionResult =
        BrowserActionResult(false, "Chrome is not the current foreground app")

    private fun dispatchScroll(startX: Float, startY: Float, endX: Float, endY: Float): BrowserActionResult {
        val controller = AndroidController(appContext)
        val result = controller.swipe(startX, startY, endX, endY, 500L)
        return BrowserActionResult(result.success, result.message)
    }

    private fun normalizeHttpUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val candidate = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val scheme = Uri.parse(candidate).scheme?.lowercase()
        return candidate.takeIf { scheme == "http" || scheme == "https" }
    }

    companion object {
        const val CHROME_PACKAGE = "com.android.chrome"
    }
}

data class BrowserActionResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap()
)

data class BrowserPageSnapshot(
    val packageName: String?,
    val capturedAtEpochMs: Long,
    val texts: List<String>
)
