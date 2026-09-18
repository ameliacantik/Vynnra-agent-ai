package lol.vynnra.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import lol.vynnra.agent.core.agent.AgentStatus
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.orchestrator.AgentActivityEvent
import lol.vynnra.agent.core.orchestrator.OrchestratorState
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

private data class V2Session(val id: Long, val title: String)
private data class V2Message(val id: Long, val role: V2Role, val text: String)
private enum class V2Role { USER, ASSISTANT }

@Composable
fun VynnraAgentShellV2(
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
    orchestratorState: OrchestratorState,
    agentActivity: List<AgentActivityEvent>,
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
    var selectedSession by remember { mutableStateOf(1L) }
    var activityExpanded by remember { mutableStateOf(true) }

    val sessions = remember { mutableStateListOf(V2Session(1L, "New Chat")) }
    val messages = remember { mutableStateListOf<V2Message>() }
    val listState = rememberLazyListState()

    var providerBaseUrl by remember(providerConfig.baseUrl) { mutableStateOf(providerConfig.baseUrl) }
    var providerApiKey by remember { mutableStateOf("") }
    var providerModel by remember(providerConfig.model) { mutableStateOf(providerConfig.model) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, agentActivity.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(voiceState.finalText) {
        val transcript = voiceState.finalText.trim()
        if (transcript.isNotEmpty() && !voiceAgentState.busy) {
            message = transcript
            sendV2Message(
                text = transcript,
                sessions = sessions,
                selectedSession = selectedSession,
                messages = messages,
                thinkingLevel = thinkingLevel,
                onSend = onSendMessage,
                setMessage = { message = it }
            )
        }
    }

    LaunchedEffect(voiceAgentState.replyId) {
        val reply = voiceAgentState.reply
        if (voiceAgentState.replyId != 0L && !reply.isNullOrBlank()) {
            messages += V2Message(System.nanoTime(), V2Role.ASSISTANT, reply)
            if (messages.size > 140) repeat(messages.size - 140) { messages.removeAt(0) }
            activityExpanded = false
        }
    }

    LaunchedEffect(agentActivity.size) {
        if (voiceAgentState.busy) activityExpanded = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07050B), VynnraBlack, Color(0xFF090610))
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            V2TopBar(
                title = sessions.firstOrNull { it.id == selectedSession }?.title ?: "New Chat",
                model = providerConfig.model.ifBlank { "No model selected" },
                providerConnected = providerStatus.startsWith("Connected"),
                onMenu = { drawerOpen = true }
            )

            if (messages.isEmpty()) {
                V2Welcome(
                    providerConnected = providerStatus.startsWith("Connected"),
                    model = providerConfig.model,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 10.dp,
                        bottom = 12.dp
                    )
                ) {
                    itemsIndexed(
                        items = messages,
                        key = { _, item -> item.id }
                    ) { _, item ->
                        V2MessageBubble(item)
                    }

                    if (agentActivity.isNotEmpty()) {
                        item(key = "agent-activity") {
                            AgentActivityCard(
                                events = agentActivity.takeLast(12),
                                state = orchestratorState,
                                busy = voiceAgentState.busy,
                                expanded = activityExpanded,
                                onExpand = { activityExpanded = !activityExpanded }
                            )
                        }
                    }
                }
            }

            if (voiceState.partialText.isNotBlank()) V2Transcript(voiceState.partialText)
            if (recordingState.recording) V2RecordingBanner(recordingState)

            if (voiceState.errorMessage != null || voiceAgentState.error != null) {
                V2ErrorBanner(voiceState.errorMessage ?: voiceAgentState.error ?: "Unexpected error")
            }

            V2Composer(
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
                    if (!voiceAgentState.busy) {
                        sendV2Message(
                            text = message,
                            sessions = sessions,
                            selectedSession = selectedSession,
                            messages = messages,
                            thinkingLevel = thinkingLevel,
                            onSend = onSendMessage,
                            setMessage = { message = it }
                        )
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(160)),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(120))
        ) {
            V2Drawer(
                sessions = sessions,
                selectedSession = selectedSession,
                providerStatus = providerStatus,
                onClose = { drawerOpen = false },
                onNewChat = {
                    val next = (sessions.maxOfOrNull { it.id } ?: 0L) + 1L
                    sessions += V2Session(next, "New Chat")
                    selectedSession = next
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
        V2SettingsPanel(
            providerBaseUrl = providerBaseUrl,
            providerApiKey = providerApiKey,
            providerModel = providerModel,
            availableModels = availableModels,
            providerStatus = providerStatus,
            providerBusy = providerBusy,
            thinkingLevel = thinkingLevel,
            microphoneGranted = microphoneGranted,
            recordingState = recordingState,
            modelMenuOpen = modelMenuOpen,
            onProviderBaseUrlChange = { providerBaseUrl = it },
            onProviderApiKeyChange = { providerApiKey = it },
            onProviderModelChange = { providerModel = it },
            onSelectModel = { providerModel = it; modelMenuOpen = false },
            onModelMenuChange = { modelMenuOpen = it },
            onRefreshModels = onRefreshModels,
            onTestProvider = onTestProvider,
            onSaveProvider = {
                onSaveProviderConfig(
                    ProviderConfig(
                        baseUrl = providerBaseUrl.trim(),
                        apiKey = providerApiKey.trim().ifBlank { providerConfig.apiKey },
                        model = providerModel.trim()
                    )
                )
                providerApiKey = ""
            },
            onClose = { settingsOpen = false }
        )
    }

    if (permissionOpen) {
        V2PermissionCenter(
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
            onRefresh = onRefreshPermissions,
            onClose = { permissionOpen = false }
        )
    }

    if (memoryTasksOpen) V2MemoryTasksPanel { memoryTasksOpen = false }

    if (recordingsOpen) {
        V2RecordingsPanel(
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

private fun sendV2Message(
    text: String,
    sessions: MutableList<V2Session>,
    selectedSession: Long,
    messages: MutableList<V2Message>,
    thinkingLevel: ThinkingLevel,
    onSend: (String, ThinkingLevel, Boolean) -> Unit,
    setMessage: (String) -> Unit
) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    val sessionIndex = sessions.indexOfFirst { it.id == selectedSession }
    if (sessionIndex >= 0 && messages.isEmpty()) {
        sessions[sessionIndex] = sessions[sessionIndex].copy(title = trimmed.take(36).ifBlank { "New Chat" })
    }
    messages += V2Message(System.nanoTime(), V2Role.USER, trimmed)
    if (messages.size > 140) repeat(messages.size - 140) { messages.removeAt(0) }
    setMessage("")
    onSend(trimmed, thinkingLevel, false)
}

@Composable
private fun V2TopBar(title: String, model: String, providerConnected: Boolean, onMenu: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xE607060A))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Outlined.Menu, "Open sidebar", tint = VynnraText) }
            V2Logo(34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.ExtraBold)
                Text(title, color = VynnraMuted, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                V2StatusPill(providerConnected)
                Spacer(Modifier.height(2.dp))
                Text(model.take(24), color = VynnraMuted, maxLines = 1)
            }
        }
        Surface(color = VynnraPurple.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = VynnraPurple, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Live safe activity · private reasoning stays private", color = VynnraMuted)
            }
        }
    }
}

