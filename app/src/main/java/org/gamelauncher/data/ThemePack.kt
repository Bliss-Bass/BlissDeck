package org.gamelauncher.data

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemeIconStyle { Filled, Outlined }

data class ThemeColors(
    val background: Color = Color(0xFF0E1820),
    val backgroundRaised: Color = Color(0xFF151E26),
    val menu: Color = Color(0xFF2B333C),
    val menuSunken: Color = Color(0xFF22282F),
    val menuDivider: Color = Color(0xFF1A1F24),
    val menuHighlight: Color = Color(0xFF3C454F),
    val footer: Color = Color(0xFF000000),
    val topBar: Color = Color(0xFF000000),
    val playGreen: Color = Color(0xFF5CB030),
    val stopRed: Color = Color(0xFFB44545),
    val pill: Color = Color(0xFFF3F5F7),
    val textPrimary: Color = Color(0xFFF7F8FA),
    val textMuted: Color = Color(0xFF8A939C),
    val textDisabled: Color = Color(0xFF5C656E),
    val tile: Color = Color(0xFF2A3138),
    val tileBorder: Color = Color(0xFFFFFFFF),
    val searchField: Color = Color(0xFFF7F8FA),
    val newsBugfix: Color = Color(0xFFC9B37A),
    val newsUpdate: Color = Color(0xFF5AD0E6),
)

enum class ThemeBlurLevel(val radius: Dp) {
    Off(0.dp),
    Soft(10.dp),
    Regular(20.dp),
    Heavy(36.dp),
    ;

    val enabled: Boolean get() = this != Off

    companion object {
        fun from(blur: Boolean, radius: Dp): ThemeBlurLevel {
            if (!blur || radius.value <= 0.5f) return Off
            return entries.drop(1).minBy { kotlin.math.abs(it.radius.value - radius.value) }
        }

        fun parse(raw: String?, radius: Dp, fallback: ThemeBlurLevel): ThemeBlurLevel {
            return when (raw?.lowercase()) {
                "off", "false", "0", "no" -> Off
                "soft" -> Soft
                "regular" -> Regular
                "heavy" -> Heavy
                "on", "true", "yes", "1" -> from(true, radius)
                else -> if (radius.value > 0.5f) from(true, radius) else fallback
            }
        }
    }
}

data class ThemeChrome(
    val topBarHeight: Dp = 52.dp,
    val bottomBarHeight: Dp = 56.dp,
    val topAlpha: Float = 1f,
    val bottomAlpha: Float = 1f,
    val menuAlpha: Float = 1f,
    val blur: Boolean = false,
    val blurRadius: Dp = 16.dp,
) {
    val blurLevel: ThemeBlurLevel get() = ThemeBlurLevel.from(blur, blurRadius)
}

data class ThemeBorders(
    val width: Dp = 4.dp,
    val radius: Dp = 6.dp,
    val color: Color = Color.White,
)

data class ThemeIcons(
    val style: ThemeIconStyle = ThemeIconStyle.Filled,
    val tint: Color = Color.White,
)

data class ThemeLayouts(
    val homeLastPlayed: Boolean = true,
    val homeMedia: Boolean = true,
    val homePlayNow: Boolean = true,
    val homeFeed: Boolean = true,
    val libraryCollections: Boolean = true,
    val gameActivity: Boolean = true,
    val gameCommunity: Boolean = true,
    val gameInfo: Boolean = true,
) {
    fun gameTabs(): List<GamePageTab> {
        val tabs = buildList {
            if (gameActivity) add(GamePageTab.Activity)
            if (gameCommunity) add(GamePageTab.Community)
            if (gameInfo) add(GamePageTab.GameInfo)
        }
        return tabs.ifEmpty { listOf(GamePageTab.Activity) }
    }
}

data class ThemePack(
    val id: String,
    val name: String,
    val author: String = "",
    val builtin: Boolean = false,
    val colors: ThemeColors = ThemeColors(),
    val chrome: ThemeChrome = ThemeChrome(),
    val borders: ThemeBorders = ThemeBorders(),
    val icons: ThemeIcons = ThemeIcons(),
    val layouts: ThemeLayouts = ThemeLayouts(),
) {
    companion object {
        val Default = ThemePack(id = "default", name = "Default", builtin = true)
    }
}

data class ThemeEntry(
    val id: String,
    val name: String,
    val author: String,
    val builtin: Boolean,
    val enabled: Boolean,
)

val LocalTheme = staticCompositionLocalOf { ThemePack.Default }

