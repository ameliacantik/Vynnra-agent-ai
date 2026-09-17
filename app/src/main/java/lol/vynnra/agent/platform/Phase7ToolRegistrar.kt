package lol.vynnra.agent.platform

import lol.vynnra.agent.core.orchestrator.ToolRegistry

/** Registers Phase 7 browser and file capabilities into the central registry. */
object Phase7ToolRegistrar {
    fun registerAll(registry: ToolRegistry, context: android.content.Context) {
        val browser = BrowserController(context)
        val files = FileAgent(context)

        registry.register(ToolRegistry.adapter(BrowserOpenTool(browser)) { input ->
            BrowserOpenInput(url = input.requiredString("url"))
        })
        registry.register(ToolRegistry.adapter(BrowserSearchTool(browser)) { input ->
            BrowserSearchInput(query = input.requiredString("query"))
        })
        registry.register(ToolRegistry.adapter(BrowserBackTool(browser)) { Unit })
        registry.register(ToolRegistry.adapter(BrowserClickTextTool(browser)) { input ->
            BrowserTextInput(text = input.requiredString("text"))
        })
        registry.register(ToolRegistry.adapter(BrowserTypeTextTool(browser)) { input ->
            BrowserTextInput(text = input.requiredString("text"))
        })
        registry.register(ToolRegistry.adapter(BrowserScrollTool(browser)) { input ->
            BrowserScrollInput(direction = input["direction"] as? String ?: "down")
        })
        registry.register(ToolRegistry.adapter(BrowserExtractTool(browser)) { input ->
            BrowserExtractInput(
                maxItems = (input["maxItems"] as? Number)?.toInt()?.coerceIn(1, 500) ?: 200
            )
        })
        registry.register(ToolRegistry.adapter(BrowserDownloadTool(browser)) { input ->
            BrowserDownloadInput(
                url = input.requiredString("url"),
                fileName = input["fileName"] as? String
            )
        })
        registry.register(ToolRegistry.adapter(BrowserUploadTool(browser)) { Unit })
        registry.register(ToolRegistry.adapter(NativeBrowserOpenTool(context)) { input ->
            NativeBrowserOpenInput(url = input["url"] as? String)
        })

        registry.register(ToolRegistry.adapter(FileListTool(files)) { input ->
            FileListInput(
                path = input.requiredString("path"),
                recursive = input["recursive"] as? Boolean ?: false,
                maxEntries = (input["maxEntries"] as? Number)?.toInt()?.coerceIn(1, 2000) ?: 500
            )
        })
        registry.register(ToolRegistry.adapter(FileReadTool(files)) { input ->
            FileReadInput(
                path = input.requiredString("path"),
                maxBytes = (input["maxBytes"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 2_000_000
            )
        })
        registry.register(ToolRegistry.adapter(FileWriteTool(files)) { input ->
            FileWriteInput(
                path = input.requiredString("path"),
                text = input.requiredString("text"),
                overwrite = input["overwrite"] as? Boolean ?: true,
                confirmed = input["confirmed"] as? Boolean ?: false
            )
        })
        registry.register(ToolRegistry.adapter(FileCopyTool(files)) { input ->
            FileCopyInput(
                source = input.requiredString("source"),
                destination = input.requiredString("destination"),
                overwrite = input["overwrite"] as? Boolean ?: false,
                confirmed = input["confirmed"] as? Boolean ?: false
            )
        })
        registry.register(ToolRegistry.adapter(FileMoveTool(files)) { input ->
            FileMoveInput(
                source = input.requiredString("source"),
                destination = input.requiredString("destination"),
                overwrite = input["overwrite"] as? Boolean ?: false,
                confirmed = input["confirmed"] as? Boolean ?: false
            )
        })
        registry.register(ToolRegistry.adapter(FileDeleteTool(files)) { input ->
            FileDeleteInput(
                path = input.requiredString("path"),
                confirmed = input["confirmed"] as? Boolean ?: false
            )
        })
        registry.register(ToolRegistry.adapter(FileSearchTool(files)) { input ->
            FileSearchInput(
                root = input.requiredString("root"),
                query = input.requiredString("query"),
                maxResults = (input["maxResults"] as? Number)?.toInt()?.coerceIn(1, 500) ?: 100
            )
        })
        registry.register(ToolRegistry.adapter(FileAccessStatusTool(files)) { Unit })
        registry.register(ToolRegistry.adapter(FileBulkDeleteTool(files)) { input ->
            FileBulkDeleteInput(
                paths = (input["paths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                confirmed = input["confirmed"] as? Boolean ?: false
            )
        })
    }

    private fun Map<String, Any?>.requiredString(key: String): String =
        (this[key] as? String)?.takeIf { it.isNotBlank() } ?: error("Missing or blank input: $key")
}
