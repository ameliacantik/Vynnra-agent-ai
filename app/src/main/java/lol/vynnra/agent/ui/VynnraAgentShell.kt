package lol.vynnra.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.ProviderConfig
import lol.vynnra.agent.core.voice.VoiceAgentUiState
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository
import lol.vynnra.agent.platform.AudioRecorderState
import lol.vynnra.agent.platform.AudioRecording
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
    accessibilityGranted: Boolean,
    fullStorageGranted: Boolean,
    overlayGranted: Boolean,
    microphoneGranted: Boolean,
    notificationGranted: Boolean,
    onRequestScreenCapture: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenStorageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    voiceState: VoiceUiState,
    voiceAgentState: VoiceAgentUiState,
    recordingState: AudioRecorderState,
    recordings: List<AudioRecording>,
    providerConfig: ProviderConfig,
    availableModels: List<String>,
    providerStatus: String,
    providerBusy: Boolean,
    onRequestMicrophone: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayRecording: (AudioRecording) -> Unit,
    onStopPlayback: () -> Unit,
    onDeleteRecording: (AudioRecording) -> Unit,
    onRefreshPermissions: () -> Unit,
    onRefreshModels: () -> Unit,
    onTestProvider: () -> Unit,
    onStopAgent: () -> Unit,
    onSendMessage: (String, ThinkingLevel, Boolean) -> Unit,
    onSaveProviderConfig: (ProviderConfig) -> Unit
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var permissionOpen by remember { mutableStateOf(false) }
    var memoryTasksOpen by remember { mutableStateOf(false) }
    var recordingsOpen by remember { mutableStateOf(false) }
    var thinkingLevel by remember { mutableStateOf(ThinkingLevel.MAX) }
    var message by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf(1) }
    val sessions = remember { mutableStateListOf(AgentSession(1, "New Chat")) }
    val messages = remember { mutableStateListOf<AgentMessage>() }
    val listState = rememberLazyListState()

    var providerBaseUrl by remember(providerConfig.baseUrl) { mutableStateOf(providerConfig.baseUrl) }
    var providerApiKey by remember { mutableStateOf("") }
    var providerModel by remember(providerConfig.model) { mutableStateOf(providerConfig.model) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

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
                providerConnected = providerStatus.startsWith("Connected"),
                onMenu = { drawerOpen = true }
            )

            AgentActivityStrip(voiceState = voiceState, agentState = voiceAgentState, providerStatus = providerStatus)

            if (messages.isEmpty()) {
                WelcomeArea(
                    thinkingLevel = thinkingLevel,
                    onThinkingChange = { thinkingLevel = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(messages, key = { index, item -> "${index}-${item.role}-${item.text.take(24)}" }) { index, item ->
                        AnimatedMessage(index = index, message = item)
                    }
                    if (voiceAgentState.busy) {
                        item(key = "thinking-indicator") {
                            ThinkingIndicator(voiceAgentState.activity ?: "Thinking…")
                        }
                    }
                }
            }

            if (voiceState.partialText.isNotBlank()) {
                LiveTranscript(voiceState.partialText)
            }

            if (recordingState.recording) {
                RecordingBanner(recordingState)
            }

            if (voiceState.errorMessage != null || voiceAgentState.error != null) {
                ErrorBanner(voiceState.errorMessage ?: voiceAgentState.error ?: "Unknown error")
            }

            Composer(
                message = message,
                onMessageChange = { message = it },
                thinkingLevel = thinkingLevel,
                onThinkingChange = { thinkingLevel = it },
                voiceStatus = voiceState.status,
                microphoneGranted = microphoneGranted,
                recordingState = recordingState,
                agentBusy = voiceAgentState.busy,
                onRequestMicrophone = onRequestMicrophone,
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onStopSpeaking = onStopSpeaking,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onStopAgent = onStopAgent,
                onSend = {
                    val trimmed = message.trim()
                    if (trimmed.isNotEmpty() && !voiceAgentState.busy) {
                        if (messages.isEmpty() && sessions.any { it.id == selectedSession }) {
                            val index = sessions.indexOfFirst { it.id == selectedSession }
                            if (index >= 0) sessions[index] = sessions[index].copy(
                                title = trimmed.take(36).ifBlank { "New Chat" }
                            )
                        }
                        messages += AgentMessage(AgentMessageRole.USER, trimmed)
                        message = ""
                        onSendMessage(trimmed, thinkingLevel, false)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            Drawer(
                sessions = sessions,
                selectedSession = selectedSession,
                providerStatus = providerStatus,
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
                onMemoryTasks = { memoryTasksOpen = true; drawerOpen = false },
                onSettings = { settingsOpen = true; drawerOpen = false },
                onRecordings = { recordingsOpen = true; drawerOpen = false }
            )
        }
    }

    if (settingsOpen) {
        SettingsPanel(
            providerBaseUrl = providerBaseUrl,
            providerApiKey = providerApiKey,
            providerModel = providerModel,
            availableModels = availableModels,
            providerStatus = providerStatus,
            providerBusy = providerBusy,
            microphoneGranted = microphoneGranted,
            recordingState = recordingState,
            thinkingLevel = thinkingLevel,
            onProviderBaseUrlChange = { providerBaseUrl = it },
            onProviderApiKeyChange = { providerApiKey = it },
            onProviderModelChange = { providerModel = it },
            onSelectModel = {
                providerModel = it
                modelMenuOpen = false
            },
            modelMenuOpen = modelMenuOpen,
            onModelMenuChange = { modelMenuOpen = it },
            onRefreshModels = onRefreshModels,
            onTestProvider = onTestProvider,
            onSaveProvider = {
                onSaveProviderConfig(
                    ProviderConfig(
                        baseUrl = providerBaseUrl,
                        apiKey = providerApiKey.trim().ifBlank { providerConfig.apiKey },
                        model = providerModel
                    )
                )
                providerApiKey = ""
            },
            onClose = { settingsOpen = false }
        )
    }

    if (permissionOpen) {
        PermissionCenter(
            screenCaptureGranted = screenCaptureGranted,
            accessibilityGranted = accessibilityGranted,
            fullStorageGranted = fullStorageGranted,
            overlayGranted = overlayGranted,
            microphoneGranted = microphoneGranted,
            notificationGranted = notificationGranted,
            onRequestScreenCapture = onRequestScreenCapture,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenStorageSettings = onOpenStorageSettings,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onRequestMicrophone = onRequestMicrophone,
            onRequestNotifications = onRequestNotifications,
            onRefreshPermissions = onRefreshPermissions,
            onClose = { permissionOpen = false }
        )
    }

    if (memoryTasksOpen) {
        Phase9Center(memoryRepository, taskRepository) { memoryTasksOpen = false }
    }

    if (recordingsOpen) {
        RecordingsPanel(
            recordings = recordings,
            state = recordingState,
            onStart = onStartRecording,
            onStop = onStopRecording,
            onPlay = onPlayRecording,
            onStopPlayback = onStopPlayback,
            onDelete = onDeleteRecording,
            onClose = { recordingsOpen = false }
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    providerConnected: Boolean,
    onMenu: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Outlined.Menu, contentDescription = "Open sidebar", tint = VynnraText)
        }
        VynnraLogo(Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.Bold)
            Text(title, color = VynnraMuted)
        }
        ConnectionBadge(connected = providerConnected)
    }
}

