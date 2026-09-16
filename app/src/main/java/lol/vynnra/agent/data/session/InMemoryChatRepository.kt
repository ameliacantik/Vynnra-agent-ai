package lol.vynnra.agent.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ChatRepository {
    val sessions: StateFlow<List<ChatSession>>
    fun createSession(title: String = "New Chat"): ChatSession
    fun appendMessage(message: ChatMessage)
}

class InMemoryChatRepository : ChatRepository {
    private val sessionState = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messages = mutableListOf<ChatMessage>()

    override val sessions: StateFlow<List<ChatSession>> = sessionState.asStateFlow()

    override fun createSession(title: String): ChatSession {
        val session = ChatSession(title = title)
        sessionState.value = listOf(session) + sessionState.value
        return session
    }

    override fun appendMessage(message: ChatMessage) {
        messages += message
    }
}
