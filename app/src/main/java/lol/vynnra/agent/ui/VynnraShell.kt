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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.ui.theme.VynnraBlack
import lol.vynnra.agent.ui.theme.VynnraBorder
import lol.vynnra.agent.ui.theme.VynnraMuted
import lol.vynnra.agent.ui.theme.VynnraPanel
import lol.vynnra.agent.ui.theme.VynnraPanelElevated
import lol.vynnra.agent.ui.theme.VynnraPurple
import lol.vynnra.agent.ui.theme.VynnraPurpleSoft
import lol.vynnra.agent.ui.theme.VynnraText

private data class UiSession(val id: Int, val title: String)
private data class UiMessage(val role: Role, val text: String)
private enum class Role { USER, ASSISTANT }

@Composable
fun VynnraShell(
    screenCaptureGranted: Boolean = false,
    onRequestScreenCapture: () -> Unit = {}
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var permissionOpen by remember { mutableStateOf(false) }
    var thinkingLevel by remember { mutableStateOf(ThinkingLevel.MAX) }
    var message by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf(1) }
    val sessions = remember { mutableStateListOf(UiSession(1, "New Chat")) }
    val messages = remember { mutableStateListOf<UiMessage>() }

    Box(Modifier.fillMaxSize().background(VynnraBlack)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = sessions.firstOrNull { it.id == selectedSession }?.title ?: "New Chat",
                thinkingLevel = thinkingLevel,
                onMenu = { drawerOpen = true },
                onThinkingChange = { thinkingLevel = it }
            )

            ActivityStrip()

            MessageArea(messages = messages, modifier = Modifier.weight(1f))

            Composer(
                message = message,
                onMessageChange = { message = it },
                onAttach = {},
                onStop = {},
                onSend = {
                    val trimmed = message.trim()
                    if (trimmed.isNotEmpty()) {
                        messages += UiMessage(Role.USER, trimmed)
                        if (sessions.firstOrNull { it.id == selectedSession }?.title == "New Chat") {
                            sessions.replaceAll { session ->
                                if (session.id == selectedSession) session.copy(title = trimmed.take(28)) else session
                            }
                        }
                        message = ""
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Drawer(
                sessions = sessions,
                selectedSession = selectedSession,
                onClose = { drawerOpen = false },
                onNewChat = {
                    val nextId = (sessions.maxOfOrNull { it.id } ?: 0) + 1
                    sessions.add(UiSession(nextId, "New Chat"))
                    selectedSession = nextId
                    messages.clear()
                    drawerOpen = false
                },
                onSession = {
                    selectedSession = it
                    messages.clear()
                    drawerOpen = false
                },
                onPermissions = {
                    permissionOpen = true
                    drawerOpen = false
                },
                onSettings = {
                    settingsOpen = true
                    drawerOpen = false
                }
            )
        }

        if (settingsOpen) {
            ModalPanel(title = "Settings", onClose = { settingsOpen = false }) {
                SettingRow("Appearance", "AMOLED / Dark", Icons.Outlined.AutoAwesome)
                SettingRow("Provider", "Not connected", Icons.Outlined.Security)
                SettingRow("Thinking level", thinkingLevel.name, Icons.Outlined.AutoAwesome)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { settingsOpen = false }) { Text("Done") }
            }
        }

        if (permissionOpen) {
            ModalPanel(title = "Permission Center", onClose = { permissionOpen = false }) {
                PermissionRow("Accessibility Control", false)
                PermissionRow("Screen Capture", screenCaptureGranted)
                if (!screenCaptureGranted) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onRequestScreenCapture,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Screen Capture")
                    }
                } else {
                    Text(
                        "Screen capture consent granted for this app session. The screenshot pipeline will consume this grant in Phase 6.",
                        color = VynnraMuted
                    )
                }
                PermissionRow("Files", false)
                PermissionRow("Overlay", false)
                PermissionRow("Microphone", false)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Permissions are user-controlled. Vynnra only uses capabilities that Android has granted.",
                    color = VynnraMuted
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    thinkingLevel: ThinkingLevel,
    onMenu: () -> Unit,
    onThinkingChange: (ThinkingLevel) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Outlined.Menu, contentDescription = "Open sidebar", tint = VynnraText)
        }
        Column(Modifier.weight(1f)) {
            Text("Vynnra Agent", color = VynnraText, fontWeight = FontWeight.Bold)
            Text(title, color = VynnraMuted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
        ThinkingSelector(thinkingLevel, onThinkingChange)
    }
}