@Composable
private fun V2Logo(size: androidx.compose.ui.unit.Dp) {
    val pulse = rememberInfiniteTransition(label = "v2-logo").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "v2-logo-scale"
    )
    Box(
        Modifier.size(size).scale(pulse.value).background(
            Brush.linearGradient(listOf(VynnraPurple, Color(0xFF5A2FC1))),
            CircleShape
        ),
        contentAlignment = Alignment.Center
    ) { Text("V", color = Color.White, fontWeight = FontWeight.Black) }
}

@Composable
private fun V2StatusPill(online: Boolean) {
    Surface(color = if (online) VynnraPurple.copy(alpha = 0.14f) else VynnraPanelElevated, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (online) VynnraPurpleSoft else VynnraMuted, shape = CircleShape, modifier = Modifier.size(6.dp)) {}
            Spacer(Modifier.width(6.dp))
            Text(if (online) "ONLINE" else "SETUP", color = if (online) VynnraPurpleSoft else VynnraMuted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V2Welcome(providerConnected: Boolean, model: String, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        V2Logo(92.dp)
        Spacer(Modifier.height(22.dp))
        Text("What can I do for you?", color = VynnraText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (providerConnected) "Provider connected · " + model else "Connect an OpenAI-compatible provider in Settings.",
            color = VynnraMuted
        )
        Spacer(Modifier.height(16.dp))
        Text("Chat · Android · Browser · Files · Vision · Web · Voice", color = VynnraPurpleSoft)
    }
}

@Composable
private fun V2MessageBubble(message: V2Message) {
    val isUser = message.role == V2Role.USER
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            V2Logo(30.dp)
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            color = if (isUser) VynnraPurple.copy(alpha = 0.16f) else VynnraPanel,
            shape = RoundedCornerShape(
                topStart = 19.dp,
                topEnd = 19.dp,
                bottomStart = if (isUser) 19.dp else 5.dp,
                bottomEnd = if (isUser) 5.dp else 19.dp
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.84f else 0.9f)
        ) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                if (!isUser) {
                    Text("Vynnra", color = VynnraPurpleSoft, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(message.text, color = VynnraText)
            }
        }
    }
}

