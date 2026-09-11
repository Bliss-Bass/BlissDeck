package org.gamelauncher.ui.chrome

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Desktop caption on this tablet is 66px at 219dpi ≈ 48dp. */
private val FreeformCaptionFallback = WindowInsets(top = 48.dp)

@Composable
fun rememberFreeformWindow(): Boolean {
    val activity = LocalContext.current as ComponentActivity
    var multiWindow by remember { mutableStateOf(activity.isInMultiWindowMode) }

    DisposableEffect(activity) {
        val onMultiWindow = Consumer<MultiWindowModeChangedInfo> { info ->
            multiWindow = info.isInMultiWindowMode
        }
        val onConfig = Consumer<Configuration> {
            multiWindow = activity.isInMultiWindowMode
        }
        activity.addOnMultiWindowModeChangedListener(onMultiWindow)
        activity.addOnConfigurationChangedListener(onConfig)
        onDispose {
            activity.removeOnMultiWindowModeChangedListener(onMultiWindow)
            activity.removeOnConfigurationChangedListener(onConfig)
        }
    }

    // Desktop fullscreen still reports a captionBar source for the restore handle.
    // Only multi-window (freeform) should pad for the title bar.
    return multiWindow
}

@Composable
fun rememberTopChromeInsets(freeform: Boolean): WindowInsets {
    return if (freeform) {
        WindowInsets.captionBar
            .union(FreeformCaptionFallback)
            .only(WindowInsetsSides.Top)
    } else {
        WindowInsets.statusBars.only(WindowInsetsSides.Top)
    }
}

@Composable
fun rememberBottomChromeInsets(freeform: Boolean): WindowInsets {
    return if (freeform) {
        WindowInsets(0, 0, 0, 0)
    } else {
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    }
}

@Composable
fun ApplySystemBarMode(freeform: Boolean) {
    val view = LocalView.current
    val window = (view.context as ComponentActivity).window
    DisposableEffect(freeform) {
        applySystemBarMode(window, view, freeform)
        onDispose { }
    }
}

fun applySystemBarMode(window: Window, view: View, freeform: Boolean) {
    val controller = WindowCompat.getInsetsController(window, view)
    val activity = view.context as? Activity
    if (freeform) {
        controller.show(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= 34) {
            activity?.requestFullscreenMode(Activity.FULLSCREEN_MODE_REQUEST_EXIT, null)
        }
    } else {
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars(),
        )
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (Build.VERSION.SDK_INT >= 34) {
            activity?.requestFullscreenMode(Activity.FULLSCREEN_MODE_REQUEST_ENTER, null)
        }
    }
}
