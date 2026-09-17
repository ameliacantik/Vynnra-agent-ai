package lol.vynnra.agent.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lol.vynnra.agent.ui.theme.VynnraMuted
import lol.vynnra.agent.ui.theme.VynnraPurpleSoft
import lol.vynnra.agent.ui.theme.VynnraText

@Composable
fun PermissionLine(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = VynnraText, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (granted) "Granted" else "Not granted",
            color = if (granted) VynnraPurpleSoft else VynnraMuted
        )
    }
}