@Composable
private fun AgentActivityCard(
    events: List<AgentActivityEvent>,
    state: OrchestratorState,
    busy: Boolean,
    expanded: Boolean,
    onExpand: () -> Unit
) {
    val latest = events.lastOrNull()
    val active = busy || state.status in setOf(
        AgentStatus.UNDERSTANDING, AgentStatus.PLANNING, AgentStatus.EXECUTING,
        AgentStatus.VERIFYING, AgentStatus.RECOVERING
    )
    Surface(
        color = VynnraPanel.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onExpand)
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pulse = rememberInfiniteTransition(label = "activity-pulse").animateFloat(
                    initialValue = 0.55f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "activity-alpha"
                )
                Surface(
                    color = (if (active) VynnraPurple else VynnraPurpleSoft).copy(alpha = if (active) pulse.value else 1f),
                    shape = CircleShape,
                    modifier = Modifier.size(9.dp)
                ) {}
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (active) "Vynnra is working" else "Vynnra activity", color = VynnraText, fontWeight = FontWeight.Bold)
                    Text(latest?.title ?: "Ready", color = VynnraMuted, maxLines = 2)
                }
                Icon(if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.MoreHoriz, null, tint = VynnraMuted)
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    events.forEachIndexed { index, event -> ActivityRow(event, index == events.lastIndex) }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(event: AgentActivityEvent, isLast: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = when (event.status) {
                    AgentStatus.COMPLETED -> VynnraPurpleSoft
                    AgentStatus.FAILED -> Color(0xFFE8738B)
                    AgentStatus.CANCELLED -> VynnraMuted
                    else -> VynnraPurple
                },
                shape = CircleShape,
                modifier = Modifier.size(8.dp)
            ) {}
            if (!isLast) {
                Box(Modifier.width(1.dp).height(23.dp).background(VynnraPanelElevated))
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.padding(bottom = 9.dp)) {
            Text(activityLabel(event.status), color = VynnraText, fontWeight = FontWeight.SemiBold)
            Text(event.title, color = VynnraMuted)
        }
    }
}

private fun activityLabel(status: AgentStatus): String = when (status) {
    AgentStatus.UNDERSTANDING -> "Understanding"
    AgentStatus.PLANNING -> "Planning"
    AgentStatus.EXECUTING -> "Action"
    AgentStatus.VERIFYING -> "Verification"
    AgentStatus.RECOVERING -> "Recovery"
    AgentStatus.BLOCKED -> "Permission"
    AgentStatus.COMPLETED -> "Completed"
    AgentStatus.FAILED -> "Failed"
    AgentStatus.CANCELLED -> "Stopped"
    AgentStatus.IDLE -> "Ready"
}

