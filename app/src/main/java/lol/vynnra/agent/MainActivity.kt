package lol.vynnra.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VynnraAgentApp() }
    }
}

private val VynnraBlack = Color(0xFF050507)
private val VynnraSurface = Color(0xFF0C0B12)
private val VynnraPurple = Color(0xFF9B6DFF)

@Composable
fun VynnraAgentApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = VynnraBlack) {
            ChatScreen()
        }
    }
}

@Composable
private fun ChatScreen() {
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(VynnraBlack)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Menu, contentDescription = "Open chats", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Vynnra Agent", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Ready", color = Color(0xFF9E9AA8), style = MaterialTheme.typography.labelSmall)
            }
            ThinkingBadge()
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("V", color = VynnraPurple, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("How can I help?", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Your personal Android AI agent", color = Color(0xFF9E9AA8))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Vynnra…") },
                shape = RoundedCornerShape(22.dp),
                singleLine = false,
                maxLines = 5
            )
            IconButton(onClick = { if (message.isNotBlank()) message = "" }) {
                Icon(Icons.Outlined.Send, contentDescription = "Send", tint = VynnraPurple)
            }
        }
    }
}

@Composable
private fun ThinkingBadge() {
    Surface(color = VynnraSurface, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MAX", color = VynnraPurple, style = MaterialTheme.typography.labelMedium)
        }
    }
}
