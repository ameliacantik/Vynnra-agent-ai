package lol.vynnra.agent.platform

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Filesystem agent for Vynnra. It supports app-owned files on every installation and
 * shared-storage operations when Android grants all-files access. Protected system paths
 * are rejected before any filesystem mutation.
 */
class FileAgent(context: Context) {
    private val appContext = context.applicationContext
    private val appRoots = listOfNotNull(
        appContext.filesDir,
        appContext.getExternalFilesDir(null)
    ).map { it.canonicalFile }

    fun accessStatus(): FileAccessStatus {
        val fullStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        return FileAccessStatus(
            fullStorageGranted = fullStorage,
            appOwnedRoots = appRoots.map { it.path },
            sharedStorageRoots = if (fullStorage) accessibleSharedRoots().map { it.path } else emptyList()
        )
    }

    fun list(path: String, recursive: Boolean = false, maxEntries: Int = 500): FileOperationResult {
        val target = resolveAccessible(path, write = false) ?: return blocked(path)
        if (!target.isDirectory) return failed("Not a directory: ${target.path}")
        val limit = maxEntries.coerceIn(1, 2000)
        val entries = mutableListOf<FileEntry>()
        if (recursive) {
            collect(target, entries, limit)
        } else {
            target.listFiles()?.take(limit)?.forEach { entries += describe(it) }
        }
        return success("Listed ${entries.size} entries", mapOf("entries" to entries))
    }

    fun readText(path: String, maxBytes: Int = 2_000_000): FileOperationResult {
        val target = resolveAccessible(path, write = false) ?: return blocked(path)
        if (!target.isFile) return failed("Not a file: ${target.path}")
        if (target.length() > maxBytes.coerceAtLeast(1)) return failed("File exceeds read limit")
        return try {
            val bytes = FileInputStream(target).use { it.readBytes() }
            success("File read", mapOf("path" to target.path, "text" to String(bytes, StandardCharsets.UTF_8)))
        } catch (error: Exception) {
            failed("Read failed: ${error.message ?: "unknown error"}")
        }
    }

    fun writeText(path: String, text: String, overwrite: Boolean = true): FileOperationResult {
        val target = resolveAccessible(path, write = true) ?: return blocked(path)
        if (target.exists() && !overwrite) return failed("File already exists")
        val parent = target.parentFile ?: return failed("Target has no parent directory")
        if (!parent.exists() && !parent.mkdirs()) return failed("Unable to create parent directory")
        return try {
            FileOutputStream(target, false).use { it.write(text.toByteArray(StandardCharsets.UTF_8)) }
            success("File written", mapOf("path" to target.path, "bytes" to target.length()))
        } catch (error: Exception) {
            failed("Write failed: ${error.message ?: "unknown error"}")
        }
    }

