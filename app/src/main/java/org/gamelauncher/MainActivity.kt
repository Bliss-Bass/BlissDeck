package org.gamelauncher

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import org.gamelauncher.ui.LauncherApp
import org.gamelauncher.ui.chrome.applySystemBarMode

class MainActivity : ComponentActivity() {
    var interceptKey: ((KeyEvent) -> Boolean)? = null
    var onHomePressed: (() -> Unit)? = null
    private var lastHomeAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemBarMode(window, window.decorView, isInMultiWindowMode)
        setContent {
            LauncherApp(onClose = { finish() })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            handleHomePress()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                handleHomePress()
            }
            return true
        }
        if (interceptKey?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    private fun handleHomePress() {
        val now = SystemClock.uptimeMillis()
        if (now - lastHomeAt < 400) return
        lastHomeAt = now
        onHomePressed?.invoke()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBarMode(window, window.decorView, isInMultiWindowMode)
        }
    }
}