@Composable
private fun ThinkingSelector(selected: ThinkingLevel, onSelected: (ThinkingLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { expanded = true }) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.name, color = VynnraPurpleSoft, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = VynnraMuted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThinkingLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.name) },
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
private fun ActivityStrip() {
    Surface(color = VynnraPanel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = VynnraPurple.copy(alpha = 0.18f), shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(8.dp))
            Text("Ready — waiting for your request", color = VynnraMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MessageArea(messages: List<UiMessage>, modifier: Modifier) {
    if (messages.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = VynnraPanelElevated, shape = CircleShape, modifier = Modifier.size(70.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("V", color = VynnraPurple, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("How can Vynnra help?", color = VynnraText, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Ask, research, plan, or control your Android device.", color = VynnraMuted)
            }
        }
        return
    }

    LazyColumn(modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(messages) { item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (item.role == Role.USER) Arrangement.End else Arrangement.Start) {
                Surface(
                    color = if (item.role == Role.USER) VynnraPanelElevated else VynnraPanel,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(item.text, color = VynnraText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun Composer(
    message: String,
    onMessageChange: (String) -> Unit,
    onAttach: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(onClick = onAttach) {
            Icon(Icons.Outlined.AttachFile, contentDescription = "Attach", tint = VynnraMuted)
        }
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message Vynnra…") },
            shape = RoundedCornerShape(22.dp),
            maxLines = 5,
            singleLine = false
        )
        IconButton(onClick = onStop) {
            Icon(Icons.Outlined.StopCircle, contentDescription = "Stop agent", tint = VynnraMuted)
        }
        IconButton(onClick = onSend) {
            Icon(Icons.Outlined.Send, contentDescription = "Send", tint = VynnraPurple)
        }
    }
}

@Composable
private fun Drawer(
    sessions: List<UiSession>,
    selectedSession: Int,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSession: (Int) -> Unit,
    onPermissions: () -> Unit,
    onSettings: () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        Surface(color = VynnraPanel, modifier = Modifier.width(310.dp).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Vynnra", color = VynnraText, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted) }
                }
                Spacer(Modifier.height(10.dp))
                Surface(color = VynnraPurple.copy(alpha = 0.14f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onNewChat() }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AddComment, contentDescription = null, tint = VynnraPurple)
                        Spacer(Modifier.width(10.dp))
                        Text("New Chat", color = VynnraText)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Chats", color = VynnraMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sessions) { session ->
                        Surface(color = if (session.id == selectedSession) VynnraPanelElevated else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onSession(session.id) }) {
                            Text(session.title, color = VynnraText, modifier = Modifier.padding(11.dp))
                        }
                    }
                }
                VerticalDivider()
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
        Icon(icon, contentDescription = null, tint = VynnraMuted)
        Spacer(Modifier.width(10.dp))
        Text(label, color = VynnraText)
    }
}

@Composable
private fun ModalPanel(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
        Surface(color = VynnraPanel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth(0.88f)) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = VynnraText, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
        Icon(icon, contentDescription = null, tint = VynnraPurple)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VynnraText)
            Text(value, color = VynnraMuted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = if (granted) VynnraPurple.copy(alpha = 0.18f) else VynnraBorder, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(10.dp))
        Text(label, color = VynnraText, modifier = Modifier.weight(1f))
        Text(if (granted) "Granted" else "Not enabled", color = VynnraMuted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}
