package lol.vynnra.agent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lol.vynnra.agent.core.permission.PermissionState
import lol.vynnra.agent.core.permission.PermissionStatus
import lol.vynnra.agent.ui.theme.VynnraBorder
import lol.vynnra.agent.ui.theme.VynnraMuted
import lol.vynnra.agent.ui.theme.VynnraPanelElevated
import lol.vynnra.agent.ui.theme.VynnraPurple
import lol.vynnra.agent.ui.theme.VynnraText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings

@Composable
fun PermissionCenter(
    permissions: List<PermissionState>,
    onRefresh: () -> Unit,
    onRequest: (PermissionState) -> Unit
) {
    Column {
        Text("Vynnra only uses capabilities you explicitly grant.", color = VynnraMuted)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(permissions) { permission ->
                PermissionCard(permission, onRequest)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh permissions") }
    }
}

@Composable
private fun PermissionCard(permission: PermissionState, onRequest: (PermissionState) -> Unit) {
    val granted = permission.status == PermissionStatus.GRANTED
    Surface(color = VynnraPanelElevated, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (granted) VynnraPurple else VynnraMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(permission.label, color = VynnraText)
                Text(permission.description, color = VynnraMuted)
                Text(permission.status.name.replace('_', ' '), color = VynnraMuted)
            }
            if (!granted && permission.status != PermissionStatus.UNAVAILABLE) {
                Text(
                    "Enable",
                    color = VynnraPurple,
                    modifier = Modifier.clickable { onRequest(permission) }.padding(8.dp)
                )
            }
        }
    }
}
