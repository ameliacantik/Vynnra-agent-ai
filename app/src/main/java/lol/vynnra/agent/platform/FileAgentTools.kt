package lol.vynnra.agent.platform

import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.core.tool.VynnraTool

class FileListTool(private val files: FileAgent) : VynnraTool<FileListInput> {
    override val definition = ToolDefinition(
        id = "file.list", name = "List files", description = "List a directory with bounded recursion.",
        requiredCapabilities = setOf(Capability.FILE_READ), riskLevel = RiskLevel.LOW
    )
    override suspend fun execute(input: FileListInput): ToolResult = files.list(input.path, input.recursive, input.maxEntries).toToolResult()
}

data class FileListInput(val path: String, val recursive: Boolean = false, val maxEntries: Int = 500)

class FileReadTool(private val files: FileAgent) : VynnraTool<FileReadInput> {
    override val definition = ToolDefinition(
        id = "file.read", name = "Read text file", description = "Read a bounded UTF-8 text file.",
        requiredCapabilities = setOf(Capability.FILE_READ), riskLevel = RiskLevel.LOW
    )
    override suspend fun execute(input: FileReadInput): ToolResult = files.readText(input.path, input.maxBytes).toToolResult()
}

data class FileReadInput(val path: String, val maxBytes: Int = 2_000_000)

class FileWriteTool(private val files: FileAgent) : VynnraTool<FileWriteInput> {
    override val definition = ToolDefinition(
        id = "file.write", name = "Write text file", description = "Write UTF-8 text to an accessible file.",
        requiredCapabilities = setOf(Capability.FILE_WRITE), riskLevel = RiskLevel.MEDIUM,
        requiresConfirmation = true
    )
    override suspend fun execute(input: FileWriteInput): ToolResult = if (!input.confirmed) {
        ToolResult(ToolResultStatus.BLOCKED, message = "Confirmation required before writing a file")
    } else {
        files.writeText(input.path, input.text, input.overwrite).toToolResult()
    }
}

data class FileWriteInput(val path: String, val text: String, val overwrite: Boolean = true, val confirmed: Boolean = false)

class FileCopyTool(private val files: FileAgent) : VynnraTool<FileCopyInput> {
    override val definition = ToolDefinition(
        id = "file.copy", name = "Copy file", description = "Copy an accessible file.",
        requiredCapabilities = setOf(Capability.FILE_READ, Capability.FILE_WRITE), riskLevel = RiskLevel.MEDIUM,
        requiresConfirmation = true
    )
    override suspend fun execute(input: FileCopyInput): ToolResult = if (!input.confirmed) {
        ToolResult(ToolResultStatus.BLOCKED, message = "Confirmation required before copying a file")
    } else files.copy(input.source, input.destination, input.overwrite).toToolResult()
}

data class FileCopyInput(val source: String, val destination: String, val overwrite: Boolean = false, val confirmed: Boolean = false)

class FileMoveTool(private val files: FileAgent) : VynnraTool<FileMoveInput> {
    override val definition = ToolDefinition(
        id = "file.move", name = "Move file", description = "Move an accessible file.",
        requiredCapabilities = setOf(Capability.FILE_READ, Capability.FILE_WRITE), riskLevel = RiskLevel.HIGH,
        requiresConfirmation = true
    )
    override suspend fun execute(input: FileMoveInput): ToolResult = if (!input.confirmed) {
        ToolResult(ToolResultStatus.BLOCKED, message = "Confirmation required before moving a file")
    } else files.move(input.source, input.destination, input.overwrite).toToolResult()
}

data class FileMoveInput(val source: String, val destination: String, val overwrite: Boolean = false, val confirmed: Boolean = false)

class FileDeleteTool(private val files: FileAgent) : VynnraTool<FileDeleteInput> {
    override val definition = ToolDefinition(
        id = "file.delete", name = "Delete file", description = "Delete an accessible file or directory.",
        requiredCapabilities = setOf(Capability.FILE_WRITE), riskLevel = RiskLevel.CRITICAL,
        requiresConfirmation = true
    )
    override suspend fun execute(input: FileDeleteInput): ToolResult = if (!input.confirmed) {
        ToolResult(ToolResultStatus.BLOCKED, message = "Confirmation required before deletion")
    } else files.delete(input.path).toToolResult()
}

data class FileDeleteInput(val path: String, val confirmed: Boolean = false)

class FileSearchTool(private val files: FileAgent) : VynnraTool<FileSearchInput> {
    override val definition = ToolDefinition(
        id = "file.search", name = "Search files", description = "Search filenames under an accessible root with a result bound.",
        requiredCapabilities = setOf(Capability.FILE_READ), riskLevel = RiskLevel.LOW,
        supportsCancellation = true
    )
    override suspend fun execute(input: FileSearchInput): ToolResult = files.search(input.root, input.query, input.maxResults).toToolResult()
}

data class FileSearchInput(val root: String, val query: String, val maxResults: Int = 100)

class FileAccessStatusTool(private val files: FileAgent) : VynnraTool<Unit> {
    override val definition = ToolDefinition(
        id = "file.access_status", name = "File access status", description = "Report current shared-storage capability and accessible roots.",
        requiredCapabilities = setOf(Capability.FILE_READ), riskLevel = RiskLevel.LOW
    )
    override suspend fun execute(input: Unit): ToolResult = ToolResult(
        ToolResultStatus.SUCCESS,
        data = mapOf("status" to files.accessStatus()),
        message = "File access status retrieved"
    )
}

class FileBulkDeleteTool(private val files: FileAgent) : VynnraTool<FileBulkDeleteInput> {
    override val definition = ToolDefinition(
        id = "file.bulk_delete", name = "Bulk delete", description = "Delete multiple files with an explicit confirmation and bounded batch size.",
        requiredCapabilities = setOf(Capability.FILE_WRITE), riskLevel = RiskLevel.CRITICAL,
        requiresConfirmation = true
    )
    override suspend fun execute(input: FileBulkDeleteInput): ToolResult {
        if (!input.confirmed) return ToolResult(ToolResultStatus.BLOCKED, message = "Confirmation required before bulk deletion")
        val paths = input.paths.distinct().take(100)
        if (paths.isEmpty()) return ToolResult(ToolResultStatus.FAILED, message = "No paths supplied")
        val results = paths.map { path -> path to files.delete(path) }
        val successes = results.count { it.second.success }
        val failures = results.filterNot { it.second.success }.map { it.first to it.second.message }
        return ToolResult(
            status = if (failures.isEmpty()) ToolResultStatus.SUCCESS else ToolResultStatus.PARTIAL,
            data = mapOf("successCount" to successes, "failureCount" to failures.size, "failures" to failures),
            message = "Bulk delete finished"
        )
    }
}

data class FileBulkDeleteInput(val paths: List<String>, val confirmed: Boolean = false)

private fun FileOperationResult.toToolResult(): ToolResult = ToolResult(
    status = if (success) ToolResultStatus.SUCCESS else ToolResultStatus.FAILED,
    data = data,
    message = message
)
