package org.gamelauncher.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import coil.compose.AsyncImage
import org.gamelauncher.data.Game
import org.gamelauncher.data.HomeFeedTab
import org.gamelauncher.data.InstalledApp
import org.gamelauncher.data.LibrarySnapshot
import org.gamelauncher.data.NewsItem
import org.gamelauncher.data.findEntry
import org.gamelauncher.data.toGame
import org.gamelauncher.ui.components.AmbientBackdrop
import org.gamelauncher.ui.components.AppIconTile
import org.gamelauncher.ui.components.ArtworkLayer
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
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

@Composable
fun HomeScreen(
    snapshot: LibrarySnapshot,
    news: List<NewsItem>,
    newsLoading: Boolean,
    onOpenGame: (String) -> Unit,
) {
    val recents = snapshot.libraryGames.ifEmpty {
        snapshot.installed.take(8).map { it.toGame() }
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

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(selectedArt)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 18.dp, bottom = 12.dp),
        ) {
        Text("Recent games", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        RecentsRow(recents, selectedId, onSelect = { selectedId = it }, onOpenGame = onOpenGame)
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
        Spacer(Modifier.height(28.dp))
        FeedTabs(feed) { feed = it }
        Spacer(Modifier.height(16.dp))
        when (feed) {
            HomeFeedTab.WhatsNew -> {
                when {
                    news.isNotEmpty() -> WhatsNewRow(news, snapshot, onOpenGame)
                    newsLoading -> EmptyCenter("Checking Play Store…")
                    else -> EmptyCenter("No news yet")
                }
            }
            HomeFeedTab.Favorites -> EmptyCenter("No favorites yet")
            HomeFeedTab.Recommended -> RecommendedRow(snapshot.installed, onOpenGame)
        }
        }
    }
}

private val RecentHeroWidth = 680.dp
private val RecentHeroHeight = 336.dp
private val RecentThumb = 228.dp
private val RecentPeek = 152.dp
private val RecentHeroOverlap = 28.dp

private fun recentCoverX(index: Int, selectedIndex: Int): Dp {
    val leftStack = RecentPeek * selectedIndex
    val delta = index - selectedIndex
    return when {
        delta < 0 -> RecentPeek * index
        delta == 0 -> leftStack
        else -> leftStack + RecentHeroWidth - RecentHeroOverlap + RecentPeek * (delta - 1)
    }
}

@Composable
private fun RecentsRow(
    recents: List<Game>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onOpenGame: (String) -> Unit,
) {
    val selectedIndex = recents.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val requesters = remember(recents.map { it.id }) { List(recents.size) { FocusRequester() } }
    val spec = spring<Dp>(
        dampingRatio = 0.84f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val rotSpec = spring<Float>(
        dampingRatio = 0.84f,
        stiffness = Spring.StiffnessMediumLow,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RecentHeroHeight),
    ) {
        recents.forEachIndexed { index, game ->
            key(game.id) {
                val delta = index - selectedIndex
                val selected = delta == 0
                val x by animateDpAsState(recentCoverX(index, selectedIndex), spec, label = "recent-x-$index")
                val y by animateDpAsState(
                    if (selected) 0.dp else (RecentHeroHeight - RecentThumb) / 2,
                    spec,
                    label = "recent-y-$index",
                )
                val rotationY by animateFloatAsState(
                    when {
                        selected -> 0f
                        delta < 0 -> 8f
                        else -> -8f
                    },
                    rotSpec,
                    label = "recent-rot-$index",
                )
                val elevation by animateFloatAsState(
                    if (selected) 12f else 4f - abs(delta).coerceAtMost(3),
                    rotSpec,
                    label = "recent-z-$index",
                )
                val scale by animateFloatAsState(
                    when {
                        selected -> 1f
                        abs(delta) == 1 -> 0.97f
                        else -> 0.92f
                    },
                    rotSpec,
                    label = "recent-scale-$index",
                )
                RecentCard(
                    game = game,
                    selected = selected,
                    onFocused = { onSelect(game.id) },
                    modifier = Modifier
                        .zIndex((recents.size - abs(delta)).toFloat() + if (selected) 6f else 0f)
                        .offset(x, y)
                        .graphicsLayer {
                            this.rotationY = rotationY
                            this.scaleX = scale
                            this.scaleY = scale
                            cameraDistance = 24f * density
                            shadowElevation = elevation * density
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .rowFocus(requesters, index),
                ) {
                    if (selectedId == game.id) onOpenGame(game.id) else onSelect(game.id)
                }
            }
        }
    }
}

@Composable
private fun RecentCard(
    game: Game,
    selected: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val shape = RoundedCornerShape(6.dp)
    val spec = spring<Dp>(
        dampingRatio = 0.84f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val width by animateDpAsState(if (selected) RecentHeroWidth else RecentThumb, spec, label = "recent-w")
    val height by animateDpAsState(if (selected) RecentHeroHeight else RecentThumb, spec, label = "recent-h")
    val iconPad by animateDpAsState(if (selected) 56.dp else 24.dp, spec, label = "recent-pad")
    val titleAlpha by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
        label = "recent-title",
    )
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
                .background(hueBrush(game.coverHue, portrait = !selected)),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkLayer(
                artwork,
                landscape = selected,
                preferIcon = true,
                modifier = Modifier.padding(iconPad),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = titleAlpha }
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
                    .graphicsLayer { alpha = titleAlpha },
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
private fun WhatsNewRow(
    news: List<NewsItem>,
    snapshot: LibrarySnapshot,
    onOpenGame: (String) -> Unit,
) {
    var selectedNews by remember { mutableStateOf<String?>(null) }
    val cards = news.mapNotNull { item ->
        snapshot.findEntry(item.gameId)?.let { item to it }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(cards, key = { _, pair -> pair.first.id }) { _, (item, game) ->
            NewsCard(
                item,
                game,
                selected = item.id == selectedNews,
                onFocused = { selectedNews = item.id },
            ) { onOpenGame(item.gameId) }
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
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = modifier
            .width(360.dp)
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
            if (item.date.isNotBlank()) {
                Text(item.date, color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
            }
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
private fun RecommendedRow(apps: List<InstalledApp>, onOpenGame: (String) -> Unit) {
    Column {
        Text("Play next from your library", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Players like you love these unplayed games in your library", color = TextMuted, fontSize = 14.sp)
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
