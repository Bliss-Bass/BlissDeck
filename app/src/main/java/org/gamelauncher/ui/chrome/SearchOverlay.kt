package org.gamelauncher.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.InstalledApp
import org.gamelauncher.data.LibrarySnapshot
import org.gamelauncher.ui.components.ArtworkLayer
import org.gamelauncher.ui.components.hueBrush
import org.gamelauncher.ui.components.rememberArtwork
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.chromeContentPadding

@Composable
fun SearchOverlay(
    snapshot: LibrarySnapshot,
    query: String,
    onOpen: (String) -> Unit,
) {
    val matches = remember(snapshot, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            emptyList()
        } else {
            snapshot.installed
                .filter {
                    it.title.contains(needle, ignoreCase = true) ||
                        it.packageName.contains(needle, ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<InstalledApp> { it.isGame }.thenBy { it.title.lowercase() },
                )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            )
            .chromeContentPadding(extraTop = 16.dp, extraBottom = 16.dp, horizontal = 28.dp),
    ) {
        when {
            query.isBlank() -> Text(
                "Search your installed games and apps",
                color = TextMuted,
                fontSize = 16.sp,
            )
            matches.isEmpty() -> Text(
                "No matching games or apps",
                color = TextMuted,
                fontSize = 16.sp,
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(matches, key = { it.id }) { app ->
                    SearchHit(app) { onOpen(app.id) }
                }
            }
        }
    }
}

@Composable
private fun SearchHit(app: InstalledApp, onClick: () -> Unit) {
    val artwork = rememberArtwork(app.packageName, app.title, app.isGame)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tileFrame(false, RoundedCornerShape(6.dp))
            .background(Tile)
            .tileClick(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(hueBrush(app.coverHue)),
        ) {
            ArtworkLayer(artwork, landscape = false)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.title, color = TextPrimary, fontSize = 17.sp)
            Text(
                if (app.isGame) "Game" else "App",
                color = TextMuted,
                fontSize = 13.sp,
            )
        }
    }
}
