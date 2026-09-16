package lol.vynnra.agent.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VynnraBlack = Color(0xFF050507)
val VynnraPanel = Color(0xFF0C0B12)
val VynnraPanelElevated = Color(0xFF15121F)
val VynnraBorder = Color(0xFF26232F)
val VynnraPurple = Color(0xFF9B6DFF)
val VynnraPurpleSoft = Color(0xFFB99BFF)
val VynnraText = Color(0xFFF4F1FA)
val VynnraMuted = Color(0xFFA7A2B0)

private val VynnraDarkColors = darkColorScheme(
    primary = VynnraPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1E4A),
    onPrimaryContainer = VynnraPurpleSoft,
    secondary = Color(0xFFB9A8D8),
    background = VynnraBlack,
    onBackground = VynnraText,
    surface = VynnraPanel,
    onSurface = VynnraText,
    surfaceVariant = VynnraPanelElevated,
    onSurfaceVariant = VynnraMuted,
    outline = VynnraBorder
)

@Composable
fun VynnraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VynnraDarkColors,
        content = content
    )
}
