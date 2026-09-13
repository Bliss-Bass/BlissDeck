package org.gamelauncher.data

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

class CloseGameService : AccessibilityService() {
    private val refresh = Handler(Looper.getMainLooper())
    private val publishWindows = Runnable { AppPresence.publishOpen(scanWindows()) }

    override fun onServiceConnected() {
        instance = this
        AppPresence.setConnected(true)
        refresh.removeCallbacks(publishWindows)
        refresh.post(publishWindows)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        refresh.removeCallbacks(publishWindows)
        refresh.postDelayed(publishWindows, 60)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        refresh.removeCallbacks(publishWindows)
        if (instance === this) instance = null
        AppPresence.setConnected(false)
        super.onDestroy()
    }

    private fun scanWindows(): List<RunningApp> {
        val self = packageName
        val pm = packageManager
        val seen = LinkedHashMap<String, RunningApp>()
        windows.orEmpty().forEach { window ->
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return@forEach
            if (window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) return@forEach
            val pkg = window.ownerPackage() ?: return@forEach
            if (GameSession.hideFromSwitcher(pkg, self)) return@forEach
            val title = window.title?.toString()?.takeIf { it.isNotBlank() && '/' !in it }
                ?: runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                    .getOrDefault(pkg)
            seen.putIfAbsent(pkg, RunningApp(pkg, title, taskId = null))
        }
        return seen.values.toList()
    }

    fun closePackage(packageName: String): Boolean {
        val windows = windows ?: return false
        val match = windows.filter { it.belongsTo(packageName) }
            .maxByOrNull { it.area() }
            ?: return false
        if (clickCloseNode(match)) return true
        val bounds = Rect()
        match.getBoundsInScreen(bounds)
        if (bounds.width() < 80 || bounds.height() < 40) return false
        val x = bounds.right - 36f
        val y = bounds.top + 22f
        return tap(x, y)
    }

    private fun clickCloseNode(window: AccessibilityWindowInfo): Boolean {
        val root = window.root ?: return false
        val hit = findCloseNode(root)
        return hit?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun findCloseNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = sequenceOf(node.contentDescription, node.text)
            .map { it?.toString()?.lowercase().orEmpty() }
            .firstOrNull { it == "close" || it == "cerrar" }
        if (label != null && node.isClickable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findCloseNode(child)?.let { return it }
        }
        return null
    }

    private fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun AccessibilityWindowInfo.ownerPackage(): String? {
        val root = root
        val fromRoot = root?.packageName?.toString()
        if (root != null && android.os.Build.VERSION.SDK_INT < 33) {
            @Suppress("DEPRECATION")
            runCatching { root.recycle() }
        }
        if (!fromRoot.isNullOrBlank()) return fromRoot
        return packageFromWindowTitle(title?.toString())
    }

    private fun AccessibilityWindowInfo.belongsTo(packageName: String): Boolean {
        if (ownerPackage() == packageName) return true
        val title = title?.toString().orEmpty()
        return title.contains(packageName, ignoreCase = true)
    }

    private fun packageFromWindowTitle(title: String?): String? {
        val raw = title?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val slash = raw.indexOf('/')
        if (slash > 0) {
            val pkg = raw.substring(0, slash).trim()
            if (pkg.contains('.')) return pkg
        }
        return null
    }

    private fun AccessibilityWindowInfo.area(): Int {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        return bounds.width() * bounds.height()
    }

    companion object {
        @Volatile
        var instance: CloseGameService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            if (instance != null) return true
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            val component = ComponentName(context, CloseGameService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(component, ignoreCase = true) }
        }
    }
}
