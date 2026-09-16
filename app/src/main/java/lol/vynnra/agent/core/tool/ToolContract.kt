package lol.vynnra.agent.core.tool

enum class Capability {
    SCREEN_READ, SCREEN_INTERACT, ACCESSIBILITY_CONTROL,
    FILE_READ, FILE_WRITE, BROWSER_CONTROL, WEB_SEARCH,
    MICROPHONE, NOTIFICATION_ACCESS, OVERLAY, BACKGROUND_EXECUTION
}

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

enum class ToolResultStatus { SUCCESS, PARTIAL, FAILED, CANCELLED, BLOCKED }

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val requiredCapabilities: Set<Capability> = emptySet(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresConfirmation: Boolean = false,
    val supportsCancellation: Boolean = true,
    val supportsVerification: Boolean = false
)

data class ToolResult(
    val status: ToolResultStatus,
    val data: Map<String, Any?> = emptyMap(),
    val message: String? = null
)

interface VynnraTool<I> {
    val definition: ToolDefinition
    suspend fun execute(input: I): ToolResult
}
