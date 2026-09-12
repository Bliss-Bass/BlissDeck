package org.gamelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.ThemePack

@Composable
fun GameLauncherTheme(theme: ThemePack, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = theme.colors.playGreen,
        onPrimary = Color.White,
        background = theme.colors.background,
        onBackground = theme.colors.textPrimary,
        surface = theme.colors.backgroundRaised,
        onSurface = theme.colors.textPrimary,
        surfaceVariant = theme.colors.tile,
        onSurfaceVariant = theme.colors.textMuted,
    )
    CompositionLocalProvider(LocalTheme provides theme) {
        MaterialTheme(
            colorScheme = scheme,
            content = content,
        )
    }
}
