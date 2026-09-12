package org.gamelauncher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import org.gamelauncher.data.Game
import org.gamelauncher.data.HomeFeedTab
import org.gamelauncher.data.InstalledApp
import org.gamelauncher.data.LauncherPrefs
import org.gamelauncher.data.LibrarySnapshot
import org.gamelauncher.data.LocalCollections
import org.gamelauncher.data.LocalPlayHistory
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.NewsItem
import org.gamelauncher.data.RecentsArt
import org.gamelauncher.data.RecentsLayout
import org.gamelauncher.data.RecentsMetrics
import org.gamelauncher.data.CollectionsStore
import org.gamelauncher.data.findEntry
import org.gamelauncher.data.recentsMetrics
import org.gamelauncher.data.toGame
import org.gamelauncher.ui.components.AmbientBackdrop
import org.gamelauncher.ui.components.AppIconTile
import org.gamelauncher.ui.components.ArtworkLayer
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.cardShape
import org.gamelauncher.ui.components.hueBrush
import org.gamelauncher.ui.components.rememberArtwork
import org.gamelauncher.ui.components.rowFocus
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.NewsBugfix
import org.gamelauncher.ui.theme.NewsUpdate
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.chromeContentPadding

@Composable
fun HomeScreen(
    snapshot: LibrarySnapshot,
    news: List<NewsItem>,
    newsLoading: Boolean,
    onOpenGame: (String) -> Unit,
) {
    val history = LocalPlayHistory.current
    val historyEpoch by history.epoch.collectAsState()
    val collections by LocalCollections.current.state.collectAsState()
    val recents = remember(snapshot, historyEpoch) {
        snapshot.libraryGames
            .sortedByDescending { history.lastPlayedMillis(it.packageName) }
            .ifEmpty { snapshot.installed.take(8).map { it.toGame() } }
    }
    if (recents.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            Text("No installed apps found", color = TextMuted, fontSize = 16.sp)
        }
        return
    }
    var selectedId by remember { mutableStateOf(recents.first().id) }
    var feed by remember { mutableStateOf(HomeFeedTab.WhatsNew) }
    val selected = recents.firstOrNull { it.id == selectedId } ?: recents.first()
    val selectedArt = rememberArtwork(selected.packageName, selected.title, selected.inLibrary)
    val settings = LocalSettings.current
    val prefs by settings.state.collectAsState()
    val layouts = LocalTheme.current.layouts

    Box(Modifier.fillMaxSize()) {
        if (prefs.ambientBackdrop) AmbientBackdrop(selectedArt)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .chromeContentPadding(extraTop = 18.dp, extraBottom = 12.dp, horizontal = 28.dp),
        ) {
        if (layouts.homeLastPlayed) {
            Text("Last played", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            RecentsRow(recents, selectedId, prefs, onSelect = { selectedId = it }, onOpenGame = onOpenGame)
        }
        if (prefs.showSelectedTitle && layouts.homePlayNow) {
            Spacer(Modifier.height(18.dp))
            Text(selected.title, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PlayGreen, modifier = Modifier.size(28.dp))
                Text(
                    "PLAY NOW!",
                    color = PlayGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                        .tileClick { onOpenGame(selected.id) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        if (layouts.homeFeed) {
        Spacer(Modifier.height(28.dp))
        FeedTabs(feed) { feed = it }
        Spacer(Modifier.height(16.dp))
        when (feed) {
            HomeFeedTab.WhatsNew -> {
                when {
                    news.isNotEmpty() -> WhatsNewTimeline(news, snapshot, onOpenGame)
                    newsLoading -> EmptyCenter("Checking Play Store…")
                    else -> EmptyCenter("No news yet")
                }
            }
            HomeFeedTab.Favorites -> {
                val favoriteIds = collections.firstOrNull { it.id == CollectionsStore.FAVORITES_ID }
                    ?.packageNames.orEmpty()
                val games = favoriteIds.mapNotNull { snapshot.findEntry(it) }
                if (games.isEmpty()) {
                    EmptyCenter("Add games to Favorites from a collection")
                } else {
                    RecommendedRow(
                        title = "Favorites",
                        subtitle = "Games you saved to your Favorites collection",
                        apps = games.map {
                            InstalledApp(it.id, it.title, it.packageName, it.coverHue, it.inLibrary)
                        },
                        onOpenGame = onOpenGame,
                    )
                }
            }
            HomeFeedTab.Recommended -> {
                val games = snapshot.libraryGames
                    .sortedBy { history.lastPlayedMillis(it.packageName) }
                    .take(12)
                RecommendedRow(
                    title = "Play next",
                    subtitle = "Games you have not opened lately",
                    apps = games.map {
                        InstalledApp(it.id, it.title, it.packageName, it.coverHue, true)
                    },
                    onOpenGame = onOpenGame,
                )
            }
        }
        }
        }
    }
}

private fun recentCoverXAt(
    index: Int,
    selectedIndex: Int,
    metrics: RecentsMetrics,
): Dp {
    val leftStack = metrics.peek * selectedIndex.coerceAtLeast(0)
    val delta = index - selectedIndex
    return when {
        delta < 0 -> metrics.peek * index
        delta == 0 -> leftStack
        else -> leftStack + metrics.heroWidth - metrics.overlap + metrics.peek * (delta - 1)
    }
}

private fun recentCoverX(
    index: Int,
    selected: Float,
    metrics: RecentsMetrics,
): Dp {
    val i0 = floor(selected).toInt()
    val t = selected - i0
    if (t <= 0.0008f) return recentCoverXAt(index, i0, metrics)
    return lerp(recentCoverXAt(index, i0, metrics), recentCoverXAt(index, i0 + 1, metrics), t)
}

private fun coverflowPos(raw: Float, last: Float): Float {
    if (last <= 0f) return 0f
    return when {
        raw < 0f -> raw * 0.32f
        raw > last -> last + (raw - last) * 0.32f
        else -> raw
    }
}

@Composable
private fun RecentsRow(
    recents: List<Game>,
    selectedId: String,
    prefs: LauncherPrefs,
    onSelect: (String) -> Unit,
    onOpenGame: (String) -> Unit,
) {
    if (prefs.recentsLayout == RecentsLayout.Row) {
        RecentsStrip(recents, selectedId, prefs, onSelect, onOpenGame)
    } else {
        RecentsCoverflow(recents, selectedId, prefs, onSelect, onOpenGame)
    }
}

@Composable
private fun RecentsStrip(
    recents: List<Game>,
    selectedId: String,
    prefs: LauncherPrefs,
    onSelect: (String) -> Unit,
    onOpenGame: (String) -> Unit,
) {
    val metrics = prefs.recentsMetrics()
    val requesters = remember(recents.map { it.id }) { List(recents.size) { FocusRequester() } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(recents, key = { _, game -> game.id }) { index, game ->
            RecentCard(
                game = game,
                selected = game.id == selectedId,
                growSelected = false,
                expand = 0f,
                metrics = metrics,
                preferIcon = prefs.recentsArt == RecentsArt.Icon,
                onFocused = { onSelect(game.id) },
                modifier = Modifier.rowFocus(requesters, index),
            ) {
                if (selectedId == game.id) onOpenGame(game.id) else onSelect(game.id)
            }
        }
    }
}

@Composable
private fun RecentsCoverflow(
    recents: List<Game>,
    selectedId: String,
    prefs: LauncherPrefs,
    onSelect: (String) -> Unit,
    onOpenGame: (String) -> Unit,
) {
    val metrics = prefs.recentsMetrics()
    val selectedIndex = recents.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val last = (recents.size - 1).coerceAtLeast(0).toFloat()
    val requesters = remember(recents.map { it.id }) { List(recents.size) { FocusRequester() } }
    val localDensity = LocalDensity.current
    val stepPx = with(localDensity) { metrics.peek.coerceAtLeast(96.dp).toPx() }
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    val onSelectLatest = rememberUpdatedState(onSelect)
    val recentsLatest = rememberUpdatedState(recents)
    val visualPos = if (dragging) dragPos else position.value
    val nearest = visualPos.roundToInt().coerceIn(0, recents.lastIndex.coerceAtLeast(0))
    val tilt = if (prefs.recentsTilt) 8f else 0f
    val recentsShape = cardShape()
    val settle = spring<Float>(
        dampingRatio = 0.92f,
        stiffness = Spring.StiffnessMedium,
    )
    LaunchedEffect(selectedIndex) {
        if (dragging) return@LaunchedEffect
        val target = selectedIndex.toFloat()
        if (abs(position.value - target) < 0.02f) {
            position.snapTo(target)
        } else {
            position.animateTo(target, settle)
        }
    }
    LaunchedEffect(nearest, dragging) {
        recents.getOrNull(nearest)?.let { game ->
            if (game.id != selectedId) onSelect(game.id)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.heroHeight)
            .pointerInput(stepPx, last) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var isDrag = false
                    var origin = position.value
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val fromDown = change.position - down.position
                        if (!isDrag) {
                            if (abs(fromDown.x) > slop && abs(fromDown.x) > abs(fromDown.y)) {
                                isDrag = true
                                origin = if (dragging) dragPos else position.value
                                dragging = true
                                dragPos = origin
                            } else if (abs(fromDown.y) > slop) {
                                return@awaitEachGesture
                            }
                        }
                        if (isDrag) {
                            val raw = origin - fromDown.x / stepPx
                            dragPos = coverflowPos(raw, last)
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                        }
                        if (!change.pressed) break
                    }
                    if (!isDrag) return@awaitEachGesture
                    val velocity = tracker.calculateVelocity().x
                    var target = dragPos.roundToInt()
                    if (velocity > 1100f) target -= 1
                    if (velocity < -1100f) target += 1
                    target = target.coerceIn(0, last.toInt())
                    val from = dragPos
                    val game = recentsLatest.value.getOrNull(target)
                    scope.launch {
                        position.snapTo(from)
                        dragging = false
                        if (game != null) onSelectLatest.value(game.id)
                        position.animateTo(target.toFloat(), settle)
                        runCatching { requesters[target].requestFocus() }
                    }
                }
            }
            .pointerInput(stepPx, last) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scroll = event.changes.fold(0f) { acc, change -> acc + change.scrollDelta.x }
                        if (scroll == 0f) continue
                        event.changes.forEach { it.consume() }
                        val next = coverflowPos(position.value - scroll / stepPx, last)
                            .coerceIn(0f, last)
                        scope.launch {
                            position.snapTo(next)
                            val idx = next.roundToInt().coerceIn(0, last.toInt())
                            recentsLatest.value.getOrNull(idx)?.let { onSelectLatest.value(it.id) }
                        }
                    }
                }
            },
    ) {
        recents.forEachIndexed { index, game ->
            key(game.id) {
                val delta = index - visualPos
                val expand = (1f - abs(delta)).coerceIn(0f, 1f)
                val selected = index == nearest
                val x = recentCoverX(index, visualPos, metrics)
                val y = lerp((metrics.heroHeight - metrics.thumbHeight) / 2, 0.dp, expand)
                val rotationY = when {
                    expand >= 0.99f || !prefs.recentsTilt -> 0f
                    delta < 0f -> tilt * (1f - expand)
                    else -> -tilt * (1f - expand)
                }
                val scale = 0.92f + 0.08f * expand
                val xPx = with(localDensity) { x.toPx() }
                val yPx = with(localDensity) { y.toPx() }
                RecentCard(
                    game = game,
                    selected = selected,
                    growSelected = true,
                    expand = expand,
                    metrics = metrics,
                    preferIcon = prefs.recentsArt == RecentsArt.Icon,
                    onFocused = { onSelect(game.id) },
                    modifier = Modifier
                        .zIndex((recents.size - abs(delta)).toFloat() + expand * 6f)
                        .graphicsLayer {
                            translationX = xPx
                            translationY = yPx
                            this.rotationY = rotationY
                            this.scaleX = scale
                            this.scaleY = scale
                            cameraDistance = 24f * this.density
                            shape = recentsShape
                            clip = true
                            shadowElevation = (4f + 8f * expand) * this.density
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .rowFocus(requesters, index),
                ) {
                    if (selectedId == game.id && !dragging) onOpenGame(game.id) else onSelect(game.id)
                }
            }
        }
    }
}

