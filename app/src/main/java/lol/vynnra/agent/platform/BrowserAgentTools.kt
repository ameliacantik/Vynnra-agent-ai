package lol.vynnra.agent.platform

import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.core.tool.VynnraTool

class BrowserOpenTool(private val browser: BrowserController) : VynnraTool<BrowserOpenInput> {
    override val definition = ToolDefinition(
        id = "browser.open_url", name = "Open URL", description = "Open an HTTP/HTTPS URL in Chrome.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL), riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )
    override suspend fun execute(input: BrowserOpenInput): ToolResult = browser.openUrl(input.url).toToolResult()
}

data class BrowserOpenInput(val url: String)

class BrowserSearchTool(private val browser: BrowserController) : VynnraTool<BrowserSearchInput> {
    override val definition = ToolDefinition(
        id = "browser.search", name = "Search web in Chrome", description = "Search the web from Chrome.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL), riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )
    override suspend fun execute(input: BrowserSearchInput): ToolResult = browser.search(input.query).toToolResult()
}

data class BrowserSearchInput(val query: String)

class BrowserBackTool(private val browser: BrowserController) : VynnraTool<Unit> {
    override val definition = ToolDefinition(
        id = "browser.back", name = "Browser back", description = "Navigate Chrome back.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.ACCESSIBILITY_CONTROL), riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )
    override suspend fun execute(input: Unit): ToolResult = browser.back().toToolResult()
}

class BrowserClickTextTool(private val browser: BrowserController) : VynnraTool<BrowserTextInput> {
    override val definition = ToolDefinition(
        id = "browser.click_text", name = "Click browser text", description = "Click a visible text target in Chrome.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.ACCESSIBILITY_CONTROL), riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: BrowserTextInput): ToolResult = browser.clickText(input.text).toToolResult()
}

class BrowserTypeTextTool(private val browser: BrowserController) : VynnraTool<BrowserTextInput> {
    override val definition = ToolDefinition(
        id = "browser.type_text", name = "Type in browser", description = "Type into the focused browser field.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.ACCESSIBILITY_CONTROL), riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: BrowserTextInput): ToolResult = browser.typeText(input.text).toToolResult()
}

data class BrowserTextInput(val text: String)

class BrowserScrollTool(private val browser: BrowserController) : VynnraTool<BrowserScrollInput> {
    override val definition = ToolDefinition(
        id = "browser.scroll", name = "Scroll browser", description = "Scroll the current Chrome page.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.ACCESSIBILITY_CONTROL), riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )
    override suspend fun execute(input: BrowserScrollInput): ToolResult =
        (if (input.direction.equals("up", true)) browser.scrollUp() else browser.scrollDown()).toToolResult()
}

data class BrowserScrollInput(val direction: String = "down")

class BrowserExtractTool(private val browser: BrowserController) : VynnraTool<BrowserExtractInput> {
    override val definition = ToolDefinition(
        id = "browser.extract_text", name = "Extract browser text", description = "Extract visible text from the active accessibility tree.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.SCREEN_READ), riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )
    override suspend fun execute(input: BrowserExtractInput): ToolResult = ToolResult(
        status = ToolResultStatus.SUCCESS,
        data = mapOf("texts" to browser.extractText(input.maxItems.coerceIn(1, 500))),
        message = "Browser text extracted"
    )
}

data class BrowserExtractInput(val maxItems: Int = 200)

class BrowserDownloadTool(private val browser: BrowserController) : VynnraTool<BrowserDownloadInput> {
    override val definition = ToolDefinition(
        id = "browser.download", name = "Download URL", description = "Queue a URL download into the Android Downloads directory.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.FILE_WRITE), riskLevel = RiskLevel.MEDIUM,
        requiresConfirmation = true, supportsVerification = true
    )
    override suspend fun execute(input: BrowserDownloadInput): ToolResult = browser.download(input.url, input.fileName).toToolResult()
}

data class BrowserDownloadInput(val url: String, val fileName: String? = null)

class BrowserUploadTool(private val browser: BrowserController) : VynnraTool<Unit> {
    override val definition = ToolDefinition(
        id = "browser.upload", name = "Choose upload file", description = "Open Android's document picker to begin a browser upload workflow.",
        requiredCapabilities = setOf(Capability.BROWSER_CONTROL, Capability.FILE_READ), riskLevel = RiskLevel.MEDIUM,
        requiresConfirmation = true
    )
    override suspend fun execute(input: Unit): ToolResult = browser.openUploadPicker().toToolResult()
}

private fun BrowserActionResult.toToolResult(): ToolResult = ToolResult(
    status = if (success) ToolResultStatus.SUCCESS else ToolResultStatus.FAILED,
    data = data,
    message = message
)
