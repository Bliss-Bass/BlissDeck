package org.gamelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import kotlin.math.absoluteValue

object InstalledCatalog {
    fun load(context: Context): LibrarySnapshot {
        val pm = context.packageManager
        val self = context.packageName
        val resolved = launcherActivities(pm)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != self }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        if (resolved.isEmpty()) return MockLibrary.snapshot

        val leanback = leanbackPackages(pm)
        val installed = resolved.map { it.toInstalledApp(pm, leanback) }
        val games = installed.filter { it.isGame }.map { it.toGame() }
        return LibrarySnapshot(
            games = games,
            installed = installed,
            news = emptyList(),
        )
    }

    fun launcherPackages(context: Context): List<String> {
        val self = context.packageName
        return launcherActivities(context.packageManager)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != self }
    }
}

fun LibrarySnapshot.findEntry(id: String): Game? =
    games.firstOrNull { it.id == id } ?: installed.firstOrNull { it.id == id }?.toGame()

fun InstalledApp.toGame(): Game = Game(
    id = id,
    title = title,
    packageName = packageName,
    inLibrary = isGame,
    lastPlayed = "Never",
    playTime = "Play Now!",
    rating = 0f,
    steamGridId = null,
    summary = "Installed Android app.",
    developer = packageName,
    publisher = packageName,
    category = when {
        isGame -> "Games"
        isMedia -> "Media"
        else -> "Application"
    },
    releaseDate = "—",
    players = "Single-Player",
    controller = "Unknown",
    coverHue = coverHue,
    detectedGame = detectedGame,
    hasLeanback = hasLeanback,
    detectedMedia = detectedMedia,
    isMedia = isMedia,
)

private fun launcherActivities(pm: PackageManager): List<ResolveInfo> {
    return queryMain(pm, Intent.CATEGORY_LAUNCHER)
}

private fun leanbackPackages(pm: PackageManager): Set<String> {
    return queryMain(pm, Intent.CATEGORY_LEANBACK_LAUNCHER)
        .map { it.activityInfo.packageName }
        .toSet()
}

internal fun leanbackActivity(pm: PackageManager, packageName: String): ResolveInfo? {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        .setPackage(packageName)
    return if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
        ).firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).firstOrNull()
    }
}

private fun queryMain(pm: PackageManager, category: String): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
    return if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
}

private fun ResolveInfo.toInstalledApp(pm: PackageManager, leanback: Set<String>): InstalledApp {
    val packageName = activityInfo.packageName
    val title = loadLabel(pm).toString()
    val appInfo = activityInfo.applicationInfo
    val pkgInfo = packageInfo(pm, packageName)
    val detected = appInfo.isGameApp()
    val media = MediaApps.detect(appInfo)
    return InstalledApp(
        id = packageName,
        title = title,
        packageName = packageName,
        coverHue = packageName.packageHue(),
        isGame = detected,
        lastUpdateTime = pkgInfo?.lastUpdateTime ?: 0L,
        versionName = pkgInfo?.versionName,
        detectedGame = detected,
        hasLeanback = packageName in leanback,
        detectedMedia = media,
        isMedia = media,
    )
}

private fun packageInfo(pm: PackageManager, packageName: String): android.content.pm.PackageInfo? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
    }.getOrNull()
}

private fun ApplicationInfo.isGameApp(): Boolean {
    if (packageName == "app.gamenative") return true
    if (packageName == "app.gamenative.stubinstaller") return false
    if (SteamNative.isGameStub(packageName)) return true
    if (Build.VERSION.SDK_INT >= 26 && category == ApplicationInfo.CATEGORY_GAME) return true
    @Suppress("DEPRECATION")
    if (flags and ApplicationInfo.FLAG_IS_GAME != 0) return true
    return false
}

fun String.packageHue(): Float = (hashCode().absoluteValue % 360).toFloat()
