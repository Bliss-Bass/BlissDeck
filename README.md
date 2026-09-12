# BlissDeck

A landscape Steam-library **HOME** for Android-x86, Bliss OS, and Waydroid. It lists installed Android games and apps, launches them, and optionally pulls covers from [SteamGridDB](https://www.steamgriddb.com/).

This is a greenfield Kotlin / Jetpack Compose app (`org.gamelauncher`). The layouts follow [Vapor Launcher](https://github.com/imperador/vapor-launcher); they are not a Qt port and not a ROM frontend.

## What’s in

- HOME with recents, a selected-title backdrop (oversized, blurred, slow Ken Burns motion), and L1/R1 feed tabs
- Library: All Games (`CATEGORY_GAME`) and Installed (every launcher app except itself)
- Game page: Activity, Community, Game Info, Play
- Desktop windowing: chrome sits below the freeform caption; fullscreen hides the status bar and nav/dock
- Package icons immediately; SteamGridDB grids/heroes when you add an API key
- Per-title SteamGrid ID override on Game Info
- Registers as `LAUNCHER` and `HOME`, `x86_64` + ARM ABIs, minSdk 26

Friends, Collections, Store, and most Settings rows are still placeholders. Gamepad focus is not wired yet (A/B/Y in the footer are mostly hints; system Back and the on-screen B control do pop the stack).

## Build

You need JDK 17+, an Android SDK (`platforms;android-35` is enough), and `ANDROID_HOME` (or `sdk.dir` in `local.properties`).

```bash
./gradlew :app:installDebug
```

Or point it at a device:

```bash
ANDROID_HOME=$HOME/Android/Sdk ANDROID_SERIAL=192.168.1.100:5555 ./gradlew :app:installDebug
```

On a desktop-mode tablet it will open as a freeform window. Maximize or fullscreen it like any other desktop app. The display name is **BlissDeck**.

## Artwork

Covers fall back to the app icon. For SteamGrid art:

1. Create a key at [steamgriddb.com/profile/preferences/api](https://www.steamgriddb.com/profile/preferences/api)
2. MENU → Settings → paste the key → Save key

Games search by title and cache the grid + hero. Non-games stay on the icon unless you set an ID under Game Info → Change ID.

## Layout

```
app/src/main/java/org/gamelauncher/
  data/          installed catalog, SteamGrid client, artwork cache
  ui/chrome/     top bar, MENU drawer, footer, window insets
  ui/home/       recents + ambient backdrop
  ui/library/    All Games / Installed
  ui/game/       Activity / Community / Game Info
  ui/settings/   SteamGridDB key
```
