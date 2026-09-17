package lol.vynnra.agent.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.UUID
import lol.vynnra.agent.core.memory.MemoryKind
import lol.vynnra.agent.core.memory.MemoryRecord
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository
import lol.vynnra.agent.ui.theme.VynnraMuted
import lol.vynnra.agent.ui.theme.VynnraPanel
import lol.vynnra.agent.ui.theme.VynnraPanelElevated
import lol.vynnra.agent.ui.theme.VynnraPurple
import lol.vynnra.agent.ui.theme.VynnraText

@Composable
fun Phase9Center(
    memoryRepository: MemoryRepository,
    taskRepository: TaskRepository,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val memories by memoryRepository.observeAll().collectAsStateWithLifecycle(emptyList())
    val tasks by taskRepository.observeTasks().collectAsStateWithLifecycle(emptyList())
    var tab by remember { mutableIntStateOf(0) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var addMemoryOpen by remember { mutableStateOf(false) }
    var deleteMemoryId by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Surface(
            color = VynnraPanel,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Memory & Tasks", color = VynnraText, fontWeight = FontWeight.Bold)
                        Text("Persistent controls for what Vynnra remembers and resumes.", color = VynnraMuted)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = VynnraMuted)
                    }
                }

                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Memory") }, icon = { Icon(Icons.Outlined.Memory, null) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Tasks") }, icon = { Icon(Icons.Outlined.TaskAlt, null) })
                }
                Spacer(Modifier.height(10.dp))

                if (tab == 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${memories.count { it.enabled }} enabled / ${memories.size} total", color = VynnraMuted)
                        Button(onClick = { addMemoryOpen = true }) { Text("Add memory") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (memories.isEmpty()) {
                        EmptyPhase9("No saved memories yet.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(memories, key = { it.id }) { memory ->
                                MemoryCard(
                                    memory = memory,
                                    onEnabled = { enabled -> scope.launch { memoryRepository.setEnabled(memory.id, enabled) } },
                                    onPinned = { pinned -> scope.launch { memoryRepository.setPinned(memory.id, pinned) } },
                                    onDelete = { deleteMemoryId = memory.id }
                                )
                            }
                        }
                    }
                } else {
                    if (tasks.isEmpty()) {
                        EmptyPhase9("No persistent tasks yet.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tasks, key = { it.id }) { task ->
                                TaskCard(task, selected = selectedTaskId == task.id, onClick = { selectedTaskId = task.id })
                            }
                        }
                    }
                }
            }
        }
    }

    if (addMemoryOpen) {
        AddMemoryDialog(
            onDismiss = { addMemoryOpen = false },
            onSave = { key, content, importance, pinned ->
                val now = System.currentTimeMillis()
                scope.launch {
                    memoryRepository.upsert(
                        MemoryRecord(
                            id = UUID.randomUUID().toString(),
                            kind = MemoryKind.CONTEXT,
                            key = key.takeIf { it.isNotBlank() },
                            content = content,
                            sourceSessionId = null,
                            importance = importance,
                            sensitive = false,
                            enabled = true,
                            pinned = pinned,
                            createdAt = now,
                            updatedAt = now,
                            lastAccessedAt = null
                        )
                    )
                    addMemoryOpen = false
                }
            }
        )
    }

    deleteMemoryId?.let { id ->
        val memory = memories.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { deleteMemoryId = null },
            title = { Text("Forget memory?") },
            text = { Text(memory?.content ?: "This saved memory will be permanently removed.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { memoryRepository.delete(id) }
                    deleteMemoryId = null
                }) { Text("Forget") }
            },
            dismissButton = { Button(onClick = { deleteMemoryId = null }) { Text("Cancel") } }
        )
    }

    selectedTaskId?.let { taskId ->
        TaskDetailDialog(
            task = tasks.firstOrNull { it.id == taskId },
            repository = taskRepository,
            onDismiss = { selectedTaskId = null }
        )
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryRecord,
    onEnabled: (Boolean) -> Unit,
    onPinned: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(memory.key ?: memory.kind.name, color = VynnraText, fontWeight = FontWeight.SemiBold)
                    Text(memory.content, color = VynnraMuted)
                    if (!memory.enabled) Text("Disabled — not used for agent context", color = VynnraMuted)
                }
                IconButton(onClick = { onPinned(!memory.pinned) }) {
                    Icon(Icons.Outlined.PushPin, contentDescription = "Pin", tint = if (memory.pinned) VynnraPurple else VynnraMuted)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Forget", tint = VynnraMuted)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", color = VynnraMuted, modifier = Modifier.weight(1f))
                Switch(checked = memory.enabled, onCheckedChange = onEnabled)
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskRecord, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) VynnraPurple.copy(alpha = 0.12f) else VynnraPanelElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(task.title, color = VynnraText, fontWeight = FontWeight.SemiBold)
            Text(task.status.name, color = VynnraPurple)
            Text("Step ${task.currentStep}/${task.totalSteps}", color = VynnraMuted)
            task.lastError?.let { Text(it, color = VynnraMuted) }
        }
    }
}

@Composable
private fun TaskDetailDialog(
    task: TaskRecord?,
    repository: TaskRepository,
    onDismiss: () -> Unit
) {
    var loadedSteps by remember(task?.id) { mutableStateOf<List<TaskStepRecord>>(emptyList()) }
    LaunchedEffect(task?.id) {
        loadedSteps = if (task == null) emptyList() else repository.getSteps(task.id)
    }

    if (task == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(task.goal)
                Text("Status: ${task.status.name}")
                Text("Checkpoint: ${task.checkpointJson ?: "none"}", color = VynnraMuted)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(loadedSteps) { step ->
                        Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${step.stepIndex + 1}. ${step.title}", color = VynnraText)
                                Text(step.status.name, color = VynnraPurple)
                                Text("Attempts: ${step.attempts}", color = VynnraMuted)
                                step.outputSummary?.let { Text(it, color = VynnraMuted) }
                                step.errorMessage?.let { Text(it, color = VynnraMuted) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AddMemoryDialog(onDismiss: () -> Unit, onSave: (String, String, Float, Boolean) -> Unit) {
    var key by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(key, { key = it }, label = { Text("Key (optional)") })
                OutlinedTextField(content, { content = it }, label = { Text("Memory") }, minLines = 3)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pinned", color = VynnraText, modifier = Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(enabled = content.isNotBlank(), onClick = { onSave(key, content.trim(), 0.7f, pinned) }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyPhase9(text: String) {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, color = VynnraMuted)
    }
}
