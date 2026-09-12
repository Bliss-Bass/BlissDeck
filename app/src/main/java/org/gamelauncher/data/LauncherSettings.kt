package org.gamelauncher.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RecentsLayout { Coverflow, Row }

enum class RecentsSize { Compact, Comfortable, Large }

enum class RecentsShape { Square, Wide }

enum class RecentsArt { Icon, Cover }

enum class ArtworkIconSource { App, SteamGrid }

enum class ArtworkCoverStyle { Tall, Wide }

enum class ArtworkBackdrop { Hero, Tall, Play }

data class LauncherPrefs(
    val recentsLayout: RecentsLayout = RecentsLayout.Coverflow,
    val recentsSize: RecentsSize = RecentsSize.Comfortable,
    val recentsShape: RecentsShape = RecentsShape.Square,
    val recentsArt: RecentsArt = RecentsArt.Icon,
    val recentsTilt: Boolean = true,
    val ambientBackdrop: Boolean = true,
    val showSelectedTitle: Boolean = true,
    val escAsBack: Boolean = true,
    val bAsBack: Boolean = true,
    val winOpensMenu: Boolean = true,
    val hoverCaptions: Boolean = true,
    val showNotificationCount: Boolean = true,
    val whatsNewCount: Int = 16,
    val storePackage: String = "",
    val iconSource: ArtworkIconSource = ArtworkIconSource.App,
    val coverStyle: ArtworkCoverStyle = ArtworkCoverStyle.Tall,
    val backdrop: ArtworkBackdrop = ArtworkBackdrop.Hero,
)

data class RecentsMetrics(
    val heroWidth: Dp,
    val heroHeight: Dp,
    val thumbWidth: Dp,
    val thumbHeight: Dp,
    val peek: Dp,
    val overlap: Dp,
)

fun LauncherPrefs.recentsMetrics(): RecentsMetrics {
    val (heroW, heroH, square) = when (recentsSize) {
        RecentsSize.Compact -> Triple(520.dp, 256.dp, 176.dp)
        RecentsSize.Comfortable -> Triple(680.dp, 336.dp, 228.dp)
        RecentsSize.Large -> Triple(800.dp, 392.dp, 268.dp)
    }
    val thumbW = if (recentsShape == RecentsShape.Square) square else heroW * 0.38f
    val thumbH = if (recentsShape == RecentsShape.Square) square else heroH * 0.62f
    val peek = if (recentsShape == RecentsShape.Square) thumbW * 0.67f else thumbW * 0.52f
    return RecentsMetrics(heroW, heroH, thumbW, thumbH, peek, 28.dp)
}

class LauncherSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<LauncherPrefs> = _state

    fun update(block: (LauncherPrefs) -> LauncherPrefs) {
        val next = block(_state.value)
        write(next)
        _state.value = next
    }

    private fun read(): LauncherPrefs = LauncherPrefs(
        recentsLayout = enumValue(prefs.getString(KEY_LAYOUT, null), RecentsLayout.Coverflow),
        recentsSize = enumValue(prefs.getString(KEY_SIZE, null), RecentsSize.Comfortable),
        recentsShape = enumValue(prefs.getString(KEY_SHAPE, null), RecentsShape.Square),
        recentsArt = enumValue(prefs.getString(KEY_ART, null), RecentsArt.Icon),
        recentsTilt = prefs.getBoolean(KEY_TILT, true),
        ambientBackdrop = prefs.getBoolean(KEY_AMBIENT, true),
        showSelectedTitle = prefs.getBoolean(KEY_TITLE, true),
        escAsBack = prefs.getBoolean(KEY_ESC, true),
        bAsBack = prefs.getBoolean(KEY_B, true),
        winOpensMenu = prefs.getBoolean(KEY_WIN, true),
        hoverCaptions = prefs.getBoolean(KEY_HOVER, true),
        showNotificationCount = prefs.getBoolean(KEY_NOTIF, true),
        whatsNewCount = prefs.getInt(KEY_NEWS_COUNT, 16).coerceIn(8, 32),
        storePackage = prefs.getString(KEY_STORE, "").orEmpty(),
        iconSource = enumValue(prefs.getString(KEY_ICON_SOURCE, null), ArtworkIconSource.App),
        coverStyle = enumValue(prefs.getString(KEY_COVER_STYLE, null), ArtworkCoverStyle.Tall),
        backdrop = enumValue(prefs.getString(KEY_BACKDROP, null), ArtworkBackdrop.Hero),
    )

    private fun write(value: LauncherPrefs) {
        prefs.edit()
            .putString(KEY_LAYOUT, value.recentsLayout.name)
            .putString(KEY_SIZE, value.recentsSize.name)
            .putString(KEY_SHAPE, value.recentsShape.name)
            .putString(KEY_ART, value.recentsArt.name)
            .putBoolean(KEY_TILT, value.recentsTilt)
            .putBoolean(KEY_AMBIENT, value.ambientBackdrop)
            .putBoolean(KEY_TITLE, value.showSelectedTitle)
            .putBoolean(KEY_ESC, value.escAsBack)
            .putBoolean(KEY_B, value.bAsBack)
            .putBoolean(KEY_WIN, value.winOpensMenu)
            .putBoolean(KEY_HOVER, value.hoverCaptions)
            .putBoolean(KEY_NOTIF, value.showNotificationCount)
            .putInt(KEY_NEWS_COUNT, value.whatsNewCount)
            .putString(KEY_STORE, value.storePackage)
            .putString(KEY_ICON_SOURCE, value.iconSource.name)
            .putString(KEY_COVER_STYLE, value.coverStyle.name)
            .putString(KEY_BACKDROP, value.backdrop.name)
            .apply()
    }

    private companion object {
        const val KEY_LAYOUT = "recents_layout"
        const val KEY_SIZE = "recents_size"
        const val KEY_SHAPE = "recents_shape"
        const val KEY_ART = "recents_art"
        const val KEY_TILT = "recents_tilt"
        const val KEY_AMBIENT = "ambient_backdrop"
        const val KEY_TITLE = "show_selected_title"
        const val KEY_ESC = "esc_as_back"
        const val KEY_B = "b_as_back"
        const val KEY_WIN = "win_opens_menu"
        const val KEY_HOVER = "hover_captions"
        const val KEY_NOTIF = "show_notification_count"
        const val KEY_NEWS_COUNT = "whats_new_count"
        const val KEY_STORE = "store_package"
        const val KEY_ICON_SOURCE = "artwork_icon_source"
        const val KEY_COVER_STYLE = "artwork_cover_style"
        const val KEY_BACKDROP = "artwork_backdrop"
    }
}

val LocalSettings = staticCompositionLocalOf<LauncherSettings> {
    error("LauncherSettings not provided")
}

private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback
