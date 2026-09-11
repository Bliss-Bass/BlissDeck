package org.gamelauncher.ui.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.gamelauncher.data.Artwork
import org.gamelauncher.ui.theme.Background

@Composable
fun AmbientBackdrop(
    artwork: Artwork,
    modifier: Modifier = Modifier,
) {
    val motion = rememberInfiniteTransition(label = "ambient-art")
    val panX by motion.animateFloat(
        initialValue = -0.045f,
        targetValue = 0.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient-x",
    )
    val panY by motion.animateFloat(
        initialValue = 0.03f,
        targetValue = -0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient-y",
    )
    val zoom by motion.animateFloat(
        initialValue = 1.28f,
        targetValue = 1.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(34_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient-zoom",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Background),
    ) {
        Crossfade(targetState = artwork, label = "ambient-crossfade") { frame ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX * size.width
                        translationY = panY * size.height
                    }
                    .then(
                        if (Build.VERSION.SDK_INT >= 31) Modifier.blur(36.dp) else Modifier,
                    ),
            ) {
                ArtworkLayer(frame, landscape = true)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.38f),
                        0.45f to Color.Black.copy(alpha = 0.52f),
                        1f to Background.copy(alpha = 0.88f),
                    ),
                ),
        )
    }
}
