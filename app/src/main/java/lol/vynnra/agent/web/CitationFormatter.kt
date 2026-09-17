package lol.vynnra.agent.web

/** Formats normalized source metadata for chat/citation rendering without exposing provider secrets. */
object CitationFormatter {
    fun markdownSources(sources: List<WebSource>): String = buildString {
        sources.forEachIndexed { index, source ->
            val title = source.title.replace("[", "\\[").replace("]", "\\]")
            append("[").append(index + 1).append("](").append(source.url).append(") ").append(title)
            if (source.snippet.isNotBlank()) {
                append(" — ").append(source.snippet.trim().replace("\n", " ").take(280))
            }
            append('\n')
        }
    }.trimEnd()
}