@Composable
private fun RecentCard(
    game: Game,
    selected: Boolean,
    growSelected: Boolean,
    expand: Float,
    metrics: RecentsMetrics,
    preferIcon: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = cardShape()
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val amount = if (growSelected) expand.coerceIn(0f, 1f) else 0f
    val width = lerp(metrics.thumbWidth, metrics.heroWidth, amount)
    val height = lerp(metrics.thumbHeight, metrics.heroHeight, amount)
    val iconPad = lerp(24.dp, 56.dp, amount)
    val landscape = amount > 0.45f
    Column(
        modifier = modifier
            .width(width)
            .height(height)
            .tileFrame(selected, shape, onFocused)
            .background(Tile)
            .tileClick(onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(hueBrush(game.coverHue, portrait = !landscape)),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkLayer(
                artwork,
                landscape = landscape,
                preferIcon = preferIcon,
                modifier = Modifier.padding(iconPad),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = amount }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    ),
            )
            Text(
                game.title,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
                    .graphicsLayer { alpha = amount },
            )
        }
    }
}

@Composable
private fun FeedTabs(selected: HomeFeedTab, onSelect: (HomeFeedTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShoulderKey("L1") {
            val values = HomeFeedTab.entries
            onSelect(values[(selected.ordinal - 1 + values.size) % values.size])
        }
        Spacer(Modifier.weight(1f))
        SteamPill("What's New", selected == HomeFeedTab.WhatsNew) { onSelect(HomeFeedTab.WhatsNew) }
        Spacer(Modifier.width(28.dp))
        SteamPill("Favorites", selected == HomeFeedTab.Favorites) { onSelect(HomeFeedTab.Favorites) }
        Spacer(Modifier.width(28.dp))
        SteamPill("Recommended", selected == HomeFeedTab.Recommended) { onSelect(HomeFeedTab.Recommended) }
        Spacer(Modifier.weight(1f))
        ShoulderKey("R1") {
            val values = HomeFeedTab.entries
            onSelect(values[(selected.ordinal + 1) % values.size])
        }
    }
}

