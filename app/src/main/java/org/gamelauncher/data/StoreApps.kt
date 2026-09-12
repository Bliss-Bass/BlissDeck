package org.gamelauncher.data

import android.content.Context
import android.content.Intent

data class StoreApp(
    val packageName: String,
    val label: String,
)

object StoreApps {
    private val known = listOf(
        "com.android.vending" to "Play Store",
        "com.aurora.store" to "Aurora Store",
        "com.aurora.store.nightly" to "Aurora Store",
        "com.looker.droidify" to "Droid-ify",
        "com.looker.droidify.debug" to "Droid-ify",
        "org.fdroid.fdroid" to "F-Droid",
        "com.machiav3lli.fdroid" to "Neo Store",
        "nya.kitsunyan.foxydroid" to "Foxy Droid",
        "com.aurora.adroid" to "Aurora Droid",
    )

    fun installed(context: Context): List<StoreApp> {
        val pm = context.packageManager
        return known.mapNotNull { (pkg, fallback) ->
            if (pm.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(fallback)
            StoreApp(pkg, label.ifBlank { fallback })
        }.distinctBy { it.packageName }
    }

    fun preferred(context: Context, storedPackage: String): StoreApp? {
        val installed = installed(context)
        if (storedPackage.isNotBlank()) {
            installed.firstOrNull { it.packageName == storedPackage }?.let { return it }
        }
        return installed.singleOrNull()
    }

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
