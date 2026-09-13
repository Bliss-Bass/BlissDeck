package org.gamelauncher.ui.game

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.gamelauncher.data.ArtCandidate
import org.gamelauncher.data.ArtSlot
import org.gamelauncher.data.Artwork
import org.gamelauncher.data.AppPresence
import org.gamelauncher.data.AppRunState
import org.gamelauncher.data.Game
import org.gamelauncher.data.GamePageTab
import org.gamelauncher.data.GameSession
import org.gamelauncher.data.LaunchIntentKind
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.data.LocalCollections
import org.gamelauncher.data.LocalDetails
import org.gamelauncher.data.LocalNews
import org.gamelauncher.data.LocalPlayHistory
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.LocalTitles
import org.gamelauncher.data.NewsItem
import org.gamelauncher.data.SteamNative
import org.gamelauncher.data.TitleDetails
import org.gamelauncher.data.TitleOverride
import org.gamelauncher.data.UNITY_FORCE_GLES
import org.gamelauncher.data.UNITY_FORCE_VULKAN
import org.gamelauncher.data.WindowingMode
import org.gamelauncher.data.toInstalledApp
import org.gamelauncher.ui.components.AmbientBackdrop
import org.gamelauncher.ui.components.GameIcon
import org.gamelauncher.ui.components.ArtworkLayer
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.HoverCaption
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.cardShape
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.components.hueBrush
import org.gamelauncher.ui.components.rememberArtwork
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.Footer
import org.gamelauncher.ui.theme.Menu
import org.gamelauncher.ui.theme.NewsBugfix
import org.gamelauncher.ui.theme.NewsUpdate
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.StopRed
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.frosted
import kotlin.math.roundToInt

@Composable
fun GameScreen(game: Game, newsId: String? = null) {
    var tab by remember { mutableStateOf(GamePageTab.Activity) }
    val visibleTabs = LocalTheme.current.layouts.gameTabs()
    LaunchedEffect(visibleTabs) {
        if (tab !in visibleTabs) tab = visibleTabs.first()
    }
    var collectionsOpen by remember { mutableStateOf(false) }
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val details = rememberTitleDetails(game)
    val history = LocalPlayHistory.current
    val historyEpoch by history.epoch.collectAsState()
    val lastPlayed = remember(historyEpoch, game.packageName) { history.lastPlayedLabel(game.packageName) }
    val playTime = remember(historyEpoch, game.packageName) { history.playTimeLabel(game.packageName) }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(artwork)
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = LocalTheme.current.chrome.bottomBarHeight),
        ) {
            GameHero(game, artwork)
            GamePlayBar(
                game = game,
                details = details,
                lastPlayed = lastPlayed,
                playTime = playTime,
                onCollections = { collectionsOpen = true },
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                ShoulderTabs(tab) { tab = it }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    GamePageTab.Activity -> ActivityPane(game, details, newsId)
                    GamePageTab.Community -> CommunityPane(game, details)
                    GamePageTab.GameInfo -> GameInfoPane(game, details)
                }
            }
        }
        if (collectionsOpen) {
            CollectionPicker(game.packageName) { collectionsOpen = false }
        }
    }
}

@Composable
private fun rememberTitleDetails(game: Game): TitleDetails {
    val repo = LocalDetails.current
    val steamId = SteamNative.steamAppId(game.packageName)
    val epoch by repo.epoch.collectAsState()
    val details by produceState(repo.peek(game.packageName) ?: TitleDetails(), game.packageName, epoch) {
        value = repo.resolve(game.packageName, steamId)
    }
    return details
}