@Composable
private fun WhatsNewTimeline(
    news: List<NewsItem>,
    snapshot: LibrarySnapshot,
    onOpenGame: (String) -> Unit,
) {
    var selectedNews by remember { mutableStateOf<String?>(null) }
    val cards = news.mapNotNull { item ->
        snapshot.findEntry(item.gameId)?.let { item to it }
    }
    if (cards.isEmpty()) {
        EmptyCenter("No news yet")
        return
    }
    val gap = 16.dp
    val cardWidth = 360.dp
    LazyRow {
        itemsIndexed(cards, key = { _, pair -> pair.first.id }) { index, (item, game) ->
            val kindColor = if (item.kind.contains("BUG", ignoreCase = true)) NewsBugfix else NewsUpdate
            Column(Modifier.width(if (index == cards.lastIndex) cardWidth else cardWidth + gap)) {
                TimelineTick(
                    date = item.date,
                    color = kindColor,
                    first = index == 0,
                    last = index == cards.lastIndex,
                )
                NewsCard(
                    item,
                    game,
                    selected = item.id == selectedNews,
                    onFocused = { selectedNews = item.id },
                    modifier = Modifier.width(cardWidth),
                ) { onOpenGame(item.gameId) }
            }
        }
    }
}

@Composable
private fun TimelineTick(
    date: String,
    color: Color,
    first: Boolean,
    last: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    ) {
        Text(
            date.ifBlank { "Recent" },
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            when {
                first && last -> Unit
                last -> Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(2.dp)
                        .background(TextMuted.copy(alpha = 0.35f)),
                )
                first -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(start = 5.dp)
                        .background(TextMuted.copy(alpha = 0.35f)),
                )
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(TextMuted.copy(alpha = 0.35f)),
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    game: Game,
    selected: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val headerUrl = artwork.imageUrl(landscape = true) ?: item.imageUrl
    val kindColor = if (item.kind.contains("BUG", ignoreCase = true)) NewsBugfix else NewsUpdate
    val shape = cardShape()
    Column(
        modifier = modifier
            .tileFrame(selected, shape, onFocused)
            .background(Tile)
            .tileClick(onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(hueBrush(game.coverHue, portrait = false)),
        ) {
            if (headerUrl != null) {
                AsyncImage(
                    model = headerUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ArtworkLayer(artwork, landscape = true)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.72f)),
                        ),
                    ),
            )
            Text(
                item.kind,
                color = kindColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(12.dp),
            )
            if (item.body.isNotBlank()) {
                Text(
                    item.body,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                item.version,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(hueBrush(game.coverHue)),
                ) {
                    ArtworkLayer(artwork, landscape = false, preferIcon = true)
                }
                Spacer(Modifier.width(8.dp))
                Text(item.gameTitle, color = TextPrimary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RecommendedRow(
    title: String,
    subtitle: String,
    apps: List<InstalledApp>,
    onOpenGame: (String) -> Unit,
) {
    Column {
        Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(apps, key = { _, app -> app.id }) { _, app ->
                if (app.isGame) {
                    CoverArt(
                        title = app.title,
                        hue = app.coverHue,
                        showTitle = true,
                        packageName = app.packageName,
                        isGame = app.isGame,
                        preferIcon = true,
                        modifier = Modifier
                            .width(150.dp)
                            .height(210.dp)
                            .tileClick { onOpenGame(app.id) },
                    )
                } else {
                    AppIconTile(
                        title = app.title,
                        hue = app.coverHue,
                        packageName = app.packageName,
                        modifier = Modifier
                            .width(150.dp)
                            .height(210.dp),
                        onClick = { onOpenGame(app.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCenter(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = TextMuted, fontSize = 16.sp)
    }
}
