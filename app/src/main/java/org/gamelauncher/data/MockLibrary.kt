package org.gamelauncher.data

object MockLibrary {
    val snapshot = LibrarySnapshot(
        games = listOf(
            Game(
                id = "skybound",
                title = "Skybound",
                packageName = "org.example.skybound",
                inLibrary = true,
                lastPlayed = "Never",
                playTime = "Play Now!",
                rating = 4.1f,
                steamGridId = "5258599",
                summary = "A physics sandbox about flinging birds at poorly designed fortresses.",
                developer = "Example Studios",
                publisher = "Example Studios",
                category = "Games, Puzzle",
                releaseDate = "2015",
                players = "Single-Player",
                controller = "Partial Controller Support",
                coverHue = 28f,
            ),
            Game(
                id = "neon-circuit",
                title = "Neon Circuit",
                packageName = "org.example.neoncircuit",
                inLibrary = true,
                lastPlayed = "Yesterday",
                playTime = "3.2 hours",
                rating = 4.6f,
                steamGridId = "112233",
                summary = "A launcher-native PC wrapper used here as a second library title.",
                developer = "Circuit Works",
                publisher = "Circuit Works",
                category = "Games, Action",
                releaseDate = "2024",
                players = "Single-Player",
                controller = "Full Controller Support",
                coverHue = 210f,
            ),
        ),
        installed = listOf(
            InstalledApp("skybound", "Skybound", "org.example.skybound", 28f, true),
            InstalledApp("argosy", "Argosy Launcher", "com.nendo.argosy", 220f, false),
            InstalledApp("audiofx", "AudioFX", "org.lineageos.audiofx", 320f, false),
            InstalledApp("bt", "BT Ferry", "org.example.bt", 210f, false),
            InstalledApp("docs", "Bass: Lineout Docs", "org.example.docs", 200f, false),
            InstalledApp("browser", "Browser", "com.android.browser", 215f, false),
            InstalledApp("calc", "Calculator", "com.android.calculator2", 0f, false),
            InstalledApp("cal", "Calendar", "com.android.calendar", 10f, false),
            InstalledApp("cam", "Camera", "com.android.camera2", 200f, false),
            InstalledApp("clock", "Clock", "com.android.deskclock", 40f, false),
        ),
        news = listOf(
            NewsItem(
                id = "n1",
                kind = "BUGFIX",
                kindColorHue = 42f,
                body = "Minor bug fixes. Bugs: the only thing that (almost) makes us angrier than the pigs.",
                date = "8 September 2026",
                version = "Skybound 26.6.0",
                gameId = "skybound",
                gameTitle = "Skybound",
            ),
            NewsItem(
                id = "n2",
                kind = "REGULAR UPDATE",
                kindColorHue = 195f,
                body = "Runtime and wrapper improvements for native titles.",
                date = "24 August 2026",
                version = "Neon Circuit 1.2.0",
                gameId = "neon-circuit",
                gameTitle = "Neon Circuit",
            ),
        ),
    )

    fun game(id: String): Game =
        snapshot.games.first { it.id == id }
}