@Composable
private fun VynnraLogo(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "logo-pulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "logo-scale"
    )
    Box(
        modifier
            .scale(pulse.value)
            .background(
                Brush.linearGradient(listOf(VynnraPurple, Color(0xFF5E34B8))),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("V", color = Color.White, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ConnectionBadge(connected: Boolean) {
    val tint by animateColorAsState(
        targetValue = if (connected) VynnraPurpleSoft else VynnraMuted,
        animationSpec = tween(300),
        label = "connection-color"
    )
    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = tint.copy(alpha = 0.8f), shape = CircleShape, modifier = Modifier.size(6.dp)) {}
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "ONLINE" else "SETUP", color = tint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AgentActivityStrip(
    voiceState: VoiceUiState,
    agentState: VoiceAgentUiState,
    providerStatus: String
) {
    val statusText = when {
        voiceState.status == VoiceStatus.LISTENING -> "Listening — speak naturally"
        voiceState.status == VoiceStatus.SPEAKING -> "Speaking — response audio active"
        agentState.busy -> agentState.activity ?: "Thinking — Vynnra is working"
        agentState.error != null -> "Agent error — open Settings to inspect provider"
        providerStatus.startsWith("Connected") -> "Ready — agent, voice, tools and provider connected"
        else -> "Ready — configure a provider to start chatting"
    }

    Surface(
        color = VynnraPanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulse = rememberInfiniteTransition(label = "status-pulse").animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "status-alpha"
            )
            Surface(
                color = VynnraPurple.copy(alpha = pulse.value),
                shape = CircleShape,
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(statusText, color = VynnraMuted)
        }
    }
}

@Composable
private fun WelcomeArea(
    thinkingLevel: ThinkingLevel,
    onThinkingChange: (ThinkingLevel) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VynnraLogo(Modifier.size(92.dp))
        Spacer(Modifier.height(18.dp))
        Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Your personal AI agent for chat, voice, Android control, browser tasks, files, memory and automation.",
            color = VynnraMuted
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeaturePill("Chat")
            FeaturePill("Voice")
            FeaturePill("Tools")
            FeaturePill("Memory")
        }
        Spacer(Modifier.height(18.dp))
        ThinkingChip(
            selected = thinkingLevel,
            onSelected = onThinkingChange,
            compact = false
        )
    }
}

