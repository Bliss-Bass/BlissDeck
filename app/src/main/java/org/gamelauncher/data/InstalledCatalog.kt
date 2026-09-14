package org.gamelauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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

/**
 * BlissDeck is often the default Home with singleTask, so the process stays up across
 * installs. Reload the launcher catalog on resume and on package add/remove/change.
 */
@Composable
fun rememberInstalledSnapshot(): LibrarySnapshot {
    val context = LocalContext.current
    val resumeTick = rememberResumeTick()
    var packageTick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_PACKAGE_REMOVED &&
                    intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                ) {
                    return
                }
                packageTick++
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return remember(resumeTick, packageTick, context) { InstalledCatalog.load(context) }
}

data class ExportedActivity(
    val className: String,
    val label: String,
    val launcher: Boolean = false,
    val leanback: Boolean = false,
) {
    val shortName: String get() = className.substringAfterLast('.')

    val menuLabel: String
        get() {
            val named = label.isNotBlank() &&
                !label.equals(shortName, ignoreCase = true) &&
                '.' !in label
            val base = if (named) label else shortName
            return if (leanback && !launcher) "$base (TV)" else base
        }
}

fun LibrarySnapshot.findEntry(id: String): Game? =
    games.firstOrNull { it.id == id } ?: installed.firstOrNull { it.id == id }?.toGame()

fun Game.toInstalledApp(): InstalledApp = InstalledApp(
    id = id,
    title = title,
    packageName = packageName,
    coverHue = coverHue,
    isGame = inLibrary,
    detectedGame = detectedGame,
    hasLeanback = hasLeanback,
    detectedMedia = detectedMedia,
    isMedia = isMedia,
)

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

fun exportedActivities(context: Context, packageName: String): List<ExportedActivity> {
    val pm = context.packageManager
    val merged = LinkedHashMap<String, ExportedActivity>()
    fun add(className: String, label: String, launcher: Boolean, leanback: Boolean) {
        if (className.isBlank()) return
        val previous = merged[className]
        merged[className] = ExportedActivity(
            className = className,
            label = label.ifBlank { previous?.label.orEmpty().ifBlank { className.substringAfterLast('.') } },
            launcher = previous?.launcher == true || launcher,
            leanback = previous?.leanback == true || leanback,
        )
    }
    declaredExported(pm, packageName).forEach { add(it.first, it.second, launcher = false, leanback = false) }
    resolvePackageActivities(pm, packageName, Intent.CATEGORY_LAUNCHER).forEach {
        add(it.first, it.second, launcher = true, leanback = false)
    }
    resolvePackageActivities(pm, packageName, Intent.CATEGORY_LEANBACK_LAUNCHER).forEach {
        add(it.first, it.second, launcher = false, leanback = true)
    }
    return merged.values.sortedWith(
        compareByDescending<ExportedActivity> { it.launcher }
            .thenByDescending { it.leanback }
            .thenBy { it.menuLabel.lowercase() },
    )
}

private fun declaredExported(pm: PackageManager, packageName: String): List<Pair<String, String>> {
    val flags = PackageManager.GET_ACTIVITIES
    val info = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }
    }.getOrNull() ?: return emptyList()
    return info.activities.orEmpty()
        .filter { it.enabled && it.exported }
        .map { it.name to it.loadLabel(pm).toString() }
}

private fun resolvePackageActivities(
    pm: PackageManager,
    packageName: String,
    category: String,
): List<Pair<String, String>> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(packageName)
    val resolved = if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
    return resolved.map { it.activityInfo.name to it.loadLabel(pm).toString() }
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
