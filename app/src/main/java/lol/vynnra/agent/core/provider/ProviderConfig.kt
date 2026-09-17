package lol.vynnra.agent.core.provider

data class ProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String
) {
    fun hasEndpoint(): Boolean = normalizedBaseUrl().isNotEmpty()

    fun isConfigured(): Boolean = hasEndpoint() && model.trim().isNotEmpty()

    fun normalizedBaseUrl(): String {
        var value = baseUrl.trim().trimEnd('/')
        value = value.removeSuffix("/chat/completions").trimEnd('/')
        value = value.removeSuffix("/models").trimEnd('/')
        return value
    }

    fun authorizationHeader(): String? =
        apiKey.trim().takeIf { it.isNotEmpty() }?.let { "Bearer $it" }
}
