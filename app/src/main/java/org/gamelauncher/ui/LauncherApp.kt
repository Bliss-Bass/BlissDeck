package org.gamelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.gamelauncher.MainActivity
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
        val newsRepo = remember(context) { PlayNewsRepository(context) }
        val artwork = remember(context) { ArtworkRepository(context, newsRepo) }
        val news by newsRepo.news.collectAsState()
        val newsLoading by newsRepo.loading.collectAsState()
        LaunchedEffect(snapshot) { newsRepo.refresh(snapshot) }
        LaunchedEffect(news) { artwork.onPlayArtUpdated() }
        var stack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
        var menuOpen by remember { mutableStateOf(false) }
        var userMenuOpen by remember { mutableStateOf(false) }
        var searchOpen by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val current = stack.last()

        fun go(screen: Screen, root: Boolean = false) {
            menuOpen = false
            userMenuOpen = false
            stack = when {
                root -> listOf(screen)
                stack.lastOrNull() == screen -> stack
                else -> stack + screen
            }
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
                stack.lastOrNull() !is Screen.Home -> stack = listOf(Screen.Home)
            }
        }

        BackHandler { back() }

        val activity = LocalContext.current as? MainActivity
        val onHomePressed = rememberUpdatedState {
            if (current is Screen.Home) {
                searchOpen = false
                searchQuery = ""
                userMenuOpen = false
                menuOpen = !menuOpen
            } else {
                go(Screen.Home, root = true)
            }
        }
        val interceptKey = rememberUpdatedState { event: android.view.KeyEvent ->
            if (!isAndroidMenuKey(event.keyCode) || current !is Screen.Home) return@rememberUpdatedState false
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                searchOpen = false
                searchQuery = ""
                userMenuOpen = false
                menuOpen = !menuOpen
            }
            true
        }
        DisposableEffect(activity) {
            activity?.onHomePressed = { onHomePressed.value() }
            activity?.interceptKey = { interceptKey.value(it) }
            onDispose {
                activity?.onHomePressed = null
                activity?.interceptKey = null
            }
        }

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
                .background(Background)
                .onPreviewKeyEvent { event ->
                    when {
                        isLauncherMenuKey(event) && current is Screen.Home -> {
                            if (event.type == KeyEventType.KeyDown) {
                                searchOpen = false
                                searchQuery = ""
                                userMenuOpen = false
                                menuOpen = !menuOpen
                            }
                            true
                        }
                        isLauncherBackKey(event, typing = searchOpen) -> {
                            if (event.type == KeyEventType.KeyUp) back()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Box(Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 52.dp),
                ) {
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
                                onLauncherSettings = { go(Screen.Settings) },
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
                                    MenuItem.Home -> go(Screen.Home, root = true)
                                    MenuItem.Library -> go(Screen.Library)
                                    MenuItem.Store -> go(Screen.Store)
                                    MenuItem.Settings -> go(Screen.Settings)
                                    MenuItem.Close -> onClose()
                                    MenuItem.Friends, MenuItem.Media, MenuItem.Downloads -> Unit
                                }
                            }
                        }
                    }
                }
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
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(rememberTopChromeInsets(freeform)),
                )
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

private fun isAndroidMenuKey(keyCode: Int): Boolean = when (keyCode) {
    android.view.KeyEvent.KEYCODE_META_LEFT,
    android.view.KeyEvent.KEYCODE_META_RIGHT,
    android.view.KeyEvent.KEYCODE_WINDOW,
    android.view.KeyEvent.KEYCODE_MENU,
    -> true
    else -> false
}

private fun isLauncherMenuKey(event: KeyEvent): Boolean =
    event.key == Key.MetaLeft ||
        event.key == Key.MetaRight ||
        event.key == Key.Window ||
        event.key == Key.Menu

private fun isLauncherBackKey(event: KeyEvent, typing: Boolean): Boolean {
    if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
    return when (event.key) {
        Key.Escape, Key.ButtonB -> true
        Key.B -> !typing
        else -> false
    }
}
