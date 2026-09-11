package org.gamelauncher.data

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

class CloseGameService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
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

    private fun AccessibilityWindowInfo.belongsTo(packageName: String): Boolean {
        val root = root
        val pkg = root?.packageName?.toString()
        if (pkg == packageName) return true
        val title = title?.toString().orEmpty()
        return title.contains(packageName, ignoreCase = true)
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