@Composable
private fun V2Composer(
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
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).animateContentSize()
    ) {
        Surface(color = VynnraPanel.copy(alpha = 0.97f), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V2ThinkingSelector(thinkingLevel, onThinkingChange)
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            when {
                                voiceStatus == VoiceStatus.LISTENING -> onStopVoice()
                                voiceStatus == VoiceStatus.SPEAKING -> onStopSpeaking()
                                !microphoneGranted -> onRequestMicrophone()
                                else -> onStartVoice()
                            }
                        }
                    ) {
                        Icon(
                            if (voiceStatus == VoiceStatus.LISTENING) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                            "Voice input",
                            tint = if (voiceStatus == VoiceStatus.LISTENING) VynnraPurpleSoft else VynnraMuted
                        )
                    }
                    IconButton(onClick = { if (recordingState.recording) onStopRecording() else onStartRecording() }) {
                        Icon(Icons.Outlined.RecordVoiceOver, "Record audio", tint = if (recordingState.recording) VynnraPurpleSoft else VynnraMuted)
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Vynnra…") },
                        maxLines = 7,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    if (agentBusy) {
                        IconButton(onClick = onStopAgent) { Icon(Icons.Outlined.StopCircle, "Stop agent", tint = VynnraPurpleSoft) }
                    } else {
                        IconButton(onClick = onSend) { Icon(Icons.Outlined.Send, "Send", tint = VynnraPurpleSoft) }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2ThinkingSelector(selected: ThinkingLevel, onSelected: (ThinkingLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = VynnraPurple.copy(alpha = 0.1f),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = VynnraPurple, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thinking · " + selected.name, color = VynnraPurpleSoft, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.ExpandMore, null, tint = VynnraMuted, modifier = Modifier.size(16.dp))
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
                                    ThinkingLevel.LOW -> "Quick response and light planning"
                                    ThinkingLevel.MEDIUM -> "Balanced planning and execution"
                                    ThinkingLevel.HIGH -> "Deeper plans for complex tasks"
                                    ThinkingLevel.MAX -> "Maximum planning depth"
                                },
                                color = VynnraMuted
                            )
                        }
                    },
                    onClick = { onSelected(level); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun V2Transcript(text: String) {
    Surface(color = VynnraPurple.copy(alpha = 0.09f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)) {
        Text("Listening · " + text, color = VynnraPurpleSoft, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun V2RecordingBanner(state: AudioRecorderState) {
    Surface(color = Color(0x221F1020), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = VynnraPurpleSoft, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(8.dp))
            Text("Recording " + state.fileName.orEmpty(), color = VynnraText, modifier = Modifier.weight(1f))
            Text(formatDuration(state.durationMs), color = VynnraPurpleSoft)
        }
    }
}

@Composable
private fun V2ErrorBanner(message: String) {
    Surface(color = Color(0x331E0D15), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)) {
        Text(message, color = Color(0xFFFFA9C1), modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun V2Drawer(
    sessions: List<V2Session>,
    selectedSession: Long,
    providerStatus: String,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSession: (Long) -> Unit,
    onPermissions: () -> Unit,
    onMemoryTasks: () -> Unit,
    onSettings: () -> Unit,
    onRecordings: () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        Surface(color = VynnraPanel, modifier = Modifier.width(324.dp).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    V2Logo(38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.ExtraBold)
                        Text("Agent workspace", color = VynnraMuted)
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close", tint = VynnraMuted) }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = VynnraPanelElevated,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text(if (providerStatus.startsWith("Connected")) "Provider connected" else "Provider setup", color = VynnraText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(providerStatus, color = VynnraMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                DrawerItem("New Chat", Icons.Outlined.AddComment, onNewChat)
                DrawerItem("Memory & Tasks", Icons.Outlined.Memory, onMemoryTasks)
                DrawerItem("Recordings", Icons.Outlined.RecordVoiceOver, onRecordings)
                DrawerItem("Permission Center", Icons.Outlined.Security, onPermissions)
                DrawerItem("Settings & Provider", Icons.Outlined.Settings, onSettings)
                Spacer(Modifier.height(10.dp))
                Text("Chats", color = VynnraMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        Surface(
                            color = if (session.id == selectedSession) VynnraPanelElevated else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSession(session.id) }
                        ) {
                            Text(session.title, color = VynnraText, modifier = Modifier.padding(10.dp), maxLines = 1)
                        }
                    }
                }
                Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(11.dp)) {
                        Text("Vynnra modules", color = VynnraText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        ModuleLine(Icons.Outlined.Bolt, "Agent Orchestrator")
                        ModuleLine(Icons.Outlined.PowerSettingsNew, "Android Control")
                        ModuleLine(Icons.Outlined.Folder, "Files & Browser")
                        ModuleLine(Icons.Outlined.Language, "Web Search")
                        ModuleLine(Icons.Outlined.Memory, "Memory & Tasks")
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.66f)).clickable(onClick = onClose))
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraMuted)
        Spacer(Modifier.width(11.dp))
        Text(label, color = VynnraText, fontWeight = FontWeight.Medium)
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
private fun V2SettingsPanel(
    providerBaseUrl: String,
    providerApiKey: String,
    providerModel: String,
    availableModels: List<String>,
    providerStatus: String,
    providerBusy: Boolean,
    thinkingLevel: ThinkingLevel,
    microphoneGranted: Boolean,
    recordingState: AudioRecorderState,
    modelMenuOpen: Boolean,
    onProviderBaseUrlChange: (String) -> Unit,
    onProviderApiKeyChange: (String) -> Unit,
    onProviderModelChange: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onModelMenuChange: (Boolean) -> Unit,
    onRefreshModels: () -> Unit,
    onTestProvider: () -> Unit,
    onSaveProvider: () -> Unit,
    onClose: () -> Unit
) {
    V2Modal {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            V2PanelHeader("Settings & Provider", Icons.Outlined.Tune, onClose)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                item {
                    V2SettingsSection("AI Provider", "OpenAI-compatible endpoint with automatic model discovery") {
                        OutlinedTextField(
                            value = providerBaseUrl,
                            onValueChange = onProviderBaseUrlChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Base URL") },
                            supportingText = { Text("https://host/v1 or https://host") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(
                            value = providerApiKey,
                            onValueChange = onProviderApiKeyChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API key") },
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
                                supportingText = { Text(if (availableModels.isEmpty()) "No discovered models" else availableModels.size.toString() + " models detected") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { onModelMenuChange(!modelMenuOpen) }) {
                                        Icon(Icons.Outlined.ExpandMore, "Models", tint = VynnraMuted)
                                    }
                                }
                            )
                            DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { onModelMenuChange(false) }) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(text = { Text(model) }, onClick = { onSelectModel(model) })
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRefreshModels, enabled = !providerBusy) {
                                Icon(Icons.Outlined.Refresh, null)
                                Spacer(Modifier.width(5.dp))
                                Text(if (providerBusy) "Fetching…" else "Refresh")
                            }
                            OutlinedButton(onClick = onTestProvider, enabled = !providerBusy) {
                                Icon(Icons.Outlined.Wifi, null)
                                Spacer(Modifier.width(5.dp))
                                Text("Test")
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(providerStatus, color = if (providerStatus.startsWith("Connected")) VynnraPurpleSoft else VynnraMuted)
                    }
                }
                item {
                    V2SettingsSection("Agent behavior", "Vynnra shows high-level work activity inside the conversation") {
                        SettingLineV2("Thinking level", thinkingLevel.name, Icons.Outlined.AutoAwesome)
                        SettingLineV2("Execution loop", "Observe → Plan → Act → Verify → Recover", Icons.Outlined.Bolt)
                        SettingLineV2("Private reasoning", "Never displayed or stored", Icons.Outlined.Security)
                        SettingLineV2("Emergency stop", "Visible while Vynnra works", Icons.Outlined.StopCircle)
                    }
                }
                item {
                    V2SettingsSection("Voice & recording", "Speech and app-private audio") {
                        SettingLineV2("Microphone", if (microphoneGranted) "Granted" else "Not granted", Icons.Outlined.Mic)
                        SettingLineV2("Recorder", if (recordingState.recording) "Recording" else "Ready", Icons.Outlined.RecordVoiceOver)
                    }
                }
                item {
                    V2SettingsSection("Long-session stability", "Protects the UI during extended conversations") {
                        SettingLineV2("Conversation buffer", "Up to 140 messages in memory", Icons.Outlined.Memory)
                        SettingLineV2("Activity timeline", "Last 32 high-level events", Icons.Outlined.Bolt)
                        SettingLineV2("Provider", "Auto model discovery + diagnostics", Icons.Outlined.Wifi)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSaveProvider,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VynnraPurple)
            ) {
                Icon(Icons.Outlined.CheckCircle, null)
                Spacer(Modifier.width(7.dp))
                Text("Save provider")
            }
        }
    }
}

