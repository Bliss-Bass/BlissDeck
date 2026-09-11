package org.gamelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.gamelauncher.data.ArtworkRepository
import org.gamelauncher.data.InstalledCatalog
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.data.PlayNewsRepository
import org.gamelauncher.data.findEntry
import org.gamelauncher.ui.chrome.ApplySystemBarMode
import org.gamelauncher.ui.chrome.CommandBar
import org.gamelauncher.ui.chrome.CommandHints
import org.gamelauncher.ui.chrome.DimScrim
import org.gamelauncher.ui.chrome.SearchOverlay
import org.gamelauncher.ui.chrome.SideMenu
import org.gamelauncher.ui.chrome.TopStatusBar
import org.gamelauncher.ui.chrome.UserMenu
import org.gamelauncher.ui.chrome.rememberBottomChromeInsets
import org.gamelauncher.ui.chrome.rememberFreeformWindow
import org.gamelauncher.ui.chrome.rememberTopChromeInsets
import org.gamelauncher.ui.game.GameScreen
import org.gamelauncher.ui.home.HomeScreen
import org.gamelauncher.ui.library.LibraryScreen
import org.gamelauncher.ui.navigation.MenuItem
import org.gamelauncher.ui.navigation.Screen
import org.gamelauncher.ui.settings.SettingsScreen
import org.gamelauncher.ui.store.StoreScreen
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.GameLauncherTheme

@Composable
fun LauncherApp(onClose: () -> Unit) {
    GameLauncherTheme {
        val context = LocalContext.current
        val snapshot = remember(context) { InstalledCatalog.load(context) }
        val artwork = remember(context) { ArtworkRepository(context) }
        val newsRepo = remember(context) { PlayNewsRepository(context) }
        val news by newsRepo.news.collectAsState()
        val newsLoading by newsRepo.loading.collectAsState()
        LaunchedEffect(snapshot) { newsRepo.refresh(snapshot) }
        var stack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
        var menuOpen by remember { mutableStateOf(false) }
        var userMenuOpen by remember { mutableStateOf(false) }
        var searchOpen by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val current = stack.last()

        fun go(screen: Screen, clear: Boolean = false) {
            stack = if (clear) listOf(screen) else stack + screen
            menuOpen = false
            userMenuOpen = false
        }

        fun back() {
            when {
                menuOpen -> menuOpen = false
                userMenuOpen -> userMenuOpen = false
                searchOpen -> {
                    searchOpen = false
                    searchQuery = ""
                }
                stack.size > 1 -> stack = stack.dropLast(1)
            }
        }

        BackHandler(enabled = menuOpen || userMenuOpen || searchOpen || stack.size > 1) { back() }

        val hints = when {
            menuOpen -> CommandHints()
            current is Screen.Home -> CommandHints(extra = "Y" to "Favorite")
            current is Screen.Game -> CommandHints()
            else -> CommandHints()
        }

        val freeform = rememberFreeformWindow()
        ApplySystemBarMode(freeform)

        CompositionLocalProvider(LocalArtwork provides artwork) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
        ) {
            TopStatusBar(
                searchOpen = searchOpen,
                searchQuery = searchQuery,
                onSearchQuery = { searchQuery = it },
                onToggleSearch = {
                    searchOpen = !searchOpen
                    if (!searchOpen) searchQuery = ""
                    userMenuOpen = false
                },
                onUserMenu = {
                    userMenuOpen = !userMenuOpen
                    searchOpen = false
                    searchQuery = ""
                    menuOpen = false
                },
                modifier = Modifier.windowInsetsPadding(rememberTopChromeInsets(freeform)),
            )
            Box(Modifier.weight(1f)) {
                when (val screen = current) {
                    Screen.Home -> HomeScreen(
                        snapshot,
                        news = news,
                        newsLoading = newsLoading,
                        onOpenGame = { go(Screen.Game(it)) },
                    )
                    Screen.Library -> LibraryScreen(snapshot, onOpenGame = { go(Screen.Game(it)) })
                    Screen.Store -> StoreScreen()
                    Screen.Settings -> SettingsScreen()
                    is Screen.Game -> {
                        val game = snapshot.findEntry(screen.id)
                        if (game != null) {
                            GameScreen(game)
                        }
                    }
                }
                if (searchOpen) {
                    SearchOverlay(snapshot, searchQuery) { id ->
                        searchOpen = false
                        searchQuery = ""
                        go(Screen.Game(id))
                    }
                }
                if (userMenuOpen) {
                    DimScrim { userMenuOpen = false }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 8.dp),
                    ) {
                        UserMenu(
                            onLauncherSettings = { go(Screen.Settings, clear = true) },
                            onCloseLauncher = onClose,
                            onDismiss = { userMenuOpen = false },
                        )
                    }
                }
                if (menuOpen) {
                    DimScrim { menuOpen = false }
                    Box(Modifier.align(Alignment.CenterStart)) {
                        SideMenu(current) { item ->
                            when (item) {
                                MenuItem.Home -> go(Screen.Home, clear = true)
                                MenuItem.Library -> go(Screen.Library, clear = true)
                                MenuItem.Store -> go(Screen.Store, clear = true)
                                MenuItem.Settings -> go(Screen.Settings, clear = true)
                                MenuItem.Close -> onClose()
                                MenuItem.Friends, MenuItem.Media, MenuItem.Downloads -> Unit
                            }
                        }
                    }
                }
            }
            CommandBar(
                hints,
                onMenu = {
                    menuOpen = !menuOpen
                    userMenuOpen = false
                },
                onBack = { back() },
                modifier = Modifier.windowInsetsPadding(rememberBottomChromeInsets(freeform)),
            )
        }
        }
    }
}
