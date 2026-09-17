package lol.vynnra.agent.platform

data class AndroidControlResult(
    val success: Boolean,
    val message: String,
    val details: Map<String, Any?> = emptyMap()
)
