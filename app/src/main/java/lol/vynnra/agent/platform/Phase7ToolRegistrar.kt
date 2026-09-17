package lol.vynnra.agent.platform

import lol.vynnra.agent.core.orchestrator.ToolRegistry

/** Registers Phase 7 browser and file capabilities into the central registry. */
object Phase7ToolRegistrar {
    fun registerAll(registry: ToolRegistry, context: android.content.Context) {
        val browser = BrowserController(context)
        val files = FileAgent(context)

        registry.register(ToolRegistry.adapter(BrowserOpenTool(browser)) { input ->
            mapOf("url" to input.requiredString("url"))
        })
        registry.register(ToolRegistry.adapter(BrowserSearchTool(browser)) { input ->
            mapOf("query" to input.requiredString("query"))
        })
        registry.register(ToolRegistry.adapter(BrowserBackTool(browser)) { Unit })
        registry.register(ToolRegistry.adapter(BrowserClickTextTool(browser)) { input ->
            mapOf("text" to input.requiredString("text"))
        })
        registry.register(ToolRegistry.adapter(BrowserTypeTextTool(browser)) { input ->
            mapOf("text" to input.requiredString("text"))
        })
        registry.register(ToolRegistry.adapter(BrowserScrollTool(browser)) { input ->
            mapOf("direction" to (input["direction"] as? String ?: "down"))
        })
        registry.register(ToolRegistry.adapter(BrowserExtractTool(browser)) { input ->
            mapOf("maxItems" to (input["maxItems"] as? Number)?.toInt()?.coerceIn(1, 500) ?: 200)
        })
        registry.register(ToolRegistry.adapter(BrowserDownloadTool(browser)) { input ->
            mapOf("url" to input.requiredString("url"), "fileName" to input["fileName"] as? String)
        })
        registry.register(ToolRegistry.adapter(BrowserUploadTool(browser)) { Unit })

        registry.register(ToolRegistry.adapter(FileListTool(files)) { input ->
            mapOf(
                "path" to input.requiredString("path"),
                "recursive" to (input["recursive"] as? Boolean ?: false),
                "maxEntries" to (input["maxEntries"] as? Number)?.toInt()?.coerceIn(1, 2000) ?: 500
            )
        })
        registry.register(ToolRegistry.adapter(FileReadTool(files)) { input ->
            mapOf("path" to input.requiredString("path"), "maxBytes" to (input["maxBytes"] as? Number)?.toInt() ?: 2_000_000)
        })
        registry.register(ToolRegistry.adapter(FileWriteTool(files)) { input ->
            mapOf(
                "path" to input.requiredString("path"),
                "text" to input["text"].requiredStringValue("text"),
                "overwrite" to (input["overwrite"] as? Boolean ?: true),
                "confirmed" to (input["confirmed"] as? Boolean ?: false)
            )
        })
        registry.register(ToolRegistry.adapter(FileCopyTool(files)) { input ->
            mapOf(
                "source" to input.requiredString("source"),
                "destination" to input.requiredString("destination"),
                "overwrite" to (input["overwrite"] as? Boolean ?: false),
                "confirmed" to (input["confirmed"] as? Boolean ?: false)
            )
        })
        registry.register(ToolRegistry.adapter(FileMoveTool(files)) { input ->
            mapOf(
                "source" to input.requiredString("source"),
                "destination" to input.requiredString("destination"),
                "overwrite" to (input["overwrite"] as? Boolean ?: false),
                "confirmed" to (input["confirmed"] as? Boolean ?: false)
            )
        })
        registry.register(ToolRegistry.adapter(FileDeleteTool(files)) { input ->
            mapOf("path" to input.requiredString("path"), "confirmed" to (input["confirmed"] as? Boolean ?: false))
        })
        registry.register(ToolRegistry.adapter(FileSearchTool(files)) { input ->
            mapOf(
                "root" to input.requiredString("root"),
                "query" to input.requiredString("query"),
                "maxResults" to (input["maxResults"] as? Number)?.toInt()?.coerceIn(1, 500) ?: 100
            )
        })
        registry.register(ToolRegistry.adapter(FileAccessStatusTool(files)) { Unit })
        registry.register(ToolRegistry.adapter(FileBulkDeleteTool(files)) { input ->
            mapOf(
                "paths" to (input["paths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList<String>(),
                "confirmed" to (input["confirmed"] as? Boolean ?: false)
            )
        })
    }

    private fun Map<String, Any?>.requiredString(key: String): String =
        (this[key] as? String)?.takeIf { it.isNotBlank() } ?: error("Missing or blank input: $key")

    private fun Any?.requiredStringValue(key: String): String =
        (this as? String)?.takeIf { it.isNotBlank() } ?: error("Missing or blank input: $key")
}
