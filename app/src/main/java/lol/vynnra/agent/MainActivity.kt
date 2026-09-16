package lol.vynnra.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import lol.vynnra.agent.ui.VynnraShell
import lol.vynnra.agent.ui.theme.VynnraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VynnraTheme {
                VynnraShell()
            }
        }
    }
}
