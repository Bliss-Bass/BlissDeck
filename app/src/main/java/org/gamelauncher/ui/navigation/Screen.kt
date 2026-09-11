package org.gamelauncher.ui.navigation

sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Store : Screen
    data object Settings : Screen
    data class Game(val id: String) : Screen
}

enum class MenuItem(val label: String, val enabled: Boolean) {
    Home("Home", true),
    Library("Library", true),
    Store("Store", true),
    Friends("Friends & Chat", false),
    Media("Media", false),
    Downloads("Downloads", false),
    Settings("Settings", true),
    Close("Close", true),
}
