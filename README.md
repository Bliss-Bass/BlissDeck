# BlissDeck

A landscape Steam-library **HOME** for Android-x86, Bliss OS, PrimeOS, and Waydroid. It lists installed Android games and apps, launches them, and can pull covers from [SteamGridDB](https://www.steamgriddb.com/).

The user-facing name is **BlissDeck**. The Android application ID is still `org.gamelauncher` (`0.1.0-dev`). Layouts follow [Vapor Launcher](https://github.com/imperador/vapor-launcher); this is a greenfield Kotlin / Jetpack Compose app, not a Qt port and not a ROM frontend.

## What’s in

- **Home** — Last Played recents (coverflow or row), selected-title backdrop, and L1/R1 feed tabs for What’s New, Favorites, and Recommended
- **Library** — All Games (`CATEGORY_GAME`), Installed apps, and user collections
- **Store** — opens Play, Aurora, Droid-ify, Neo Store, or another installed store from Settings
- **Game page** — hero, Play/Stop, then Activity / Community / Game Info without tearing down the chrome
- **Settings** — themes, display (text size, rounded cards), chrome bars, borders and corner radius, recents, input, store, permissions, SteamGridDB
- Live Wi‑Fi, battery, optional notification count, and an account photo in the top bar
- Package icons immediately; SteamGridDB grids, heroes, and icons when you add an API key (per-title picker on Game Info)
- Theme packs as stacked `.ini` / `.cfg` overlays (Default + Midnight built in; import/export)
- Registers as `LAUNCHER` and `HOME`, `x86_64` + ARM ABIs, minSdk 26

Desktop windowing keeps chrome below the freeform caption; fullscreen hides the status bar and nav/dock.

Gamepad focus is still catching up (A/B/Y in the footer are mostly hints; system Back and the on-screen B control pop the stack). Friends is not built yet.

## Build

You need JDK 17+, an Android SDK (`platforms;android-35` is enough), and `ANDROID_HOME` (or `sdk.dir` in `local.properties`). Do not commit `local.properties`.

```bash
./gradlew :app:installDebug
```

Or point it at a device:

```bash
ANDROID_HOME=$HOME/Android/Sdk ANDROID_SERIAL=<serial-or-ip:5555> ./gradlew :app:installDebug
```

On a desktop-mode tablet it opens as a freeform window. Maximize or fullscreen it like any other desktop app.

### Set as Home

MENU → **Set as Home app**, or Settings → Permissions. Optional extras:

- **Usage access** — last-played and running titles
- **Accessibility** — close / switch freeform game windows
- **Notification listener** — badge count next to Wi‑Fi and battery

## Artwork

Covers fall back to the app icon. For SteamGrid art:

1. Create a key at [steamgriddb.com/profile/preferences/api](https://www.steamgriddb.com/profile/preferences/api)
2. MENU → Settings → SteamGridDB → paste the key → Save key

The key is stored in app SharedPreferences on the device. It is not part of this repository. Games search by title and cache the grid + hero. Non-games stay on the icon unless you set an ID under Game Info → Change ID.

## Layout

```
app/src/main/java/org/gamelauncher/
  data/          catalog, sessions, SteamGrid, Play news, themes, prefs
  ui/chrome/     top bar, MENU, search, footer, window insets
  ui/home/       recents, What’s New, ambient backdrop
  ui/library/    All Games / Installed / Collections
  ui/game/       Activity / Community / Game Info
  ui/store/      preferred store launcher
  ui/settings/   prefs and theme packs
  ui/theme/      colors and blur
```

Project conventions for Cursor live in `.cursor/rules/`.

## AI-assisted development

BlissDeck is built in [Cursor](https://cursor.com) with AI coding assistants drafting, editing, and iterating on much of the Kotlin and Compose. That is a working method, not a claim that the app wrote itself.

Humans set the product direction (HOME for Bliss OS / Android-x86, Vapor-like chrome, settings-first prefs), review the diffs, and test on device. Assistants are asked to follow the rules in `.cursor/rules/` — persist user choices in Settings, commit a finished unit before starting the next task — and they still get things wrong. Treat every AI-authored change as something a person signed off on.

If you contribute, use those tools or ignore them. The source in git is what ships.

## License

No license file is in the tree yet. Please treat the code as all rights reserved until one is added.