internal fun parseIni(text: String): Map<String, Map<String, String>> {
    val sections = LinkedHashMap<String, LinkedHashMap<String, String>>()
    var current = "meta"
    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) return@forEach
        if (line.startsWith("[") && line.endsWith("]")) {
            current = line.substring(1, line.length - 1).trim().lowercase()
            sections.getOrPut(current) { LinkedHashMap() }
            return@forEach
        }
        val eq = line.indexOf('=')
        if (eq <= 0) return@forEach
        val key = line.take(eq).trim().lowercase()
        val value = line.substring(eq + 1).trim().trim('"')
        sections.getOrPut(current) { LinkedHashMap() }[key] = value
    }
    return sections
}

internal fun mergeIni(
    base: Map<String, Map<String, String>>,
    overlay: Map<String, Map<String, String>>,
): Map<String, Map<String, String>> {
    val out = LinkedHashMap<String, LinkedHashMap<String, String>>()
    (base.keys + overlay.keys).forEach { section ->
        val merged = LinkedHashMap<String, String>()
        base[section]?.let { merged.putAll(it) }
        overlay[section]?.let { merged.putAll(it) }
        out[section] = merged
    }
    return out
}

internal fun ThemePack.Companion.fromIni(
    sections: Map<String, Map<String, String>>,
    builtin: Boolean = false,
): ThemePack {
    val meta = sections["meta"].orEmpty()
    val colors = sections["colors"].orEmpty()
    val chrome = sections["chrome"].orEmpty()
    val borders = sections["borders"].orEmpty()
    val icons = sections["icons"].orEmpty()
    val home = sections["home"].orEmpty()
    val library = sections["library"].orEmpty()
    val game = sections["game"].orEmpty()
    val fallback = Default
    return ThemePack(
        id = meta["id"]?.ifBlank { null } ?: "theme",
        name = meta["name"]?.ifBlank { null } ?: "Theme",
        author = meta["author"].orEmpty(),
        builtin = builtin,
        colors = ThemeColors(
            background = colors.color("background", fallback.colors.background),
            backgroundRaised = colors.color("background_raised", fallback.colors.backgroundRaised),
            menu = colors.color("menu", fallback.colors.menu),
            menuSunken = colors.color("menu_sunken", fallback.colors.menuSunken),
            menuDivider = colors.color("menu_divider", fallback.colors.menuDivider),
            menuHighlight = colors.color("menu_highlight", fallback.colors.menuHighlight),
            footer = colors.color("footer", fallback.colors.footer),
            topBar = colors.color("top_bar", fallback.colors.topBar),
            playGreen = colors.color("play_green", fallback.colors.playGreen),
            stopRed = colors.color("stop_red", fallback.colors.stopRed),
            pill = colors.color("pill", fallback.colors.pill),
            textPrimary = colors.color("text_primary", fallback.colors.textPrimary),
            textMuted = colors.color("text_muted", fallback.colors.textMuted),
            textDisabled = colors.color("text_disabled", fallback.colors.textDisabled),
            tile = colors.color("tile", fallback.colors.tile),
            tileBorder = colors.color("tile_border", fallback.colors.tileBorder),
            searchField = colors.color("search_field", fallback.colors.searchField),
            newsBugfix = colors.color("news_bugfix", fallback.colors.newsBugfix),
            newsUpdate = colors.color("news_update", fallback.colors.newsUpdate),
        ),
        chrome = ThemeChrome(
            topBarHeight = chrome.dp("top_bar", fallback.chrome.topBarHeight),
            bottomBarHeight = chrome.dp("bottom_bar", fallback.chrome.bottomBarHeight),
            topAlpha = chrome.alpha("top_alpha", fallback.chrome.topAlpha),
            bottomAlpha = chrome.alpha("bottom_alpha", fallback.chrome.bottomAlpha),
            menuAlpha = chrome.alpha("menu_alpha", fallback.chrome.menuAlpha),
            blur = run {
                val level = ThemeBlurLevel.parse(
                    chrome["blur"],
                    chrome.dp("blur_radius", fallback.chrome.blurRadius),
                    fallback.chrome.blurLevel,
                )
                level.enabled
            },
            blurRadius = run {
                val level = ThemeBlurLevel.parse(
                    chrome["blur"],
                    chrome.dp("blur_radius", fallback.chrome.blurRadius),
                    fallback.chrome.blurLevel,
                )
                if (level == ThemeBlurLevel.Off) {
                    chrome.dp("blur_radius", fallback.chrome.blurRadius)
                } else {
                    level.radius
                }
            },
        ),
        borders = ThemeBorders(
            width = borders.dp("width", fallback.borders.width),
            radius = borders.dp("radius", fallback.borders.radius),
            color = borders.color("color", fallback.borders.color),
        ),
        icons = ThemeIcons(
            style = if (icons["style"].equals("outlined", true)) {
                ThemeIconStyle.Outlined
            } else {
                ThemeIconStyle.Filled
            },
            tint = icons.color("tint", fallback.icons.tint),
        ),
        layouts = ThemeLayouts(
            homeLastPlayed = home.bool("last_played", fallback.layouts.homeLastPlayed),
            homeMedia = home.bool("media", fallback.layouts.homeMedia),
            homePlayNow = home.bool("play_now", fallback.layouts.homePlayNow),
            homeFeed = home.bool("feed", fallback.layouts.homeFeed),
            libraryCollections = library.bool("collections", fallback.layouts.libraryCollections),
            gameActivity = game.bool("activity", fallback.layouts.gameActivity),
            gameCommunity = game.bool("community", fallback.layouts.gameCommunity),
            gameInfo = game.bool("info", fallback.layouts.gameInfo),
        ),
    )
}