@Composable
private fun GameHero(game: Game, artwork: Artwork) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(hueBrush(game.coverHue, portrait = false)),
        contentAlignment = Alignment.Center,
    ) {
        ArtworkLayer(artwork, landscape = true)
        GameIcon(
            artwork,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 18.dp)
                .size(84.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
        )
        if (artwork.imageUrl(true) == null) {
            Text(
                game.title.uppercase(),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun GamePlayBar(
    game: Game,
    details: TitleDetails,
    lastPlayed: String,
    playTime: String,
    onCollections: () -> Unit,
) {
    val context = LocalContext.current
    val history = LocalPlayHistory.current
    val prefs by LocalSettings.current.state.collectAsState()
    val titles = LocalTitles.current
    val overrides by titles.state.collectAsState()
    val title = overrides[game.packageName] ?: TitleOverride()
    val presence by AppPresence.snapshot.collectAsState()
    val mapperInstalled = remember { GameSession.hasXtMapper(context) }
    var actionsOpen by remember { mutableStateOf(false) }
    var launchEditorOpen by remember { mutableStateOf(false) }
    var amRunning by remember(game.packageName) { mutableStateOf(false) }
    LaunchedEffect(game.packageName, presence.connected) {
        if (presence.connected) return@LaunchedEffect
        while (true) {
            amRunning = GameSession.running(context, game.packageName) == true
            delay(1500)
        }
    }
    val runState = when {
        presence.connected -> AppPresence.state(game.packageName, presence)
        amRunning -> AppRunState.Running
        else -> AppRunState.Stopped
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Footer)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .width(240.dp)
                .height(56.dp)
                .clip(cardShape())
                .background(
                    when (runState) {
                        AppRunState.Stopped -> PlayGreen
                        AppRunState.Running -> StopRed
                        AppRunState.Closing -> Color(0xFF8A6A32)
                    },
                )
                .clickable(enabled = runState != AppRunState.Closing) {
                    when (runState) {
                        AppRunState.Stopped -> {
                            if (GameSession.launch(context, game.packageName, prefs, title)) {
                                history.record(game.packageName)
                            }
                        }
                        AppRunState.Running -> GameSession.close(context, game.packageName)
                        AppRunState.Closing -> Unit
                    }
                }
                .semantics {
                    contentDescription = when (runState) {
                        AppRunState.Stopped -> "Play"
                        AppRunState.Running -> "Running"
                        AppRunState.Closing -> "Closing"
                    }
                }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (runState == AppRunState.Stopped) {
                    Icons.Default.PlayArrow
                } else {
                    Icons.Default.Close
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when (runState) {
                    AppRunState.Stopped -> "Play"
                    AppRunState.Running -> "Running"
                    AppRunState.Closing -> "Closing"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(28.dp))
        Stat("Last Played", lastPlayed)
        Spacer(Modifier.width(28.dp))
        Stat("Play Time", playTime)
        Spacer(Modifier.width(28.dp))
        Column {
            Text("RATING", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
            RatingStars(details.rating)
        }
        Spacer(Modifier.weight(1f))
        Glyph(
            Icons.Default.SportsEsports,
            onClick = { GameSession.openXtMapper(context) },
            filled = mapperInstalled,
            description = "Open XTMapper",
        )
        Spacer(Modifier.width(10.dp))
        val density = LocalDensity.current
        Box {
            Glyph(
                Icons.Default.Settings,
                onClick = { actionsOpen = !actionsOpen },
                filled = title.hasLaunchTweaks,
                description = "Game options",
            )
            if (actionsOpen) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(0, with(density) { 56.dp.roundToPx() }),
                    onDismissRequest = { actionsOpen = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    GameActionsMenu(
                        game = game,
                        override = title,
                        onToggleGame = {
                            val want = !game.inLibrary
                            titles.update(game.packageName) {
                                it.copy(markedGame = if (want == game.detectedGame) null else want)
                            }
                        },
                        onToggleMedia = {
                            val want = !game.isMedia
                            titles.update(game.packageName) {
                                it.copy(markedMedia = if (want == game.detectedMedia) null else want)
                            }
                        },
                        onLaunchOptions = {
                            actionsOpen = false
                            launchEditorOpen = true
                        },
                        onAppInfo = {
                            actionsOpen = false
                            GameSession.openAppInfo(context, game.packageName)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Glyph(Icons.Default.Folder, onClick = onCollections, description = "Collections")
    }
    if (launchEditorOpen) {
        Dialog(onDismissRequest = { launchEditorOpen = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .clip(cardShape())
                    .background(Tile)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                LaunchOverridesCard(game, showGameToggle = false, framed = false)
                Text(
                    "Done",
                    color = PlayGreen,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { launchEditorOpen = false }
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ActivityPane(game: Game, details: TitleDetails, highlightNewsId: String?) {
    val newsRepo = LocalNews.current
    val app = remember(game.id, game.packageName, game.isMedia, game.inLibrary) { game.toInstalledApp() }
    val cached = remember(game.packageName) { newsRepo.peekNews(app) }
    val loaded by produceState(cached to cached.isEmpty(), game.packageName) {
        value = runCatching { newsRepo.newsFor(app) }.getOrDefault(cached) to false
    }
    val items = loaded.first
    val loading = loaded.second
    val history = LocalPlayHistory.current
    val historyEpoch by history.epoch.collectAsState()
    val lastPlayed = remember(historyEpoch, game.packageName) { history.lastPlayedLabel(game.packageName) }
    val playTime = remember(historyEpoch, game.packageName) { history.playTimeLabel(game.packageName) }
    val launched = remember(historyEpoch, game.packageName) { history.launchCount(game.packageName) > 0 }
    val players = if (details.players.isNotBlank()) details.players else game.players

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(18.dp)
                    .background(hueBrush(game.coverHue, portrait = false)),
            )
            Spacer(Modifier.width(12.dp))
            Text(game.title, color = TextPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Person, contentDescription = null, tint = TextPrimary)
            Spacer(Modifier.width(6.dp))
            Text(players, color = TextPrimary, fontSize = 15.sp)
        }
        if (launched) {
            Spacer(Modifier.height(18.dp))
            Text("Played", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("$lastPlayed · $playTime", color = TextMuted, fontSize = 16.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text("Updates", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        when {
            loading && items.isEmpty() -> Text("Checking Play Store…", color = TextMuted, fontSize = 16.sp)
            items.isEmpty() -> Text("No updates for this title yet", color = TextMuted, fontSize = 16.sp)
            else -> {
                items.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    ActivityUpdateCard(item, game, selected = item.id == highlightNewsId)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActivityUpdateCard(item: NewsItem, game: Game, selected: Boolean) {
    val kindColor = if (item.kind.contains("BUG", ignoreCase = true)) NewsBugfix else NewsUpdate
    val shape = cardShape()
    val banner = item.imageUrl
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tileFrame(selected, shape)
            .clip(shape)
            .background(Tile),
    ) {
        if (!banner.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(hueBrush(game.coverHue, portrait = false)),
            ) {
                AsyncImage(
                    model = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                item.kind,
                color = kindColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            if (item.date.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.date, color = TextMuted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                item.version,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            val text = item.fullText
            if (text.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(text, color = TextPrimary, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun CommunityPane(game: Game, details: TitleDetails) {
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Average Review: ${if (details.rating > 0f) String.format("%.1f", details.rating) else "—"}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(16.dp))
            RatingStars(details.rating, 22.dp)
        }
        Spacer(Modifier.height(22.dp))
        Text("Screenshots", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        val shots = details.screenshots.ifEmpty {
            listOfNotNull(artwork.heroUrl ?: artwork.coverUrl)
        }
        if (shots.isEmpty()) {
            Text("No screenshots yet", color = TextMuted, fontSize = 16.sp)
        } else {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            shots.take(6).forEach { url ->
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(200.dp)
                        .clip(cardShape())
                        .background(hueBrush(game.coverHue, portrait = false)),
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun GameInfoPane(game: Game, details: TitleDetails) {
    val context = LocalContext.current
    val repo = LocalArtwork.current
    var showChangeId by remember { mutableStateOf(false) }
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val steamId = artwork.steamGridId.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            CoverArt(
                game.title,
                game.coverHue,
                packageName = game.packageName,
                isGame = game.inLibrary,
                modifier = Modifier.width(150.dp).height(220.dp),
            )
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    details.summary.ifBlank { game.summary },
                    color = TextPrimary,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(22.dp))
                MetaLine("Developer", details.developer.ifBlank { game.developer })
                MetaLine("Publisher", details.publisher.ifBlank { game.publisher })
                MetaLine("Category", details.category.ifBlank { game.category })
                MetaLine("Release Date", details.releaseDate.ifBlank { game.releaseDate })
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (details.players.isNotBlank()) details.players else game.players, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (details.controller.isNotBlank()) details.controller else game.controller, color = TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        ArtPicker(game, artwork)
        Spacer(Modifier.height(28.dp))
        LaunchOverridesCard(game)
        Spacer(Modifier.height(28.dp))
        Text(
            "You can change the SteamGrid ID per game and suggest it for future releases. Check the box ‘Suggest this ID’ only for correct IDs",
            color = TextMuted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape())
                .background(Tile)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Steam Grid DB Metadata", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text("ID ${steamId.ifBlank { "—" }}", color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            InfoAction("View Page") {
                val id = steamId.ifBlank { return@InfoAction }
                openUrl(context, "https://www.steamgriddb.com/game/$id")
            }
            Spacer(Modifier.width(10.dp))
            InfoAction("Change ID") { showChangeId = true }
            Spacer(Modifier.width(10.dp))
            InfoAction("Store Page") {
                openUrl(context, "https://play.google.com/store/apps/details?id=${game.packageName}")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(game.packageName, color = TextMuted, fontSize = 15.sp)
        Text(game.title, color = TextMuted, fontSize = 15.sp)
    }

    if (showChangeId) {
        Dialog(onDismissRequest = { showChangeId = false }) {
            ChangeIdSheet(
                current = steamId,
                onDismiss = { showChangeId = false },
                onSave = {
                    repo.setSteamGridId(game.packageName, it)
                    showChangeId = false
                },
            )
        }
    }
}

@Composable
private fun ArtPicker(game: Game, artwork: Artwork) {
    val repo = LocalArtwork.current
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Artwork", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Pick a tall card, backdrop, and icon from SteamGridDB. Defaults for new games live in Settings.",
            color = TextMuted,
            fontSize = 14.sp,
        )
        ArtSlot.entries.forEach { slot ->
            ArtChoiceRow(game, artwork, slot) { url -> repo.select(game.packageName, slot, url) }
        }
    }
}

@Composable
private fun ArtChoiceRow(
    game: Game,
    artwork: Artwork,
    slot: ArtSlot,
    onSelect: (String) -> Unit,
) {
    val repo = LocalArtwork.current
    val options by produceState(emptyList<ArtCandidate>(), game.packageName, artwork.steamGridId, slot, repo.epoch.collectAsState().value) {
        value = repo.candidates(game.packageName, game.title, game.inLibrary, slot)
    }
    val selected = when (slot) {
        ArtSlot.Cover -> artwork.coverUrl
        ArtSlot.Hero -> artwork.heroUrl
        ArtSlot.Icon -> artwork.iconUrl ?: ArtCandidate.APP_ICON
    }
    val label = when (slot) {
        ArtSlot.Cover -> "Tall card"
        ArtSlot.Hero -> "Backdrop"
        ArtSlot.Icon -> "Icon"
    }
    Column {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        if (options.isEmpty()) {
            Text(
                if (repo.apiKey.isBlank()) "Add a SteamGridDB API key in Settings to load more art."
                else "No ${label.lowercase()} options yet.",
                color = TextMuted,
                fontSize = 14.sp,
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                options.forEach { option ->
                    val picked = option.url == selected || (option.isAppIcon && artwork.usesAppIcon)
                    val (w, h) = when (slot) {
                        ArtSlot.Cover -> 72.dp to 108.dp
                        ArtSlot.Hero -> 168.dp to 54.dp
                        ArtSlot.Icon -> 64.dp to 64.dp
                    }
                    val caption = if (option.isAppIcon) "App icon" else option.source
                    HoverCaption(caption) {
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(h)
                            .tileFrame(picked, cardShape(), width = 2.dp)
                            .background(Tile, cardShape())
                            .tileClick { onSelect(option.url) }
                            .clip(cardShape()),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            option.isAppIcon -> GameIcon(artwork.copy(iconUrl = ArtCandidate.APP_ICON), Modifier.fillMaxSize().padding(8.dp))
                            else -> AsyncImage(
                                model = option.thumbUrl,
                                contentDescription = option.source,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeIdSheet(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    Column(
        modifier = Modifier
            .width(420.dp)
            .clip(cardShape())
            .background(Tile)
            .padding(24.dp),
    ) {
        Text("Change SteamGrid ID", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Background, RoundedCornerShape(4.dp))
                .padding(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoAction("Cancel", onDismiss)
            InfoAction("Save") { onSave(value) }
        }
    }
}

@Composable
private fun ShoulderTabs(selected: GamePageTab, onTab: (GamePageTab) -> Unit) {
    val values = LocalTheme.current.layouts.gameTabs()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ShoulderKey("L1") {
            val i = values.indexOf(selected).let { if (it < 0) 0 else it }
            onTab(values[(i - 1 + values.size) % values.size])
        }
        Spacer(Modifier.weight(1f))
        CenteredTabs(selected, onTab, values)
        Spacer(Modifier.weight(1f))
        ShoulderKey("R1") {
            val i = values.indexOf(selected).let { if (it < 0) 0 else it }
            onTab(values[(i + 1) % values.size])
        }
    }
}

@Composable
private fun CenteredTabs(
    selected: GamePageTab,
    onTab: (GamePageTab) -> Unit,
    values: List<GamePageTab> = LocalTheme.current.layouts.gameTabs(),
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEachIndexed { index, tab ->
            if (index > 0) Spacer(Modifier.width(24.dp))
            val label = when (tab) {
                GamePageTab.Activity -> "Activity"
                GamePageTab.Community -> "Community"
                GamePageTab.GameInfo -> "Game Info"
            }
            SteamPill(label, selected == tab) { onTab(tab) }
        }
    }
}

@Composable
private fun RatingStars(rating: Float, size: Dp = 18.dp) {
    val filled = rating.roundToInt().coerceIn(0, 5)
    Row {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < filled) Icons.Default.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
private fun CollectionPicker(packageName: String, onDismiss: () -> Unit) {
    val store = LocalCollections.current
    val collections by store.state.collectAsState()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(cardShape())
                .background(Tile)
                .padding(24.dp),
        ) {
            Text("Collections", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            collections.forEach { collection ->
                val included = collection.contains(packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { store.toggle(collection.id, packageName) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (included) "Added" else "Add",
                        color = if (included) PlayGreen else TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.width(64.dp),
                    )
                    Text(collection.name, color = TextPrimary, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Done",
                color = PlayGreen,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onDismiss)
                    .padding(8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LaunchOverridesCard(game: Game, showGameToggle: Boolean = true, framed: Boolean = true) {
    val titles = LocalTitles.current
    val overrides by titles.state.collectAsState()
    val override = overrides[game.packageName] ?: TitleOverride()
    var extras by remember(game.packageName) { mutableStateOf(override.extras) }
    var activity by remember(game.packageName) { mutableStateOf(override.activity) }

    fun edit(block: (TitleOverride) -> TitleOverride) {
        titles.update(game.packageName, block)
    }

    Column(
        modifier = if (framed) {
            Modifier
                .fillMaxWidth()
                .clip(cardShape())
                .background(Tile)
                .padding(18.dp)
        } else {
            Modifier.fillMaxWidth().padding(10.dp)
        },
    ) {
        Text("Launch options", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (game.hasLeanback) {
                "Leanback is available on this app. Windowing and extras apply to Play."
            } else {
                "Windowing, launch activity, and extras apply to Play."
            },
            color = TextMuted,
            fontSize = 14.sp,
        )
        if (showGameToggle) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tileFrame(false, cardShape(), width = 2.dp)
                .tileClick {
                    val want = !game.inLibrary
                    edit { it.copy(markedGame = if (want == game.detectedGame) null else want) }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Treat as game", color = TextPrimary, fontSize = 15.sp)
                Text(
                    if (override.markedGame == null) {
                        if (game.detectedGame) "Detected as a game" else "Detected as an app"
                    } else {
                        "Overridden"
                    },
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }
            Text(
                if (game.inLibrary) "On" else "Off",
                color = if (game.inLibrary) PlayGreen else TextMuted,
                fontSize = 15.sp,
            )
        }
        }
        Spacer(Modifier.height(16.dp))
        Text("Windowing", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SteamPill("Default", override.windowing == null) { edit { it.copy(windowing = null) } }
            WindowingMode.entries.forEach { mode ->
                SteamPill(mode.name, override.windowing == mode) { edit { it.copy(windowing = mode) } }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Launch activity", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SteamPill("Default", override.launchIntent == null) {
                edit { it.copy(launchIntent = null) }
            }
            SteamPill("App launcher", override.launchIntent == LaunchIntentKind.Launcher) {
                edit { it.copy(launchIntent = LaunchIntentKind.Launcher) }
            }
            SteamPill(
                if (game.hasLeanback) "Leanback" else "Leanback (none)",
                override.launchIntent == LaunchIntentKind.Leanback,
            ) {
                edit { it.copy(launchIntent = LaunchIntentKind.Leanback) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Activity class (optional)", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = activity,
            onValueChange = {
                activity = it
                edit { cur -> cur.copy(activity = it.trim()) }
            },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(TextPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .background(Background, RoundedCornerShape(4.dp))
                .padding(12.dp),
            decorationBox = { inner ->
                if (activity.isEmpty()) {
                    Text("com.unity3d.player.UnityPlayerActivity", color = TextMuted, fontSize = 16.sp)
                }
                inner()
            },
        )
        Spacer(Modifier.height(16.dp))
        Text("Arguments", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Same flags as adb am start extras, for example --es unity -force-gles",
            color = TextMuted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = extras,
            onValueChange = {
                extras = it
                edit { cur -> cur.copy(extras = it) }
            },
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(TextPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Background, RoundedCornerShape(4.dp))
                .padding(12.dp),
            decorationBox = { inner ->
                if (extras.isEmpty()) {
                    Text("--es unity -force-gles", color = TextMuted, fontSize = 16.sp)
                }
                inner()
            },
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SteamPill("Force GLES", extras.contains("-force-gles")) {
                val next = mergeArg(extras, UNITY_FORCE_GLES)
                extras = next
                edit { it.copy(extras = next) }
            }
            SteamPill("Force Vulkan", extras.contains("-force-vulkan")) {
                val next = mergeArg(extras, UNITY_FORCE_VULKAN)
                extras = next
                edit { it.copy(extras = next) }
            }
        }
    }
}

private fun mergeArg(current: String, snippet: String): String {
    if (current.contains(snippet) || current.contains(snippet.removePrefix("--es ").trim())) {
        return current.trim()
    }
    return (current.trim() + " " + snippet).trim()
}

private val TitleOverride.hasLaunchTweaks: Boolean
    get() = windowing != null ||
        launchIntent != null ||
        extras.isNotBlank() ||
        activity.isNotBlank()

@Composable
private fun GameActionsMenu(
    game: Game,
    override: TitleOverride,
    onToggleGame: () -> Unit,
    onToggleMedia: () -> Unit,
    onLaunchOptions: () -> Unit,
    onAppInfo: () -> Unit,
) {
    val menuColor = Menu.copy(alpha = LocalTheme.current.chrome.menuAlpha)
    val argsHint = when {
        override.extras.isNotBlank() -> override.extras.trim()
        override.activity.isNotBlank() -> override.activity
        override.windowing != null -> override.windowing.name
        override.launchIntent == LaunchIntentKind.Leanback -> "Leanback"
        else -> "Windowing, Leanback, extras"
    }
    Column(
        modifier = Modifier
            .width(320.dp)
            .clip(cardShape())
            .frosted(menuColor)
            .padding(vertical = 8.dp),
    ) {
        GameActionRow(
            label = "Treat as game",
            icon = Icons.Default.SportsEsports,
            status = if (game.inLibrary) "On" else "Off",
            onClick = onToggleGame,
        )
        GameActionRow(
            label = "Treat as media",
            icon = Icons.Default.LiveTv,
            status = if (game.isMedia) "On" else "Off",
            onClick = onToggleMedia,
        )
        GameActionRow(
            label = "Launch options",
            icon = Icons.Default.Tune,
            status = argsHint,
            onClick = onLaunchOptions,
        )
        GameActionRow(
            label = "App info",
            icon = Icons.Default.Info,
            onClick = onAppInfo,
        )
    }
}

@Composable
private fun GameActionRow(
    label: String,
    icon: ImageVector,
    status: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 16.sp)
            if (status != null) {
                Text(
                    status,
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Glyph(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {},
    filled: Boolean = true,
    description: String? = null,
) {
    HoverCaption(description.orEmpty()) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(cardShape())
                .background(Tile)
                .clickable(onClick = onClick)
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (filled) TextPrimary else TextMuted, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row {
        Text("$label: ", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(value, color = TextPrimary, fontSize = 15.sp)
    }
}

@Composable
private fun InfoAction(label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .clip(cardShape())
            .background(Color(0xFF3A424A))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
