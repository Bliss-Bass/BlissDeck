BlissDeck is a landscape Steam-library **HOME** for Android-x86, Bliss OS, PrimeOS, and Waydroid. It lists installed Android games and apps, launches them, and can pull covers from [SteamGridDB](https://www.steamgriddb.com/).

**Early development** (`0.1.0-dev`). Tested on device. The application ID is still `org.gamelauncher`.

### Highlights

- Last Played recents, What's New, library, collections, and a preferred-store shortcut
- Theme packs, chrome, corner radius, and display prefs in Settings
- SteamGridDB covers, heroes, and icons when you add an API key on-device
- Registers as `LAUNCHER` / `HOME` for x86_64 and ARM (minSdk 26)

## Install

1. Download **`app-universal-release.apk`** below. That file is the **universal** build (arm64 + x86_64 + 32-bit). There are also per-ABI APKs if you want a named download.
2. Install on a landscape Android device or Bliss OS / Waydroid session.
3. MENU -> **Set as Home app**, or launch from the app drawer.

```bash
adb install -r app-universal-release.apk
```

### Automatic updates (Obtainium)

Install [Obtainium](https://github.com/ImranR98/Obtainium), add GitHub repo **`Bliss-Bass/BlissDeck`**, filter releases to **`app-universal-release.apk`**, and it will track new **`v*`** tags from this page.

Optional extras after install: usage access (last played), accessibility (close/switch freeform games), and the notification listener (badge count).
