package org.gamelauncher.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.ArtworkCoverStyle
import org.gamelauncher.data.InstalledApp
import org.gamelauncher.data.LibrarySnapshot
import org.gamelauncher.data.LibraryTab
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.ui.components.AppIconTile
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.ShoulderKey
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.chromeContentPadding

@Composable
fun LibraryScreen(
    snapshot: LibrarySnapshot,
    onOpenGame: (String) -> Unit,
) {
    val settings = LocalSettings.current
    val prefs by settings.state.collectAsState()
    val coverLandscape = prefs.coverStyle == ArtworkCoverStyle.Wide
    val layouts = LocalTheme.current.layouts
    val visibleTabs = remember(layouts.libraryCollections, layouts.libraryMedia) {
        LibraryTab.entries.filter { tab ->
            when (tab) {
                LibraryTab.Media -> layouts.libraryMedia
                LibraryTab.Collections -> layouts.libraryCollections
                else -> true
            }
        }
    }
    val tab = prefs.libraryTab.takeIf { it in visibleTabs } ?: visibleTabs.first()
    LaunchedEffect(visibleTabs, prefs.libraryTab) {
        if (visibleTabs.isEmpty() || prefs.libraryTab == tab) return@LaunchedEffect
        settings.update { it.copy(libraryTab = tab) }
    }
    val firstFocus = remember(tab) { FocusRequester() }
    LaunchedEffect(tab) {
        kotlinx.coroutines.delay(80)
        runCatching { firstFocus.requestFocus() }
    }
    val media = remember(snapshot) { snapshot.installed.filter { it.isMedia } }

    fun select(next: LibraryTab) {
        settings.update { it.copy(libraryTab = next) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .chromeContentPadding(extraTop = 12.dp, extraBottom = 12.dp, horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShoulderKey("L1") {
                val i = visibleTabs.indexOf(tab).let { if (it < 0) 0 else it }
                select(visibleTabs[(i - 1 + visibleTabs.size) % visibleTabs.size])
            }
            Spacer(Modifier.weight(1f))
            SteamPill("Games", tab == LibraryTab.AllGames, count = snapshot.libraryGames.size) {
                select(LibraryTab.AllGames)
            }
            if (layouts.libraryMedia) {
                Spacer(Modifier.width(28.dp))
                SteamPill("Media", tab == LibraryTab.Media, count = media.size) {
                    select(LibraryTab.Media)
                }
            }
            Spacer(Modifier.width(28.dp))
            SteamPill("Installed", tab == LibraryTab.Installed, count = snapshot.installed.size) {
                select(LibraryTab.Installed)
            }
            if (layouts.libraryCollections) {
                Spacer(Modifier.width(28.dp))
                SteamPill("Collections", tab == LibraryTab.Collections) { select(LibraryTab.Collections) }
            }
            Spacer(Modifier.weight(1f))
            ShoulderKey("R1") {
                val i = visibleTabs.indexOf(tab).let { if (it < 0) 0 else it }
                select(visibleTabs[(i + 1) % visibleTabs.size])
            }
        }
        Spacer(Modifier.height(20.dp))
        when (tab) {
            LibraryTab.AllGames -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.focusGroup(),
            ) {
                itemsIndexed(snapshot.libraryGames, key = { _, game -> game.id }) { index, game ->
                    CoverArt(
                        title = game.title,
                        hue = game.coverHue,
                        showTitle = true,
                        packageName = game.packageName,
                        isGame = true,
                        landscape = coverLandscape,
                        modifier = Modifier
                            .height(230.dp)
                            .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                            .tileClick { onOpenGame(game.id) },
                    )
                }
            }
            LibraryTab.Installed -> LibraryAppGrid(
                apps = snapshot.installed,
                coverLandscape = coverLandscape,
                firstFocus = firstFocus,
                onOpenGame = onOpenGame,
            )
            LibraryTab.Media -> {
                if (media.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No media apps yet. Mark a title as media from the sprocket.",
                            color = TextMuted,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    LibraryAppGrid(
                        apps = media,
                        coverLandscape = coverLandscape,
                        firstFocus = firstFocus,
                        onOpenGame = onOpenGame,
                    )
                }
            }
            LibraryTab.Collections -> CollectionsPane(snapshot, onOpenGame)
        }
    }
}

@Composable
private fun LibraryAppGrid(
    apps: List<InstalledApp>,
    coverLandscape: Boolean,
    firstFocus: FocusRequester,
    onOpenGame: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        modifier = Modifier.focusGroup(),
    ) {
        itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
            if (app.isGame) {
                CoverArt(
                    title = app.title,
                    hue = app.coverHue,
                    showTitle = true,
                    packageName = app.packageName,
                    isGame = true,
                    landscape = coverLandscape,
                    modifier = Modifier
                        .height(210.dp)
                        .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                        .tileClick { onOpenGame(app.id) },
                )
            } else {
                AppIconTile(
                    title = app.title,
                    hue = app.coverHue,
                    packageName = app.packageName,
                    modifier = Modifier
                        .height(210.dp)
                        .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier),
                    onClick = { onOpenGame(app.id) },
                )
            }
        }
    }
}
