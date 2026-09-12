package org.gamelauncher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.ui.theme.Footer
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import org.gamelauncher.data.Artwork
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.data.LocalTheme
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
        Artwork(packageName, repo.steamGridId(packageName), null, null, null, icon)
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
    preferIcon: Boolean = false,
    onFocused: () -> Unit = {},
) {
    val artwork = if (packageName.isNotBlank()) {
        rememberArtwork(packageName, title, isGame)
    } else {
        Artwork("", null, null, null, null, null)
    }
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .tileFrame(selected, shape, onFocused)
            .background(hueBrush(hue, portrait = !landscape)),
    ) {
        ArtworkLayer(
            artwork,
            landscape,
            modifier = if (preferIcon) Modifier.padding(horizontal = 28.dp, vertical = 16.dp) else Modifier,
            preferIcon = preferIcon,
        )
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
fun ArtworkLayer(
    artwork: Artwork,
    landscape: Boolean,
    modifier: Modifier = Modifier,
    preferIcon: Boolean = false,
) {
    val iconUrl = artwork.iconUrl?.takeIf { it.startsWith("http") }
    val url = when {
        preferIcon && iconUrl != null -> iconUrl
        preferIcon -> null
        else -> artwork.imageUrl(landscape)
    }
    when {
        url != null -> AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = if (preferIcon) ContentScale.Fit else ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
        artwork.icon != null -> DrawableImage(
            artwork.icon,
            modifier.fillMaxSize(),
            contentScale = if (preferIcon) ContentScale.Fit else ContentScale.Crop,
        )
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
fun AppIconImage(drawable: Drawable, modifier: Modifier = Modifier) {
    DrawableImage(drawable, modifier, ContentScale.Crop)
}

@Composable
fun GameIcon(artwork: Artwork, modifier: Modifier = Modifier) {
    val url = artwork.iconUrl?.takeIf { it.startsWith("http") }
    when {
        url != null -> AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        artwork.icon != null -> AppIconImage(artwork.icon, modifier)
    }
}

@Composable
private fun DrawableImage(
    drawable: Drawable,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap = remember(drawable) {
        val size = maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight, 128)
        drawable.toBitmap(size, size)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = contentScale,
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
    onFocused: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = modifier
            .tileFrame(selected, shape, onFocused)
            .background(Tile)
            .tileClick(onClick)
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
            .tileFrame(
                selected = false,
                shape = shape,
                width = 2.dp,
                color = if (selected) Color(0xFF1B1F24) else TileBorder,
            )
            .background(if (selected) Pill else Color.Transparent)
            .tileClick(onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color(0xFF1B1F24) else TextPrimary,
            style = centeredLabelStyle(13.sp, FontWeight.Medium, 1.1.sp),
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                color = if (selected) Color(0xFF1B1F24) else TextMuted,
                style = centeredLabelStyle(13.sp),
            )
        }
    }
}

@Composable
fun ShoulderKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val caption = when (label) {
        "L1" -> "Previous"
        "R1" -> "Next"
        else -> null
    }
    HoverCaption(caption.orEmpty(), modifier) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .tileFrame(false, shape, width = 2.dp, color = Color.Black)
                .background(Color.White)
                .tileClick(onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = Color.Black,
                style = centeredLabelStyle(14.sp, FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun FaceButton(letter: String, caption: String, modifier: Modifier = Modifier) {
    val circle = 22.dp * LocalDensity.current.fontScale
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(circle)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                color = Color.Black,
                style = centeredLabelStyle(12.sp, FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            caption.uppercase(),
            color = TextPrimary,
            style = centeredLabelStyle(13.sp, FontWeight.Medium, 1.sp),
        )
    }
}

fun centeredLabelStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Medium,
    letterSpacing: TextUnit = TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontSize = fontSize,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing,
    lineHeight = fontSize,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

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

@Composable
fun HoverCaption(
    caption: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val enabled = LocalSettings.current.state.collectAsState().value.hoverCaptions
    if (!enabled || caption.isBlank()) {
        Box(modifier) { content() }
        return
    }
    val hover = remember { MutableInteractionSource() }
    val hoverableHovered by hover.collectIsHoveredAsState()
    var pointerHovered by remember { mutableStateOf(false) }
    val hovered = hoverableHovered || pointerHovered
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) {
        if (hovered) {
            delay(280)
            visible = true
        } else {
            visible = false
        }
    }
    val gapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val position = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = captionOffset(anchorBounds, windowSize, popupContentSize, gapPx)
        }
    }
    Box(
        modifier
            .hoverable(hover)
            .pointerInput(caption) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Enter -> pointerHovered = true
                            PointerEventType.Exit -> pointerHovered = false
                            else -> Unit
                        }
                    }
                }
            },
    ) {
        content()
        if (visible) {
            Popup(
                popupPositionProvider = position,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                Text(
                    caption,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Footer.copy(alpha = 0.96f))
                        .border(1.dp, TileBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun captionOffset(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupContentSize: IntSize,
    gapPx: Int,
): IntOffset {
    val maxX = (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8)
    val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2).coerceIn(8, maxX)
    val below = anchorBounds.bottom + gapPx
    val y = if (below + popupContentSize.height <= windowSize.height - 8) {
        below
    } else {
        (anchorBounds.top - popupContentSize.height - gapPx).coerceAtLeast(8)
    }
    return IntOffset(x, y)
}

fun Modifier.tileClick(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    )
}

fun Modifier.tileFrame(
    selected: Boolean,
    shape: RoundedCornerShape,
    onFocused: () -> Unit = {},
    width: Dp? = null,
    color: Color? = null,
): Modifier = composed {
    val theme = LocalTheme.current
    val stroke = width ?: theme.borders.width
    val strokeColor = color ?: theme.borders.color
    val frameShape = RoundedCornerShape(theme.borders.radius)
    var focused by remember { mutableStateOf(false) }
    onFocusChanged {
        focused = it.isFocused
        if (it.isFocused) onFocused()
    }
        .clip(frameShape)
        .then(if (selected || focused) Modifier.border(stroke, strokeColor, frameShape) else Modifier)
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.rowFocus(requesters: List<FocusRequester>, index: Int): Modifier {
    return focusRequester(requesters[index]).focusProperties {
        left = requesters.getOrNull(index - 1) ?: FocusRequester.Cancel
        right = requesters.getOrNull(index + 1) ?: FocusRequester.Cancel
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.columnFocus(requesters: List<FocusRequester>, index: Int): Modifier {
    return focusRequester(requesters[index]).focusProperties {
        up = requesters.getOrNull(index - 1) ?: FocusRequester.Cancel
        down = requesters.getOrNull(index + 1) ?: FocusRequester.Cancel
        left = FocusRequester.Cancel
        right = FocusRequester.Cancel
    }
}