fun ThemePack.toIni(): String = buildString {
    fun section(name: String, body: StringBuilder.() -> Unit) {
        append('[').append(name).append("]\n")
        body()
        append('\n')
    }
    fun StringBuilder.kv(key: String, value: String) {
        append(key).append('=').append(value).append('\n')
    }
    append("; BlissDeck theme\n\n")
    section("meta") {
        kv("id", id)
        kv("name", name)
        kv("author", author)
    }
    section("colors") {
        kv("background", colors.background.hex())
        kv("background_raised", colors.backgroundRaised.hex())
        kv("menu", colors.menu.hex())
        kv("menu_sunken", colors.menuSunken.hex())
        kv("menu_divider", colors.menuDivider.hex())
        kv("menu_highlight", colors.menuHighlight.hex())
        kv("footer", colors.footer.hex())
        kv("top_bar", colors.topBar.hex())
        kv("play_green", colors.playGreen.hex())
        kv("stop_red", colors.stopRed.hex())
        kv("pill", colors.pill.hex())
        kv("text_primary", colors.textPrimary.hex())
        kv("text_muted", colors.textMuted.hex())
        kv("text_disabled", colors.textDisabled.hex())
        kv("tile", colors.tile.hex())
        kv("tile_border", colors.tileBorder.hex())
        kv("search_field", colors.searchField.hex())
        kv("news_bugfix", colors.newsBugfix.hex())
        kv("news_update", colors.newsUpdate.hex())
    }
    section("chrome") {
        kv("top_bar", chrome.topBarHeight.value.toInt().toString())
        kv("bottom_bar", chrome.bottomBarHeight.value.toInt().toString())
        kv("top_alpha", (chrome.topAlpha * 100f).toInt().toString())
        kv("bottom_alpha", (chrome.bottomAlpha * 100f).toInt().toString())
        kv("menu_alpha", (chrome.menuAlpha * 100f).toInt().toString())
        kv("blur", chrome.blurLevel.name.lowercase())
        kv("blur_radius", chrome.blurLevel.radius.value.toInt().toString())
    }
    section("borders") {
        kv("width", borders.width.value.toInt().toString())
        kv("radius", borders.radius.value.toInt().toString())
        kv("color", borders.color.hex())
    }
    section("icons") {
        kv("style", if (icons.style == ThemeIconStyle.Outlined) "outlined" else "filled")
        kv("tint", icons.tint.hex())
    }
    section("home") {
        kv("last_played", layouts.homeLastPlayed.toString())
        kv("media", layouts.homeMedia.toString())
        kv("play_now", layouts.homePlayNow.toString())
        kv("feed", layouts.homeFeed.toString())
    }
    section("library") {
        kv("collections", layouts.libraryCollections.toString())
    }
    section("game") {
        kv("activity", layouts.gameActivity.toString())
        kv("community", layouts.gameCommunity.toString())
        kv("info", layouts.gameInfo.toString())
    }
}

private fun Map<String, String>.color(key: String, fallback: Color): Color =
    this[key]?.toColorOrNull() ?: fallback

private fun Map<String, String>.dp(key: String, fallback: Dp): Dp =
    this[key]?.toFloatOrNull()?.dp ?: fallback

private fun Map<String, String>.alpha(key: String, fallback: Float): Float {
    val raw = this[key] ?: return fallback
    val n = raw.toFloatOrNull() ?: return fallback
    return if (n > 1f) (n / 100f).coerceIn(0f, 1f) else n.coerceIn(0f, 1f)
}

private fun Map<String, String>.bool(key: String, fallback: Boolean): Boolean {
    val raw = this[key]?.lowercase() ?: return fallback
    return when (raw) {
        "1", "true", "yes", "on" -> true
        "0", "false", "no", "off" -> false
        else -> fallback
    }
}

private fun String.toColorOrNull(): Color? {
    val hex = removePrefix("#").trim()
    val value = hex.toLongOrNull(16) ?: return null
    return when (hex.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> null
    }
}

private fun Color.hex(): String {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return if (a == 255) {
        "#%02X%02X%02X".format(r, g, b)
    } else {
        "#%02X%02X%02X%02X".format(a, r, g, b)
    }
}
