package lol.vynnra.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.ProviderConfig
import lol.vynnra.agent.core.voice.VoiceAgentUiState
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository
import lol.vynnra.agent.platform.VoiceStatus
import lol.vynnra.agent.platform.VoiceUiState
import lol.vynnra.agent.ui.theme.VynnraBlack
import lol.vynnra.agent.ui.theme.VynnraMuted
import lol.vynnra.agent.ui.theme.VynnraPanel
import lol.vynnra.agent.ui.theme.VynnraPanelElevated
import lol.vynnra.agent.ui.theme.VynnraPurple
import lol.vynnra.agent.ui.theme.VynnraPurpleSoft
import lol.vynnra.agent.ui.theme.VynnraText

private data class AgentSession(val id: Int, val title: String)
private data class AgentMessage(val role: AgentMessageRole, val text: String)
private enum class AgentMessageRole { USER, ASSISTANT }

@Composable
fun VynnraAgentShell(
    memoryRepository: MemoryRepository,
    taskRepository: TaskRepository,
    screenCaptureGranted: Boolean,
    onRequestScreenCapture: () -> Unit,
    microphoneGranted: Boolean,
    voiceState: VoiceUiState,
    voiceAgentState: VoiceAgentUiState,
    providerConfig: ProviderConfig,
    onRequestMicrophone: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onStopAgent: () -> Unit,
    onSendMessage: (String, ThinkingLevel, Boolean) -> Unit,
    onSaveProviderConfig: (ProviderConfig) -> Unit
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var permissionOpen by remember { mutableStateOf(false) }
    var phase9Open by remember { mutableStateOf(false) }
    var thinkingLevel by remember { mutableStateOf(ThinkingLevel.MAX) }
    var message by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf(1) }
    val sessions = remember { mutableStateListOf(AgentSession(1, "New Chat")) }
    val messages = remember { mutableStateListOf<AgentMessage>() }

    var providerBaseUrl by remember(providerConfig.baseUrl) { mutableStateOf(providerConfig.baseUrl) }
    var providerApiKey by remember { mutableStateOf("") }
    var providerModel by remember(providerConfig.model) { mutableStateOf(providerConfig.model) }

    LaunchedEffect(voiceState.finalText) {
        val transcript = voiceState.finalText.trim()
        if (transcript.isNotEmpty()) {
            message = transcript
            onSendMessage(transcript, thinkingLevel, true)
        }
    }

    LaunchedEffect(voiceAgentState.replyId) {
        val reply = voiceAgentState.reply
        if (voiceAgentState.replyId != 0L && !reply.isNullOrBlank()) {
            messages += AgentMessage(AgentMessageRole.ASSISTANT, reply)
        }
    }

    Box(Modifier.fillMaxSize().background(VynnraBlack)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = sessions.firstOrNull { it.id == selectedSession }?.title ?: "New Chat",
                thinkingLevel = thinkingLevel,
                onMenu = { drawerOpen = true },
                onThinkingChange = { thinkingLevel = it }
            )
            StatusStrip(voiceState = voiceState, agentState = voiceAgentState)
            MessageArea(messages, Modifier.weight(1f))

            if (voiceState.partialText.isNotBlank()) {
                Surface(
                    color = VynnraPanelElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        "Listening: ${voiceState.partialText}",
                        color = VynnraMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (voiceState.errorMessage != null) {
                Text(
                    voiceState.errorMessage,
                    color = VynnraMuted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)
                )
            }
            if (voiceAgentState.error != null) {
                Text(
                    voiceAgentState.error,
                    color = VynnraMuted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)
                )
            }

            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message Vynnra…") },
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 5
                )
                IconButton(onClick = {
                    when {
                        voiceState.status == VoiceStatus.LISTENING -> onStopVoice()
                        voiceState.status == VoiceStatus.SPEAKING -> onStopSpeaking()
                        !microphoneGranted -> onRequestMicrophone()
                        else -> onStartVoice()
                    }
                }) {
                    Icon(
                        imageVector = if (voiceState.status == VoiceStatus.LISTENING) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                        contentDescription = if (voiceState.status == VoiceStatus.LISTENING) "Stop voice input" else "Voice input",
                        tint = if (voiceState.status == VoiceStatus.LISTENING) VynnraPurple else VynnraMuted
                    )
                }
                IconButton(onClick = onStopAgent, enabled = voiceAgentState.busy) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = "Stop agent", tint = VynnraMuted)
                }
                IconButton(onClick = {
                    val trimmed = message.trim()
                    if (trimmed.isNotEmpty() && !voiceAgentState.busy) {
                        messages += AgentMessage(AgentMessageRole.USER, trimmed)
                        message = ""
                        onSendMessage(trimmed, thinkingLevel, false)
                    }
                }) {
                    Icon(Icons.Outlined.Send, contentDescription = "Send", tint = VynnraPurple)
                }
            }
        }

        AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut()) {
            Drawer(
                sessions = sessions,
                selectedSession = selectedSession,
                onClose = { drawerOpen = false },
                onNewChat = {
                    val nextId = (sessions.maxOfOrNull { it.id } ?: 0) + 1
                    sessions.add(AgentSession(nextId, "New Chat"))
                    selectedSession = nextId
                    messages.clear()
                    drawerOpen = false
                },
                onSession = {
                    selectedSession = it
                    messages.clear()
                    drawerOpen = false
                },
                onPermissions = { permissionOpen = true; drawerOpen = false },
                onMemoryTasks = { phase9Open = true; drawerOpen = false },
                onSettings = { settingsOpen = true; drawerOpen = false }
            )
        }

        if (settingsOpen) {
            ModalPanel("Settings", { settingsOpen = false }) {
                SettingRow("Appearance", "AMOLED / Dark", Icons.Outlined.AutoAwesome)
                SettingRow("Thinking", thinkingLevel.name, Icons.Outlined.AutoAwesome)
                SettingRow("Voice", voiceState.status.name, Icons.Outlined.Mic)
                Spacer(Modifier.height(8.dp))
                Text("AI Provider", color = VynnraText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = providerBaseUrl,
                    onValueChange = { providerBaseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = providerApiKey,
                    onValueChange = { providerApiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key (leave blank to keep saved key)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = providerModel,
                    onValueChange = { providerModel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (providerConfig.isConfigured()) "Provider configured" else "Provider not configured",
                    color = if (providerConfig.isConfigured()) VynnraPurpleSoft else VynnraMuted
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = {
                        val key = providerApiKey.trim().ifBlank { providerConfig.apiKey }
                        onSaveProviderConfig(
                            ProviderConfig(
                                baseUrl = providerBaseUrl,
                                apiKey = key,
                                model = providerModel
                            )
                        )
                        providerApiKey = ""
                    }) { Text("Save provider") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { settingsOpen = false }) { Text("Done") }
                }
            }
        }

        if (permissionOpen) {
            ModalPanel("Permission Center", { permissionOpen = false }) {
                PermissionLine("Screen Capture", screenCaptureGranted)
                if (!screenCaptureGranted) {
                    Button(onClick = onRequestScreenCapture, modifier = Modifier.fillMaxWidth()) { Text("Grant Screen Capture") }
                }
                PermissionLine("Accessibility Control", false)
                PermissionLine("Files", false)
                PermissionLine("Overlay", false)
                PermissionLine("Microphone", microphoneGranted)
                if (!microphoneGranted) {
                    Button(onClick = onRequestMicrophone, modifier = Modifier.fillMaxWidth()) { Text("Grant Microphone") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Capabilities remain user-controlled by Android.", color = VynnraMuted)
            }
        }
    }

    if (phase9Open) {
        Phase9Center(memoryRepository, taskRepository) { phase9Open = false }
    }
}

@Composable
private fun TopBar(title: String, thinkingLevel: ThinkingLevel, onMenu: () -> Unit, onThinkingChange: (ThinkingLevel) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenu) { Icon(Icons.Outlined.Menu, contentDescription = "Open sidebar", tint = VynnraText) }
        Column(Modifier.weight(1f)) {
            Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.Bold)
            Text(title, color = VynnraMuted)
        }
        ThinkingSelector(thinkingLevel, onThinkingChange)
    }
}

