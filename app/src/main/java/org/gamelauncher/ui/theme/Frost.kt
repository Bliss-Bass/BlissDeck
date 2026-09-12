package org.gamelauncher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.ThemeBlurLevel

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun rememberLauncherHazeState(): HazeState = remember { HazeState() }

@Composable
fun ProvideFrost(state: HazeState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHazeState provides state, content = content)
}

fun Modifier.frostSource(): Modifier = composed {
    val state = LocalHazeState.current
    if (state == null) this else hazeSource(state)
}

fun Modifier.frosted(tint: Color, blurRadius: Dp? = null): Modifier = composed {
    val state = LocalHazeState.current
    val radius = blurRadius ?: LocalTheme.current.chrome.blurLevel.radius
    if (state == null || radius <= 0.dp) {
        background(tint)
    } else {
        hazeEffect(
            state = state,
            style = HazeStyle(
                backgroundColor = tint.copy(alpha = 1f),
                tints = listOf(HazeTint(tint)),
                blurRadius = radius,
            ),
        )
    }
}

fun Modifier.chromeContentPadding(
    extraTop: Dp = 0.dp,
    extraBottom: Dp = 0.dp,
    horizontal: Dp = 0.dp,
): Modifier = composed {
    val chrome = LocalTheme.current.chrome
    padding(
        start = horizontal,
        end = horizontal,
        top = chrome.topBarHeight + extraTop,
        bottom = chrome.bottomBarHeight + extraBottom,
    )
}

fun ThemeBlurLevel.label(): String = when (this) {
    ThemeBlurLevel.Off -> "Off"
    ThemeBlurLevel.Soft -> "Soft"
    ThemeBlurLevel.Regular -> "Regular"
    ThemeBlurLevel.Heavy -> "Heavy"
}
