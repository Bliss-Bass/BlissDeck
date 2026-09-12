package org.gamelauncher.data

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log

data class RunningApp(
    val packageName: String,
    val title: String,
    val taskId: Int?,
)

object GameSession {
    const val XTMAPPER_PACKAGE = "xtr.keymapper"

    private const val TAG = "GameSession"
    private const val WINDOWING_MODE_FREEFORM = 5
    private const val REQUEST_BASE = 0x4100
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var launchableCache: List<String>? = null
    @Volatile private var launchableAt = 0L
    @Volatile private var resumedCache: Set<String>? = null
    @Volatile private var resumedAt = 0L

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        val activity = context.findActivity()
        val started = if (activity != null) {
            launchFromActivity(activity, packageName, intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
        if (started) AppPresence.markStarting(packageName)
        return started
    }

    fun close(context: Context, packageName: String) {
        if (packageName.isBlank() || packageName == context.packageName) return
        AppPresence.markClosing(packageName)
        val closed = CloseGameService.instance?.closePackage(packageName) == true
        Log.i(TAG, "close $packageName accessibility=$closed enabled=${CloseGameService.isEnabled(context)}")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        runCatching {
            ActivityManager::class.java
                .getMethod("forceStopPackage", String::class.java)
                .invoke(am, packageName)
        }
        runCatching { am.killBackgroundProcesses(packageName) }
        mainHandler.postDelayed({
            if (CloseGameService.instance?.closePackage(packageName) != true) {
                runCatching { am.killBackgroundProcesses(packageName) }
            }
        }, 250)
    }

    /**
     * `true` if [packageName] has a live process, `false` if it does not,
     * `null` when this process cannot see other apps (typical on stock Android).
     */
    fun running(context: Context, packageName: String): Boolean? {
        val presence = AppPresence.snapshot.value
        if (presence.connected) {
            if (AppPresence.state(packageName, presence) != AppRunState.Stopped) return true
        }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (hasUsageAccess(context)) {
            val importance = packageImportance(am, packageName)
            if (importance != null) {
                return importance < ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE
            }
        }
        if (tasks(am).any { it.matches(packageName) }) return true
        val procs = am.runningAppProcesses ?: return if (presence.connected) false else null
        val self = context.packageName
        val match = procs.any { packageName in it.pkgList }
        if (match) return true
        val seesOthers = procs.any { self !in it.pkgList }
        if (!seesOthers) return if (presence.connected) false else null
        return false
    }

    fun hasXtMapper(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(XTMAPPER_PACKAGE) != null ||
            xtMapperResolve(context) != null

    fun openXtMapper(context: Context) {
        if (launch(context, XTMAPPER_PACKAGE)) return
        val resolved = xtMapperResolve(context) ?: return
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(resolved.activityInfo.packageName, resolved.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    @Suppress("DEPRECATION")
    private fun launchFromActivity(activity: Activity, packageName: String, launch: Intent): Boolean {
        val intent = Intent(launch)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val opts = ActivityOptions.makeBasic()
        runCatching {
            ActivityOptions::class.java
                .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(opts, WINDOWING_MODE_FREEFORM)
        }
        Log.i(TAG, "launch $packageName component=${intent.component}")
        return runCatching {
            activity.startActivityForResult(intent, requestCode(packageName), opts.toBundle())
            true
        }.recoverCatching {
            activity.startActivity(intent, opts.toBundle())
            true
        }.getOrElse { error ->
            Log.w(TAG, "launch failed $packageName", error)
            false
        }
    }

    fun runningApps(context: Context): List<RunningApp> {
        val self = context.packageName
        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val presence = AppPresence.snapshot.value
        val seen = LinkedHashMap<String, RunningApp>()
        fun add(packageName: String?, taskId: Int?) {
            val pkg = packageName?.takeIf { it.isNotBlank() } ?: return
            if (hideFromSwitcher(pkg, self)) return
            val existing = seen[pkg]
            if (existing != null) {
                if (existing.taskId == null && taskId != null) {
                    seen[pkg] = existing.copy(taskId = taskId)
                }
                return
            }
            val title = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            seen[pkg] = RunningApp(pkg, title, taskId)
        }
        presence.open.forEach { add(it.packageName, it.taskId) }
        presence.starting.forEach { add(it, null) }
        tasks(am).forEach { task ->
            add(task.topActivity?.packageName ?: task.baseActivity?.packageName, task.taskId)
        }
        am.runningAppProcesses?.forEach { proc ->
            proc.pkgList?.forEach { add(it, null) }
        }
        if (hasUsageAccess(context)) {
            val resumed = recentlyResumedPackages(context)
            launcherPackages(context).forEach { pkg ->
                if (pkg !in resumed) return@forEach
                val importance = packageImportance(am, pkg) ?: return@forEach
                if (importance < ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE) {
                    add(pkg, null)
                }
            }
        }
        return seen.values.toList()
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun switchTo(context: Context, app: RunningApp): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (app.taskId != null) {
            val moved = runCatching {
                am.moveTaskToFront(app.taskId, 0)
                true
            }.getOrDefault(false)
            if (moved) return true
        }
        return launch(context, app.packageName)
    }

    internal fun hideFromSwitcher(packageName: String, self: String): Boolean {
        if (packageName == self) return true
        if (packageName == "android" || packageName == "com.android.systemui") return true
        if (packageName.startsWith("com.android.systemui")) return true
        if (packageName.startsWith("com.android.inputmethod")) return true
        if (packageName.contains("smartdock")) return true
        if (packageName == "com.android.launcher3") return true
        if (packageName.startsWith("com.android.wm.shell")) return true
        if (packageName == "app.gamenative.stubinstaller") return true
        return false
    }

    private fun launcherPackages(context: Context): List<String> {
        val now = SystemClock.uptimeMillis()
        val cached = launchableCache
        if (cached != null && now - launchableAt < 15_000L) return cached
        val packages = InstalledCatalog.launcherPackages(context)
        launchableCache = packages
        launchableAt = now
        return packages
    }

    private fun recentlyResumedPackages(context: Context): Set<String> {
        val now = SystemClock.uptimeMillis()
        val cached = resumedCache
        if (cached != null && now - resumedAt < 5_000L) return cached
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptySet()
        val end = System.currentTimeMillis()
        val begin = end - 12 * 60 * 60 * 1000L
        val events = runCatching { usm.queryEvents(begin, end) }.getOrNull() ?: return emptySet()
        val opened = LinkedHashSet<String>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            @Suppress("DEPRECATION")
            if (type == UsageEvents.Event.ACTIVITY_RESUMED ||
                type == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                opened.add(event.packageName)
            }
        }
        resumedCache = opened
        resumedAt = now
        return opened
    }

    private fun packageImportance(am: ActivityManager, packageName: String): Int? {
        return runCatching {
            ActivityManager::class.java
                .getMethod("getPackageImportance", String::class.java)
                .invoke(am, packageName) as Int
        }.getOrNull()
    }

    private fun requestCode(packageName: String): Int =
        REQUEST_BASE + (packageName.hashCode() and 0x3fff)

    @Suppress("DEPRECATION")
    private fun tasks(am: ActivityManager): List<ActivityManager.RunningTaskInfo> {
        val found = LinkedHashMap<Int, ActivityManager.RunningTaskInfo>()
        runCatching { am.getRunningTasks(64) }.getOrNull()?.forEach { found[it.taskId] = it }
        runCatching {
            am.getRecentTasks(64, ActivityManager.RECENT_WITH_EXCLUDED)
                .map { recent ->
                    val info = ActivityManager.RunningTaskInfo()
                    info.taskId = if (Build.VERSION.SDK_INT >= 29) recent.taskId else {
                        @Suppress("DEPRECATION")
                        recent.id
                    }
                    info.baseActivity = recent.baseIntent?.component
                    info.topActivity = recent.origActivity ?: recent.baseIntent?.component
                    info
                }
        }.getOrNull()?.forEach { found.putIfAbsent(it.taskId, it) }
        return found.values.toList()
    }

    private fun ActivityManager.RunningTaskInfo.matches(packageName: String): Boolean {
        val top = topActivity?.packageName
        val base = baseActivity?.packageName
        return top == packageName || base == packageName
    }

    private fun Context.findActivity(): Activity? {
        var current: Context = this
        while (current is android.content.ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private fun xtMapperResolve(context: Context): android.content.pm.ResolveInfo? {
        val pm = context.packageManager
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(
                launch,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launch, PackageManager.MATCH_ALL)
        }
        return resolved.firstOrNull {
            val pkg = it.activityInfo.packageName
            pkg == XTMAPPER_PACKAGE ||
                it.loadLabel(pm).toString().contains("xtmapper", ignoreCase = true)
        }
    }
}