@Composable
private fun FeaturePill(text: String) {
    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(14.dp)) {
        Text(text, color = VynnraPurpleSoft, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
private fun AnimatedMessage(index: Int, message: AgentMessage) {
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(280, delayMillis = index.coerceAtMost(6) * 35),
        label = "message-alpha"
    )
    Row(
        Modifier.fillMaxWidth().alpha(alpha),
        horizontalArrangement = if (message.role == AgentMessageRole.USER) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.role == AgentMessageRole.USER) VynnraPanelElevated else VynnraPanel,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.role == AgentMessageRole.USER) 18.dp else 5.dp,
                bottomEnd = if (message.role == AgentMessageRole.USER) 5.dp else 18.dp
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    if (message.role == AgentMessageRole.USER) "You" else "Vynnra",
                    color = VynnraPurpleSoft,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(message.text, color = VynnraText)
            }
        }
    }
}

@Composable
private fun ThinkingIndicator(activity: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
    ) {
        val transition = rememberInfiniteTransition(label = "thinking-dots")
        repeat(3) { index ->
            val scale = transition.animateFloat(
                initialValue = 0.65f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = index * 120, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot-$index"
            )
            Surface(
                color = VynnraPurple,
                shape = CircleShape,
                modifier = Modifier.size(7.dp).scale(scale.value)
            ) {}
            Spacer(Modifier.width(5.dp))
        }
        Text(activity, color = VynnraMuted)
    }
}

@Composable
private fun LiveTranscript(text: String) {
    Surface(
        color = VynnraPanelElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Text("Listening: $text", color = VynnraMuted, modifier = Modifier.padding(11.dp))
    }
}

@Composable
private fun RecordingBanner(state: AudioRecorderState) {
    val seconds = (state.durationMs / 1000).coerceAtLeast(0)
    Surface(
        color = VynnraPurple.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.RecordVoiceOver, null, tint = VynnraPurple)
            Spacer(Modifier.width(8.dp))
            Text("Recording • %02d:%02d".format(seconds / 60, seconds % 60), color = VynnraText)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = Color(0xFF25141D),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Text(message, color = Color(0xFFFFA7C7), modifier = Modifier.padding(11.dp))
    }
}