    fun copy(sourcePath: String, destinationPath: String, overwrite: Boolean = false): FileOperationResult {
        val source = resolveAccessible(sourcePath, write = false) ?: return blocked(sourcePath)
        val destination = resolveAccessible(destinationPath, write = true) ?: return blocked(destinationPath)
        if (!source.isFile) return failed("Copy currently supports files only")
        if (destination.exists() && !overwrite) return failed("Destination exists")
        val parent = destination.parentFile ?: return failed("Destination has no parent directory")
        if (!parent.exists() && !parent.mkdirs()) return failed("Unable to create destination directory")
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination, false).use { output -> input.copyTo(output) }
            }
            success("File copied", mapOf("source" to source.path, "destination" to destination.path))
        } catch (error: Exception) {
            failed("Copy failed: ${error.message ?: "unknown error"}")
        }
    }

    fun move(sourcePath: String, destinationPath: String, overwrite: Boolean = false): FileOperationResult {
        val source = resolveAccessible(sourcePath, write = true) ?: return blocked(sourcePath)
        val destination = resolveAccessible(destinationPath, write = true) ?: return blocked(destinationPath)
        if (!source.exists()) return failed("Source does not exist")
        if (destination.exists() && !overwrite) return failed("Destination exists")
        val parent = destination.parentFile ?: return failed("Destination has no parent directory")
        if (!parent.exists() && !parent.mkdirs()) return failed("Unable to create destination directory")
        if (destination.exists() && !destination.delete()) return failed("Unable to replace destination")
        return if (source.renameTo(destination)) {
            success("File moved", mapOf("source" to source.path, "destination" to destination.path))
        } else {
            failed("Move failed")
        }
    }

    fun delete(path: String): FileOperationResult {
        val target = resolveAccessible(path, write = true) ?: return blocked(path)
        if (!target.exists()) return failed("Path does not exist")
        if (target == target.parentFile?.root || target.path == "/") return failed("Refusing to delete filesystem root")
        return if (target.deleteRecursively()) {
            success("Deleted", mapOf("path" to target.path))
        } else {
            failed("Delete failed")
        }
    }

    fun search(rootPath: String, query: String, maxResults: Int = 100): FileOperationResult {
        val root = resolveAccessible(rootPath, write = false) ?: return blocked(rootPath)
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return failed("Search query is blank")
        val results = mutableListOf<FileEntry>()
        fun visit(file: File) {
            if (results.size >= maxResults.coerceIn(1, 500)) return
            if (file.name.lowercase().contains(needle)) results += describe(file)
            if (file.isDirectory) file.listFiles()?.forEach(::visit)
        }
        visit(root)
        return success("Search completed", mapOf("results" to results))
    }

    private fun resolveAccessible(path: String, write: Boolean): File? {
        val raw = path.trim()
        if (raw.isEmpty()) return null
        val candidate = try { File(raw).canonicalFile } catch (_: Exception) { return null }
        if (isProtected(candidate)) return null
        if (appRoots.any { isWithin(candidate, it) }) return candidate
        val fullStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        if (!fullStorage) return null
        val allowedRoot = accessibleSharedRoots().firstOrNull { isWithin(candidate, it) }
        return allowedRoot?.let { candidate }
    }

    private fun accessibleSharedRoots(): List<File> = listOfNotNull(
        Environment.getExternalStorageDirectory(),
        appContext.getExternalFilesDirs(null).firstOrNull { it != null }?.parentFile?.parentFile?.parentFile
    ).map { it.canonicalFile }.distinctBy { it.path }

    private fun isProtected(file: File): Boolean {
        val path = file.path
        return PROTECTED_PREFIXES.any { path == it || path.startsWith("$it/") }
    }

    private fun isWithin(file: File, root: File): Boolean =
        file == root || file.path.startsWith(root.path + File.separator)

    private fun collect(directory: File, out: MutableList<FileEntry>, limit: Int) {
        if (out.size >= limit) return
        directory.listFiles()?.forEach { child ->
            if (out.size >= limit) return
            out += describe(child)
            if (child.isDirectory) collect(child, out, limit)
        }
    }

    private fun describe(file: File): FileEntry = FileEntry(
        path = file.path,
        name = file.name,
        isDirectory = file.isDirectory,
        sizeBytes = file.length(),
        lastModifiedEpochMs = file.lastModified()
    )

    private fun success(message: String, data: Map<String, Any?> = emptyMap()) =
        FileOperationResult(true, message, data)

    private fun failed(message: String) = FileOperationResult(false, message)

    private fun blocked(path: String) = FileOperationResult(false, "Path unavailable or protected: $path")

    companion object {
        private val PROTECTED_PREFIXES = listOf("/data", "/proc", "/sys", "/system", "/vendor", "/dev")
    }
}

data class FileAccessStatus(
    val fullStorageGranted: Boolean,
    val appOwnedRoots: List<String>,
    val sharedStorageRoots: List<String>
)

data class FileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long
)

data class FileOperationResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap()
)
