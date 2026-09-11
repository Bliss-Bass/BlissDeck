package org.gamelauncher.data

object SteamNative {
    private val STUB = Regex("""^app\.gamenative\.stub\.steam_(\d+)$""")

    fun steamAppId(packageName: String): String? =
        STUB.matchEntire(packageName)?.groupValues?.get(1)

    fun isGameStub(packageName: String): Boolean = steamAppId(packageName) != null

    fun cdnCover(steamAppId: String): String =
        "https://cdn.cloudflare.steamstatic.com/steam/apps/$steamAppId/library_600x900.jpg"

    fun cdnHero(steamAppId: String): String =
        "https://cdn.cloudflare.steamstatic.com/steam/apps/$steamAppId/library_hero.jpg"
}
