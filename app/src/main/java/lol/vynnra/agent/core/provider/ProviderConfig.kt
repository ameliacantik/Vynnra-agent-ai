package lol.vynnra.agent.core.provider

data class ProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String
) {
    fun isConfigured(): Boolean =
        baseUrl.trim().isNotEmpty() && apiKey.trim().isNotEmpty() && model.trim().isNotEmpty()

    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')
}
