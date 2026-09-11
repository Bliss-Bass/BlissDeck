package org.gamelauncher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import org.gamelauncher.data.Artwork
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.ui.theme.Pill
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.TileBorder
import kotlin.math.absoluteValue

fun hueBrush(hue: Float, portrait: Boolean = true): Brush {
    val a = Color.hsl((hue).mod(360f), 0.62f, 0.42f)
    val b = Color.hsl((hue + 40f).mod(360f), 0.55f, 0.22f)
    val c = Color.hsl((hue - 25f).mod(360f), 0.48f, 0.16f)
    return if (portrait) {
        Brush.linearGradient(listOf(a, b, c), start = Offset(0f, 0f), end = Offset(400f, 900f))
    } else {
        Brush.linearGradient(listOf(c, a, b), start = Offset(0f, 0f), end = Offset(1400f, 400f))
    }
}

@Composable
fun rememberArtwork(
    packageName: String,
    title: String,
    isGame: Boolean,
): Artwork {
    val repo = LocalArtwork.current
    val epoch by repo.epoch.collectAsState()
    val icon = remember(packageName) { repo.iconFor(packageName) }
    val placeholder = remember(packageName, icon) {
        Artwork(packageName, repo.steamGridId(packageName), null, null, icon)
    }
    val art by produceState(placeholder, packageName, title, isGame, epoch) {
        value = repo.resolve(packageName, title, isGame)
    }
    return art
}

@Composable
fun CoverArt(
    title: String,
    hue: Float,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showTitle: Boolean = false,
    packageName: String = "",
    isGame: Boolean = true,
    landscape: Boolean = false,
) {
    val artwork = if (packageName.isNotBlank()) {
        rememberArtwork(packageName, title, isGame)
    } else {
        Artwork("", null, null, null, null)
    }
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (selected) Modifier.border(3.dp, TileBorder, shape) else Modifier)
            .background(hueBrush(hue, portrait = !landscape)),
    ) {
        ArtworkLayer(artwork, landscape)
        if (showTitle) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
fun ArtworkLayer(artwork: Artwork, landscape: Boolean, modifier: Modifier = Modifier) {
    val url = artwork.imageUrl(landscape)
    when {
        url != null -> AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
        artwork.icon != null -> DrawableImage(artwork.icon, modifier.fillMaxSize())
        else -> Canvas(modifier.fillMaxSize()) {
            val mark = Path().apply {
                val cx = size.width * 0.5f
                val cy = size.height * 0.42f
                val r = size.minDimension * 0.18f
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(mark, Color.White.copy(alpha = 0.88f), style = Fill)
        }
    }
}

@Composable
private fun DrawableImage(drawable: Drawable, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        val size = maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight, 128)
        drawable.toBitmap(size, size)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
fun AppIconTile(
    title: String,
    hue: Float,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    packageName: String = "",
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, TileBorder, shape) else Modifier)
            .background(Tile)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(hueBrush(hue)),
            contentAlignment = Alignment.Center,
        ) {
            val artwork = if (packageName.isNotBlank()) {
                rememberArtwork(packageName, title, isGame = false)
            } else {
                null
            }
            if (artwork?.icon != null) {
                DrawableImage(artwork.icon, Modifier.fillMaxSize())
            } else {
                Text(
                    text = title.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SteamPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Pill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color(0xFF1B1F24) else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                color = if (selected) Color(0xFF1B1F24) else TextMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun ShoulderKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun FaceButton(letter: String, caption: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            caption.uppercase(),
            color = TextPrimary,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun DiamondMark(size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width / 2f, 2f)
            lineTo(this@Canvas.size.width - 2f, this@Canvas.size.height / 2f)
            lineTo(this@Canvas.size.width / 2f, this@Canvas.size.height - 2f)
            lineTo(2f, this@Canvas.size.height / 2f)
            close()
        }
        drawPath(path, Color.White)
        val inner = Path().apply {
            val cx = this@Canvas.size.width / 2f
            val cy = this@Canvas.size.height / 2f
            val r = this@Canvas.size.minDimension * 0.22f
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        drawPath(inner, Color.Black)
    }
}

fun String.stableHue(): Float = (hashCode().absoluteValue % 360).toFloat()