@Composable
private fun Composer(
    message: String,
    onMessageChange: (String) -> Unit,
    thinkingLevel: ThinkingLevel,
    onThinkingChange: (ThinkingLevel) -> Unit,
    voiceStatus: VoiceStatus,
    microphoneGranted: Boolean,
    recordingState: AudioRecorderState,
    agentBusy: Boolean,
    onRequestMicrophone: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStopAgent: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThinkingChip(
                selected = thinkingLevel,
                onSelected = onThinkingChange,
                compact = true
            )
            ActionChip("Record", Icons.Outlined.RecordVoiceOver, recordingState.recording, if (recordingState.recording) onStopRecording else onStartRecording)
            ActionChip("Voice", if (voiceStatus == VoiceStatus.LISTENING) Icons.Outlined.MicOff else Icons.Outlined.Mic, voiceStatus == VoiceStatus.LISTENING, {
                when {
                    voiceStatus == VoiceStatus.LISTENING -> onStopVoice()
                    voiceStatus == VoiceStatus.SPEAKING -> onStopSpeaking()
                    !microphoneGranted -> onRequestMicrophone()
                    else -> onStartVoice()
                }
            })
        }

        Spacer(Modifier.height(7.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Vynnra anything…") },
                shape = RoundedCornerShape(22.dp),
                maxLines = 6
            )
            Spacer(Modifier.width(6.dp))
            if (agentBusy) {
                IconButton(onClick = onStopAgent) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = "Stop agent", tint = VynnraPurple)
                }
            } else {
                IconButton(onClick = onSend) {
                    Icon(Icons.Outlined.Send, contentDescription = "Send", tint = VynnraPurple)
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (active) VynnraPurpleSoft else VynnraMuted,
        animationSpec = tween(250),
        label = "chip-$label"
    )
    Surface(
        color = if (active) VynnraPurple.copy(alpha = 0.16f) else VynnraPanelElevated,
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = tint, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ThinkingChip(
    selected: ThinkingLevel,
    onSelected: (ThinkingLevel) -> Unit,
    compact: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = VynnraPanelElevated,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = VynnraPurple)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (compact) "Thinking • ${selected.name}" else "Thinking · ${selected.name}",
                    color = VynnraPurpleSoft,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Outlined.ExpandMore, null, tint = VynnraMuted)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThinkingLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(level.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                when (level) {
                                    ThinkingLevel.LOW -> "Fast plans, minimal tool steps"
                                    ThinkingLevel.MEDIUM -> "Balanced planning and execution"
                                    ThinkingLevel.HIGH -> "Deeper planning for complex tasks"
                                    ThinkingLevel.MAX -> "Maximum planned depth and recovery"
                                },
                                color = VynnraMuted
                            )
                        }
                    },
                    onClick = {
                        onSelected(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Drawer(
    sessions: List<AgentSession>,
    selectedSession: Int,
    providerStatus: String,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSession: (Int) -> Unit,
    onPermissions: () -> Unit,
    onMemoryTasks: () -> Unit,
    onSettings: () -> Unit,
    onRecordings: () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        Surface(color = VynnraPanel, modifier = Modifier.width(326.dp).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(15.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    VynnraLogo(Modifier.size(38.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.ExtraBold)
                        Text("Full feature workspace", color = VynnraMuted)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted)
                    }
                }

                Spacer(Modifier.height(12.dp))
                DrawerHero(providerStatus)

                Spacer(Modifier.height(12.dp))
                DrawerAction("New Chat", Icons.Outlined.AddComment, onNewChat)
                DrawerAction("Memory & Tasks", Icons.Outlined.Memory, onMemoryTasks)
                DrawerAction("Recordings", Icons.Outlined.RecordVoiceOver, onRecordings)
                DrawerAction("Permission Center", Icons.Outlined.Security, onPermissions)
                DrawerAction("Settings & Provider", Icons.Outlined.Settings, onSettings)

                Spacer(Modifier.height(10.dp))
                Text("Chats", color = VynnraMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        Surface(
                            color = if (session.id == selectedSession) VynnraPanelElevated else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSession(session.id) }
                        ) {
                            Text(session.title, color = VynnraText, modifier = Modifier.padding(11.dp))
                        }
                    }
                }

                Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(11.dp)) {
                        Text("Vynnra modules", color = VynnraText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        ModuleLine(Icons.Outlined.Bolt, "Agent Orchestrator")
                        ModuleLine(Icons.Outlined.Folder, "Files & Browser")
                        ModuleLine(Icons.Outlined.Language, "Web Search")
                        ModuleLine(Icons.Outlined.PowerSettingsNew, "Android Control")
                        ModuleLine(Icons.Outlined.Memory, "Memory & Tasks")
                    }
                }
            }
        }
        Box(
            Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.6f)).clickable { onClose() }
        )
    }
}

@Composable
private fun DrawerHero(providerStatus: String) {
    Surface(
        color = Brush.linearGradient(
            listOf(VynnraPurple.copy(alpha = 0.2f), VynnraPanelElevated)
        ).let { Color.Transparent },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().background(
            Brush.linearGradient(
                listOf(VynnraPurple.copy(alpha = 0.18f), VynnraPanelElevated)
            ),
            RoundedCornerShape(18.dp)
        )
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Wifi, null, tint = VynnraPurple)
                Spacer(Modifier.width(8.dp))
                Text(if (providerStatus.startsWith("Connected")) "Provider connected" else "Provider needs setup", color = VynnraText, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Text(providerStatus, color = VynnraMuted)
        }
    }
}

@Composable
private fun ModuleLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraPurpleSoft, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = VynnraMuted)
    }
}