@Composable
private fun V2SettingsSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(color = VynnraPanelElevated.copy(alpha = 0.72f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(13.dp)) {
            Text(title, color = VynnraText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = VynnraMuted)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingLineV2(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraPurpleSoft, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = VynnraMuted, modifier = Modifier.weight(1f))
        Text(value, color = VynnraText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun V2PermissionCenter(
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
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    V2Modal {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            V2PanelHeader("Permission Center", Icons.Outlined.Security, onClose)
            Text("Grant only the capabilities Vynnra needs. Android still enforces protected system boundaries.", color = VynnraMuted)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                item { PermissionRowV2("Screen capture", screenCaptureGranted, "Capture the current screen", onRequestScreenCapture) }
                item { PermissionRowV2("Accessibility", accessibilityGranted, "Read and interact with supported UI controls", onOpenAccessibilitySettings) }
                item { PermissionRowV2("Full shared storage", fullStorageGranted, "Access eligible shared storage", onOpenStorageSettings) }
                item { PermissionRowV2("Overlay", overlayGranted, "Allow Vynnra UI over other apps", onOpenOverlaySettings) }
                item { PermissionRowV2("Microphone", microphoneGranted, "Voice input and recording", onRequestMicrophone) }
                item { PermissionRowV2("Notifications", notificationGranted, "Status and foreground notifications", onRequestNotifications) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Refresh permission state")
            }
        }
    }
}

@Composable
private fun PermissionRowV2(title: String, granted: Boolean, detail: String, onAction: () -> Unit) {
    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAction).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = if (granted) VynnraPurple.copy(alpha = 0.18f) else VynnraPanel, shape = CircleShape, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Security,
                        null,
                        tint = if (granted) VynnraPurpleSoft else VynnraMuted
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = VynnraText, fontWeight = FontWeight.SemiBold)
                Text(detail, color = VynnraMuted)
            }
            Text(if (granted) "ON" else "OPEN", color = if (granted) VynnraPurpleSoft else VynnraMuted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V2MemoryTasksPanel(onClose: () -> Unit) {
    V2Modal {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            V2PanelHeader("Memory & Tasks", Icons.Outlined.Memory, onClose)
            Text(
                "Vynnra keeps durable memory and task checkpoints through the app repositories.",
                color = VynnraMuted
            )
            Spacer(Modifier.height(14.dp))
            V2SettingsSection("Persistent workspace", "The runtime is already wired to memory and task repositories") {
                SettingLineV2("Memory", "Connected to agent runtime", Icons.Outlined.Memory)
                SettingLineV2("Tasks", "Checkpoint + resume enabled", Icons.Outlined.Bolt)
                SettingLineV2("Recovery", "Bounded retries + verification", Icons.Outlined.Security)
            }
            Spacer(Modifier.height(12.dp))
            V2SettingsSection("Current architecture", "Agent execution remains observable without exposing private reasoning") {
                Text(
                    "OBSERVE → UNDERSTAND → PLAN → ACT → VERIFY → RECOVER → RESPOND",
                    color = VynnraPurpleSoft,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "High-level action telemetry is shown in chat. Hidden chain-of-thought is never surfaced.",
                    color = VynnraMuted
                )
            }
        }
    }
}

