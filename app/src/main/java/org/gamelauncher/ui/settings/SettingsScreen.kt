package org.gamelauncher.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val artwork = LocalArtwork.current
    var apiKey by remember { mutableStateOf(artwork.apiKey) }
    val rows = listOf(
        "Appearance" to "Grid size, theme, colours",
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Tile)
                .padding(18.dp),
        ) {
            Text("SteamGridDB", color = TextPrimary, fontSize = 18.sp)
            Text(
                if (apiKey.isBlank()) {
                    "App icons show immediately. Paste an API key to fetch covers and heroes."
                } else {
                    "API key saved. Covers refresh for games as they appear."
                },
                color = TextMuted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Background)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (apiKey.isEmpty()) {
                        Text("API key", color = TextMuted, fontSize = 16.sp)
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Save key",
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Background)
                    .clickable { artwork.apiKey = apiKey }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Get a key at steamgriddb.com/profile/preferences/api",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.steamgriddb.com/profile/preferences/api"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
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