@Composable
private fun ThinkingSelector(selected: ThinkingLevel, onSelected: (ThinkingLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { expanded = true }) {
            Text(selected.name, color = VynnraPurpleSoft, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThinkingLevel.entries.forEach { level ->
                DropdownMenuItem(text = { Text(level.name) }, onClick = { onSelected(level); expanded = false })
            }
        }
    }
}

@Composable
private fun StatusStrip(voiceState: VoiceUiState, agentState: VoiceAgentUiState) {
    val statusText = when {
        voiceState.status == VoiceStatus.LISTENING -> "Listening — speak naturally"
        voiceState.status == VoiceStatus.SPEAKING -> "Speaking — Vynnra is reading the response"
        agentState.busy -> "Thinking — processing your request"
        agentState.error != null -> "AI error — check provider settings"
        voiceState.status == VoiceStatus.ERROR -> "Voice error — ${voiceState.errorMessage ?: "check permissions"}"
        voiceState.status == VoiceStatus.UNSUPPORTED -> "Voice unavailable on this device"
        else -> "Ready — memory, tasks, voice, and AI provider enabled"
    }
    Surface(color = VynnraPanel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = VynnraPurple.copy(alpha = 0.18f), shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(8.dp))
            Text(statusText, color = VynnraMuted)
        }
    }
}

