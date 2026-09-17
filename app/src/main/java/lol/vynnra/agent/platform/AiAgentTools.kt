package lol.vynnra.agent.platform

import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.VynnraSystemPrompt
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.core.tool.VynnraTool

data class AiAnswerInput(
    val prompt: String,
    val context: String = ""
)

class AiAnswerTool(
    private val provider: AiProvider,
    private val modelProvider: () -> String
) : VynnraTool<AiAnswerInput> {

    override val definition = ToolDefinition(
        id = "ai.answer",
        name = "Answer User",
        description = "Generate the final natural-language response from the configured AI provider.",
        riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )

    override suspend fun execute(input: AiAnswerInput): ToolResult {
        val prompt = input.prompt.trim()
        if (prompt.isEmpty()) {
            return ToolResult(ToolResultStatus.FAILED, message = "Answer prompt is empty")
        }

        val model = modelProvider().trim()
        if (model.isEmpty()) {
            return ToolResult(ToolResultStatus.FAILED, message = "AI model is not configured")
        }

        return try {
            val context = input.context.trim().take(12_000)
            val response = provider.complete(
                AiRequest(
                    model = model,
                    messages = buildList {
                        add(AiMessage(AiMessage.Role.SYSTEM, VynnraSystemPrompt.value))
                        if (context.isNotBlank()) {
                            add(
                                AiMessage(
                                    AiMessage.Role.SYSTEM,
                                    "Verified tool observations from the current task:\n$context"
                                )
                            )
                        }
                        add(AiMessage(AiMessage.Role.USER, prompt))
                    }
                )
            )
            ToolResult(
                status = ToolResultStatus.SUCCESS,
                data = mapOf(
                    "response" to response.content,
                    "model" to response.model,
                    "finishReason" to response.finishReason
                ),
                message = response.content
            )
        } catch (error: Throwable) {
            ToolResult(
                status = ToolResultStatus.FAILED,
                message = error.message ?: "AI answer generation failed"
            )
        }
    }
}
