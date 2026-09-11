package org.gamelauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

@Composable
fun SettingsScreen() {
    val rows = listOf(
        "Appearance" to "Grid size, theme, colours",
        "Library" to "Installed games, SteamGridDB, filters",
        "Input" to "Gamepad, keyboard, mouse",
        "Home" to "Set as default HOME launcher",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(28.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        rows.forEach { (title, subtitle) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Tile)
                    .padding(18.dp),
            ) {
                Text(title, color = TextPrimary, fontSize = 18.sp)
                Text(subtitle, color = TextMuted, fontSize = 14.sp)
            }
        }
    }
}
