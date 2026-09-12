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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.gamelauncher.data.AppPresence
import org.gamelauncher.data.AppRunState
import org.gamelauncher.data.Game
import org.gamelauncher.data.GamePageTab
import org.gamelauncher.data.GameSession
import kotlin.math.roundToInt
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.data.LocalCollections
import org.gamelauncher.data.LocalDetails
import org.gamelauncher.data.LocalPlayHistory
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.SteamNative
import org.gamelauncher.data.TitleDetails
import org.gamelauncher.ui.components.AmbientBackdrop
import org.gamelauncher.ui.components.AppIconImage
import org.gamelauncher.ui.components.ArtworkLayer
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.hueBrush
import org.gamelauncher.ui.components.rememberArtwork
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.Footer
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.StopRed
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

@Composable
fun GameScreen(game: Game) {
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
            when (tab) {
                GamePageTab.Activity -> ActivityPage(
                    game = game,
                    details = details,
                    lastPlayed = lastPlayed,
                    playTime = playTime,
                    onCollections = { collectionsOpen = true },
                    onTab = { tab = it },
                )
                GamePageTab.Community -> CommunityPage(game, details) { tab = it }
                GamePageTab.GameInfo -> GameInfoPage(game, details) { tab = it }
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
private fun ActivityPage(
    game: Game,
    details: TitleDetails,
    lastPlayed: String,
    playTime: String,
    onCollections: () -> Unit,
    onTab: (GamePageTab) -> Unit,
) {
    val context = LocalContext.current
    val history = LocalPlayHistory.current
    val presence by AppPresence.snapshot.collectAsState()
    val mapperInstalled = remember { GameSession.hasXtMapper(context) }
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

    Column(Modifier.fillMaxSize()) {
        val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(hueBrush(game.coverHue, portrait = false)),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkLayer(artwork, landscape = true)
            artwork.icon?.let { icon ->
                AppIconImage(
                    icon,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 18.dp)
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
                )
            }
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
                    .clip(RoundedCornerShape(2.dp))
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
                                if (GameSession.launch(context, game.packageName)) {
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
            Glyph(
                Icons.Default.Settings,
                onClick = { GameSession.openAppInfo(context, game.packageName) },
                description = "App info",
            )
            Spacer(Modifier.width(10.dp))
            Glyph(Icons.Default.Folder, onClick = onCollections, description = "Collections")
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CenteredTabs(GamePageTab.Activity, onTab)
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Text(if (details.players.isNotBlank()) details.players else game.players, color = TextPrimary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CommunityPage(game: Game, details: TitleDetails, onTab: (GamePageTab) -> Unit) {
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        ShoulderTabs(GamePageTab.Community, onTab)
        Spacer(Modifier.height(28.dp))
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
                        .clip(RoundedCornerShape(2.dp))
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
private fun GameInfoPage(game: Game, details: TitleDetails, onTab: (GamePageTab) -> Unit) {
    val context = LocalContext.current
    val repo = LocalArtwork.current
    var showChangeId by remember { mutableStateOf(false) }
    val artwork = rememberArtwork(game.packageName, game.title, game.inLibrary)
    val steamId = artwork.steamGridId.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        ShoulderTabs(GamePageTab.GameInfo, onTab)
        Spacer(Modifier.height(28.dp))
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
        Text(
            "You can change the SteamGrid ID per game and suggest it for future releases. Check the box ‘Suggest this ID’ only for correct IDs",
            color = TextMuted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
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
private fun ChangeIdSheet(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    Column(
        modifier = Modifier
            .width(420.dp)
            .clip(RoundedCornerShape(6.dp))
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
                .background(Tile, RoundedCornerShape(6.dp))
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
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Tile)
            .clickable(onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) TextPrimary else TextMuted, modifier = Modifier.size(26.dp))
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
            .clip(RoundedCornerShape(2.dp))
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
