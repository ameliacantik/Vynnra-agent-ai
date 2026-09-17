package lol.vynnra.agent.platform

import android.content.Context
import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.core.tool.VynnraTool

class NativeBrowserOpenTool(private val context: Context) : VynnraTool<NativeBrowserOpenInput> {
    override val definition = ToolDefinition(
        id = "browser.open_native",
        name = "Open Vynnra Browser",
        description = "Open the AI-native Vynnra browser surface instead of driving Chrome UI.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL),
        riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )

    override suspend fun execute(input: NativeBrowserOpenInput): ToolResult = try {
        context.startActivity(VynnraBrowserActivity.intent(context, input.url))
        ToolResult(ToolResultStatus.SUCCESS, data = mapOf("url" to input.url), message = "Vynnra Browser opened")
    } catch (error: Exception) {
        ToolResult(ToolResultStatus.FAILED, message = "Unable to open Vynnra Browser: ${error.message ?: "unknown error"}")
    }
}

data class NativeBrowserOpenInput(val url: String? = null)
