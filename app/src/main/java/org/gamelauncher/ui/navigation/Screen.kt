package org.gamelauncher.ui.navigation

sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Store : Screen
    data object Settings : Screen
    data class Game(val id: String, val newsId: String? = null) : Screen
}

enum class MenuItem(val label: String, val enabled: Boolean) {
    Home("Home", true),
    Library("Library", true),
    Store("Store", true),
    Settings("Settings", true),
    Close("Close", true),
}