@Composable
private fun MessageArea(messages: List<AgentMessage>, modifier: Modifier) {
    if (messages.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = VynnraPanelElevated, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("V", color = VynnraPurple, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(14.dp))
                Text("How can Vynnra help?", color = VynnraText, fontWeight = FontWeight.Bold)
                Text("Ask, plan, research, control Android, or speak to Vynnra.", color = VynnraMuted)
            }
        }
    } else {
        LazyColumn(modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (item.role == AgentMessageRole.USER) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (item.role == AgentMessageRole.USER) VynnraPanelElevated else VynnraPanel, shape = RoundedCornerShape(18.dp)) {
                        Text(item.text, color = VynnraText, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Drawer(
    sessions: List<AgentSession>,
    selectedSession: Int,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSession: (Int) -> Unit,
    onPermissions: () -> Unit,
    onMemoryTasks: () -> Unit,
    onSettings: () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        Surface(color = VynnraPanel, modifier = Modifier.width(310.dp).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Vynnra", color = VynnraText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted) }
                }
                Surface(color = VynnraPurple.copy(alpha = 0.14f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onNewChat() }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AddComment, null, tint = VynnraPurple)
                        Spacer(Modifier.width(10.dp))
                        Text("New Chat", color = VynnraText)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Chats", color = VynnraMuted)
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sessions) { session ->
                        Surface(color = if (session.id == selectedSession) VynnraPanelElevated else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onSession(session.id) }) {
                            Text(session.title, color = VynnraText, modifier = Modifier.padding(11.dp))
                        }
                    }
                }
                DrawerAction("Memory & Tasks", Icons.Outlined.Memory, onMemoryTasks)
                DrawerAction("Permission Center", Icons.Outlined.Security, onPermissions)
                DrawerAction("Settings", Icons.Outlined.Settings, onSettings)
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.52f)).clickable { onClose() })
    }
}

@Composable
private fun DrawerAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraMuted)
        Spacer(Modifier.width(10.dp))
        Text(label, color = VynnraText)
    }
}

@Composable
private fun ModalPanel(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
        Surface(color = VynnraPanel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = VynnraText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted) }
                }
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraPurple)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VynnraText)
            Text(value, color = VynnraMuted)
        }
    }
}