@Composable
private fun DrawerAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = VynnraMuted)
        Spacer(Modifier.width(11.dp))
        Text(label, color = VynnraText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsPanel(
    providerBaseUrl: String,
    providerApiKey: String,
    providerModel: String,
    availableModels: List<String>,
    providerStatus: String,
    providerBusy: Boolean,
    microphoneGranted: Boolean,
    recordingState: AudioRecorderState,
    thinkingLevel: ThinkingLevel,
    onProviderBaseUrlChange: (String) -> Unit,
    onProviderApiKeyChange: (String) -> Unit,
    onProviderModelChange: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    modelMenuOpen: Boolean,
    onModelMenuChange: (Boolean) -> Unit,
    onRefreshModels: () -> Unit,
    onTestProvider: () -> Unit,
    onSaveProvider: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)), contentAlignment = Alignment.Center) {
        Surface(
            color = VynnraPanel,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Tune, null, tint = VynnraPurple)
                    Spacer(Modifier.width(9.dp))
                    Text("Settings & Provider", color = VynnraText, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted)
                    }
                }

                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        SettingsSection(
                            icon = Icons.Outlined.Wifi,
                            title = "AI Provider",
                            subtitle = "OpenAI-compatible endpoint with automatic /models discovery"
                        ) {
                            OutlinedTextField(
                                value = providerBaseUrl,
                                onValueChange = onProviderBaseUrlChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Provider Base URL") },
                                supportingText = { Text("Examples: https://host/v1 or https://host") },
                                singleLine = true
                            )
                            Spacer(Modifier.height(7.dp))
                            OutlinedTextField(
                                value = providerApiKey,
                                onValueChange = onProviderApiKeyChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("API Key") },
                                placeholder = { Text("Optional for local providers") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Spacer(Modifier.height(7.dp))
                            Box {
                                OutlinedTextField(
                                    value = providerModel,
                                    onValueChange = onProviderModelChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Model") },
                                    supportingText = { Text(if (availableModels.isEmpty()) "No models fetched yet." else "${availableModels.size} models available") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { onModelMenuChange(!modelMenuOpen) }) {
                                            Icon(Icons.Outlined.ExpandMore, null, tint = VynnraMuted)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = modelMenuOpen,
                                    onDismissRequest = { onModelMenuChange(false) }
                                ) {
                                    availableModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = { onSelectModel(model) }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onRefreshModels,
                                    enabled = !providerBusy
                                ) {
                                    Icon(Icons.Outlined.Refresh, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (providerBusy) "Fetching…" else "Refresh models")
                                }
                                OutlinedButton(
                                    onClick = onTestProvider,
                                    enabled = !providerBusy
                                ) {
                                    Icon(Icons.Outlined.Wifi, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Test")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                providerStatus,
                                color = if (providerStatus.startsWith("Connected")) VynnraPurpleSoft else VynnraMuted
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Agent",
                            subtitle = "Execution behavior and safety controls"
                        ) {
                            SettingLine("Thinking control", "Live in the chat composer", Icons.Outlined.AutoAwesome)
                            SettingLine("Current level", thinkingLevel.name, Icons.Outlined.MoreHoriz)
                            SettingLine("Verification", "Observe → Act → Verify → Recover", Icons.Outlined.Security)
                            SettingLine("Emergency stop", "Available beside the composer while running", Icons.Outlined.StopCircle)
                        }
                    }

                    item {
                        SettingsSection(
                            icon = Icons.Outlined.RecordVoiceOver,
                            title = "Voice & Recording",
                            subtitle = "Speech recognition, spoken responses, and audio files"
                        ) {
                            SettingLine("Microphone", if (microphoneGranted) "Granted" else "Not granted", Icons.Outlined.Mic)
                            SettingLine("Recording", if (recordingState.recording) "Recording now" else "Ready", Icons.Outlined.RecordVoiceOver)
                            if (recordingState.error != null) {
                                Text(recordingState.error, color = Color(0xFFFFA7C7))
                            }
                        }
                    }

                    item {
                        SettingsSection(
                            icon = Icons.Outlined.Storage,
                            title = "Workspace",
                            subtitle = "Memory, tasks, files, browser, permissions and local recordings"
                        ) {
                            SettingLine("Theme", "AMOLED black + violet motion UI", Icons.Outlined.AutoAwesome)
                            SettingLine("App recordings", "Stored privately in Vynnra app storage", Icons.Outlined.Folder)
                            SettingLine("Tool control", "Capabilities appear only after Android permissions are granted", Icons.Outlined.Security)
                        }
                    }

                    item {
                        SettingsSection(
                            icon = Icons.Outlined.PowerSettingsNew,
                            title = "About",
                            subtitle = "Vynnra Agent AI · 0.2"
                        ) {
                            Text(
                                "Built as a full agent workspace rather than a minimal chat shell. Custom providers, tool orchestration, voice, recording, memory and Android controls are integrated.",
                                color = VynnraMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSaveProvider,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VynnraPurple)
                ) {
                    Text("Save provider & apply")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = VynnraPurple)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = VynnraText, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = VynnraMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingLine(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraPurpleSoft, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VynnraText)
            Text(value, color = VynnraMuted)
        }
    }
}

@Composable
private fun PermissionCenter(
    screenCaptureGranted: Boolean,
    accessibilityGranted: Boolean,
    fullStorageGranted: Boolean,
    overlayGranted: Boolean,
    microphoneGranted: Boolean,
    notificationGranted: Boolean,
    onRequestScreenCapture: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenStorageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestMicrophone: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onClose: () -> Unit
) {
    ModalPanel("Permission Center", onClose) {
        PermissionLine("Screen Capture", screenCaptureGranted)
        if (!screenCaptureGranted) PermissionButton("Grant", onRequestScreenCapture)

        PermissionLine("Accessibility Control", accessibilityGranted)
        PermissionButton("Open Android Settings", onOpenAccessibilitySettings, enabled = !accessibilityGranted)

        PermissionLine("Full Shared Storage", fullStorageGranted)
        PermissionButton("Open Android Settings", onOpenStorageSettings, enabled = !fullStorageGranted)

        PermissionLine("Overlay", overlayGranted)
        PermissionButton("Open Android Settings", onOpenOverlaySettings, enabled = !overlayGranted)

        PermissionLine("Microphone", microphoneGranted)
        if (!microphoneGranted) PermissionButton("Grant", onRequestMicrophone)

        PermissionLine("Notifications", notificationGranted)
        if (!notificationGranted) PermissionButton("Grant", onRequestNotifications)

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRefreshPermissions, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("Refresh permission state")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Android remains the final capability boundary. Vynnra only uses access that you explicitly grant.",
            color = VynnraMuted
        )
    }
}

@Composable
private fun PermissionButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun RecordingsPanel(
    recordings: List<AudioRecording>,
    state: AudioRecorderState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPlay: (AudioRecording) -> Unit,
    onStopPlayback: () -> Unit,
    onDelete: (AudioRecording) -> Unit,
    onClose: () -> Unit
) {
    ModalPanel("Recordings", onClose) {
        Text(
            if (state.recording) "Recording active — tap Stop to save the file." else "Audio recordings are stored privately inside Vynnra.",
            color = VynnraMuted
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = if (state.recording) onStop else onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (state.recording) Color(0xFF8D2945) else VynnraPurple)
        ) {
            Icon(if (state.recording) Icons.Outlined.StopCircle else Icons.Outlined.RecordVoiceOver, null)
            Spacer(Modifier.width(7.dp))
            Text(if (state.recording) "Stop & Save" else "Start recording")
        }
        Spacer(Modifier.height(10.dp))
        if (recordings.isEmpty()) {
            Text("No recordings yet.", color = VynnraMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(280.dp)) {
                items(recordings, key = { it.path }) { recording ->
                    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(recording.name, color = VynnraText, maxLines = 1)
                                Text(
                                    "${recording.sizeBytes / 1024} KB",
                                    color = VynnraMuted
                                )
                            }
                            if (state.playingPath == recording.path) {
                                IconButton(onClick = onStopPlayback) {
                                    Icon(Icons.Outlined.StopCircle, null, tint = VynnraPurple)
                                }
                            } else {
                                IconButton(onClick = { onPlay(recording) }) {
                                    Icon(Icons.Outlined.PlayArrow, null, tint = VynnraPurple)
                                }
                            }
                            IconButton(onClick = { onDelete(recording) }) {
                                Icon(Icons.Outlined.DeleteOutline, null, tint = VynnraMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalPanel(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.76f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = VynnraPanel,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.93f)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = VynnraText, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted)
                    }
                }
                Spacer(Modifier.height(9.dp))
                content()
            }
        }
    }
}
