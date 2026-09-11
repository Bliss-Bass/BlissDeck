package org.gamelauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.NewsBugfix
import org.gamelauncher.ui.theme.NewsUpdate
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.TileBorder

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
        Text("Recent games", color = TextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            recents.forEach { game ->
                val wide = game.id == selectedId
                CoverArt(
                    title = game.title,
                    hue = game.coverHue,
                    selected = wide,
                    showTitle = true,
                    packageName = game.packageName,
                    isGame = game.inLibrary,
                    landscape = true,
                    preferIcon = true,
                    modifier = Modifier
                        .width(if (wide) 340.dp else 170.dp)
                        .height(168.dp)
                        .clickable {
                            if (selectedId == game.id) onOpenGame(game.id) else selectedId = game.id
                        },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(selected.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PlayGreen)
            Text(
                "PLAY NOW!",
                color = PlayGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onOpenGame(selected.id) },
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
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        news.forEachIndexed { index, item ->
            val game = snapshot.findEntry(item.gameId) ?: return@forEachIndexed
            NewsCard(item, game, selected = index == 0) { onOpenGame(item.gameId) }
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    game: Game,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val headerUrl = artwork.imageUrl(landscape = true) ?: item.imageUrl
    val kindColor = if (item.kind.contains("BUG", ignoreCase = true)) NewsBugfix else NewsUpdate
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = Modifier
            .width(360.dp)
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, TileBorder, shape) else Modifier)
            .background(Tile)
            .clickable(onClick = onClick),
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
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            apps.forEach { app ->
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
                            .clickable { onOpenGame(app.id) },
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
