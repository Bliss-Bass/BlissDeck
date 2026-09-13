package org.gamelauncher.data

enum class LibraryTab { AllGames, Installed, Collections }

enum class HomeFeedTab { WhatsNew, Favorites, Recommended }

enum class HomeShelfTab { LastPlayed, Media }

enum class GamePageTab { Activity, Community, GameInfo }

data class Game(
    val id: String,
    val title: String,
    val packageName: String,
    val inLibrary: Boolean,
    val lastPlayed: String,
    val playTime: String,
    val rating: Float,
    val steamGridId: String?,
    val summary: String,
    val developer: String,
    val publisher: String,
    val category: String,
    val releaseDate: String,
    val players: String,
    val controller: String,
    val coverHue: Float,
    val detectedGame: Boolean = inLibrary,
    val hasLeanback: Boolean = false,
    val detectedMedia: Boolean = false,
    val isMedia: Boolean = detectedMedia,
)

data class InstalledApp(
    val id: String,
    val title: String,
    val packageName: String,
    val coverHue: Float,
    val isGame: Boolean,
    val lastUpdateTime: Long = 0L,
    val versionName: String? = null,
    val detectedGame: Boolean = isGame,
    val hasLeanback: Boolean = false,
    val detectedMedia: Boolean = false,
    val isMedia: Boolean = detectedMedia,
)

data class NewsItem(
    val id: String,
    val kind: String,
    val body: String,
    val date: String,
    val version: String,
    val gameId: String,
    val gameTitle: String,
    val imageUrl: String? = null,
    val sortMillis: Long = 0L,
    val detail: String = "",
) {
    val fullText: String get() = detail.ifBlank { body }
}

data class Screenshot(
    val id: String,
    val caption: String,
    val hue: Float,
)

data class LibrarySnapshot(
    val games: List<Game>,
    val installed: List<InstalledApp>,
    val news: List<NewsItem>,
) {
    val libraryGames: List<Game> get() = games.filter { it.inLibrary }
}
