package org.gamelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.gamelauncher.data.MockLibrary
import org.gamelauncher.ui.chrome.CommandBar
import org.gamelauncher.ui.chrome.CommandHints
import org.gamelauncher.ui.chrome.DimScrim
import org.gamelauncher.ui.chrome.SideMenu
import org.gamelauncher.ui.chrome.TopStatusBar
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
        val snapshot = remember { MockLibrary.snapshot }
        var stack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
        var menuOpen by remember { mutableStateOf(false) }
        var searchOpen by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val current = stack.last()

        fun go(screen: Screen, clear: Boolean = false) {
            stack = if (clear) listOf(screen) else stack + screen
            menuOpen = false
        }

        fun back() {
            when {
                menuOpen -> menuOpen = false
                searchOpen -> {
                    searchOpen = false
                    searchQuery = ""
                }
                stack.size > 1 -> stack = stack.dropLast(1)
            }
        }

        BackHandler(enabled = menuOpen || searchOpen || stack.size > 1) { back() }

        val hints = when {
            menuOpen -> CommandHints()
            current is Screen.Home -> CommandHints(extra = "Y" to "Favorite")
            current is Screen.Game -> CommandHints()
            else -> CommandHints()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            TopStatusBar(
                searchOpen = searchOpen,
                searchQuery = searchQuery,
                onSearchQuery = { searchQuery = it },
                onToggleSearch = { searchOpen = !searchOpen },
            )
            Box(Modifier.weight(1f)) {
                when (val screen = current) {
                    Screen.Home -> HomeScreen(snapshot, onOpenGame = { go(Screen.Game(it)) })
                    Screen.Library -> LibraryScreen(snapshot, onOpenGame = { go(Screen.Game(it)) })
                    Screen.Store -> StoreScreen()
                    Screen.Settings -> SettingsScreen()
                    is Screen.Game -> GameScreen(MockLibrary.game(screen.id))
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
            CommandBar(hints, onMenu = { menuOpen = !menuOpen }, onBack = { back() })
        }
    }
}