@Composable
private fun V2RecordingsPanel(
    recordings: List<AudioRecording>,
    state: AudioRecorderState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPlay: (AudioRecording) -> Unit,
    onStopPlayback: () -> Unit,
    onDelete: (AudioRecording) -> Unit,
    onClose: () -> Unit
) {
    V2Modal {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            V2PanelHeader("Recordings", Icons.Outlined.RecordVoiceOver, onClose)
            Text("Saved inside Vynnra's private app storage.", color = VynnraMuted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (state.recording) onStop() else onStart() },
                    colors = ButtonDefaults.buttonColors(containerColor = VynnraPurple)
                ) {
                    Icon(Icons.Outlined.RecordVoiceOver, null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (state.recording) "Stop" else "Record")
                }
                OutlinedButton(onClick = onStopPlayback, enabled = state.playingPath != null) {
                    Icon(Icons.Outlined.StopCircle, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Stop playback")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.error != null) Text(state.error.orEmpty(), color = Color(0xFFFFA8C0))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(recordings, key = { it.path }) { recording ->
                    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(15.dp)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(recording.name, color = VynnraText, maxLines = 1)
                                Text(
                                    formatDuration(recording.durationMs) + " · " +
                                        (recording.sizeBytes / 1024L).toString() + " KB",
                                    color = VynnraMuted
                                )
                            }
                            IconButton(onClick = { onPlay(recording) }) { Icon(Icons.Outlined.PlayArrow, "Play", tint = VynnraPurpleSoft) }
                            IconButton(onClick = { onDelete(recording) }) { Icon(Icons.Outlined.DeleteOutline, "Delete", tint = VynnraMuted) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2Modal(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Surface(
            color = VynnraPanel,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f)
        ) {
            content()
        }
    }
}

@Composable
private fun V2PanelHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VynnraPurple)
        Spacer(Modifier.width(9.dp))
        Text(title, color = VynnraText, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close", tint = VynnraMuted) }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
