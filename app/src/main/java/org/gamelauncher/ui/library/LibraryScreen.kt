package org.gamelauncher.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.LibrarySnapshot
import org.gamelauncher.data.LibraryTab
import org.gamelauncher.ui.components.AppIconTile
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted

@Composable
fun LibraryScreen(
    snapshot: LibrarySnapshot,
    onOpenGame: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(LibraryTab.AllGames) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShoulderKey("L1") {
                val values = LibraryTab.entries
                tab = values[(tab.ordinal - 1 + values.size) % values.size]
            }
            Spacer(Modifier.weight(1f))
            SteamPill("All Games", tab == LibraryTab.AllGames, count = snapshot.libraryGames.size) {
                tab = LibraryTab.AllGames
            }
            Spacer(Modifier.width(28.dp))
            SteamPill("Installed", tab == LibraryTab.Installed, count = snapshot.installed.size) {
                tab = LibraryTab.Installed
            }
            Spacer(Modifier.width(28.dp))
            SteamPill("Friends", tab == LibraryTab.Friends) { tab = LibraryTab.Friends }
            Spacer(Modifier.width(28.dp))
            SteamPill("Collections", tab == LibraryTab.Collections) { tab = LibraryTab.Collections }
            Spacer(Modifier.weight(1f))
            ShoulderKey("R1") {
                val values = LibraryTab.entries
                tab = values[(tab.ordinal + 1) % values.size]
            }
        }
        Spacer(Modifier.height(20.dp))
        when (tab) {
            LibraryTab.AllGames -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(snapshot.libraryGames, key = { it.id }) { game ->
                    CoverArt(
                        title = game.title,
                        hue = game.coverHue,
                        modifier = Modifier
                            .height(230.dp)
                            .clickable { onOpenGame(game.id) },
                    )
                }
            }
            LibraryTab.Installed -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(snapshot.installed, key = { it.id }) { app ->
                    if (app.isGame) {
                        CoverArt(
                            title = app.title,
                            hue = app.coverHue,
                            modifier = Modifier
                                .height(210.dp)
                                .clickable { onOpenGame(app.id) },
                        )
                    } else {
                        AppIconTile(
                            title = app.title,
                            hue = app.coverHue,
                            modifier = Modifier.height(210.dp),
                        )
                    }
                }
            }
            LibraryTab.Friends, LibraryTab.Collections -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    "Not available yet :(",
                    color = TextMuted,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
