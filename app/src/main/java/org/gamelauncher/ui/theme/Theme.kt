package org.gamelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = PlayGreen,
    onPrimary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = BackgroundRaised,
    onSurface = TextPrimary,
    surfaceVariant = Tile,
    onSurfaceVariant = TextMuted,
)

@Composable
fun GameLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        content = content,
    )
}
