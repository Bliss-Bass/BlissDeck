package org.gamelauncher.data

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

object GameSession {
    const val XTMAPPER_PACKAGE = "xtr.keymapper"

    private const val TAG = "GameSession"
    private const val WINDOWING_MODE_FREEFORM = 5
    private const val REQUEST_BASE = 0x4100
    private val mainHandler = Handler(Looper.getMainLooper())

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        val activity = context.findActivity()
        return if (activity != null) {
            launchFromActivity(activity, packageName, intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }

    fun close(context: Context, packageName: String) {
        if (packageName.isBlank() || packageName == context.packageName) return
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
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (tasks(am).any { it.matches(packageName) }) return true
        val procs = am.runningAppProcesses ?: return null
        val self = context.packageName
        val match = procs.any { packageName in it.pkgList }
        if (match) return true
        val seesOthers = procs.any { self !in it.pkgList }
        if (!seesOthers) return null
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
