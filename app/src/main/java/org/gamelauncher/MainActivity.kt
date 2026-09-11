package org.gamelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import org.gamelauncher.ui.LauncherApp
import org.gamelauncher.ui.chrome.applySystemBarMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemBarMode(window, window.decorView, isInMultiWindowMode)
        setContent {
            LauncherApp(onClose = { finish() })
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBarMode(window, window.decorView, isInMultiWindowMode)
        }
    }
}
