package org.gamelauncher.ui.game

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.gamelauncher.data.Game
import org.gamelauncher.data.GamePageTab
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.hueBrush
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.Footer
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

@Composable
fun GameScreen(game: Game) {
    var tab by remember { mutableStateOf(GamePageTab.Activity) }
    var favorite by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Background)) {
        when (tab) {
            GamePageTab.Activity -> ActivityPage(game, favorite, { favorite = !favorite }, { tab = it })
            GamePageTab.Community -> CommunityPage(game) { tab = it }
            GamePageTab.GameInfo -> GameInfoPage(game) { tab = it }
        }
    }
}

@Composable
private fun ActivityPage(
    game: Game,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onTab: (GamePageTab) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(hueBrush(game.coverHue, portrait = false)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                game.title.uppercase(),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
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
                    .background(PlayGreen)
                    .clickable { }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Text("Play", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(28.dp))
            Stat("Last Played", game.lastPlayed)
            Spacer(Modifier.width(28.dp))
            Stat("Play Time", game.playTime)
            Spacer(Modifier.width(28.dp))
            Column {
                Text("RATING", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
                Row {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < game.rating.toInt()) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Glyph(Icons.Default.SportsEsports)
            Spacer(Modifier.width(10.dp))
            Glyph(Icons.Default.Settings)
            Spacer(Modifier.width(10.dp))
            Glyph(Icons.Default.Star, onClick = onFavorite, filled = favorite)
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
            Text(game.players, color = TextPrimary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CommunityPage(game: Game, onTab: (GamePageTab) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        ShoulderTabs(GamePageTab.Community, onTab)
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Average Review: ${game.rating}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(16.dp))
            repeat(5) { i ->
                Icon(
                    imageVector = if (i < game.rating.toInt()) Icons.Default.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("Screenshots", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(game.coverHue, game.coverHue + 30f, game.coverHue + 70f).forEach { hue ->
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(hueBrush(hue, portrait = false)),
                )
            }
        }
    }
}

@Composable
private fun GameInfoPage(game: Game, onTab: (GamePageTab) -> Unit) {
    var showChangeId by remember { mutableStateOf(false) }
    var steamId by remember { mutableStateOf(game.steamGridId.orEmpty()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        ShoulderTabs(GamePageTab.GameInfo, onTab)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth()) {
            CoverArt(game.title, game.coverHue, modifier = Modifier.width(150.dp).height(220.dp))
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(game.summary, color = TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(22.dp))
                MetaLine("Developer", game.developer)
                MetaLine("Publisher", game.publisher)
                MetaLine("Category", game.category)
                MetaLine("Release Date", game.releaseDate)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(game.players, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(game.controller, color = TextPrimary)
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
            InfoAction("View Page")
            Spacer(Modifier.width(10.dp))
            InfoAction("Change ID") { showChangeId = true }
            Spacer(Modifier.width(10.dp))
            InfoAction("Store Page")
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
                    steamId = it
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ShoulderKey("L1") {
            val values = GamePageTab.entries
            onTab(values[(selected.ordinal - 1 + values.size) % values.size])
        }
        Spacer(Modifier.weight(1f))
        CenteredTabs(selected, onTab)
        Spacer(Modifier.weight(1f))
        ShoulderKey("R1") {
            val values = GamePageTab.entries
            onTab(values[(selected.ordinal + 1) % values.size])
        }
    }
}

@Composable
private fun CenteredTabs(selected: GamePageTab, onTab: (GamePageTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SteamPill("Activity", selected == GamePageTab.Activity) { onTab(GamePageTab.Activity) }
        Spacer(Modifier.width(24.dp))
        SteamPill("Community", selected == GamePageTab.Community) { onTab(GamePageTab.Community) }
        Spacer(Modifier.width(24.dp))
        SteamPill("Game Info", selected == GamePageTab.GameInfo) { onTab(GamePageTab.GameInfo) }
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
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Tile)
            .clickable(onClick = onClick),
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
