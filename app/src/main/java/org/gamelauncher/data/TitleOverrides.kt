package org.gamelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

enum class WindowingMode { Auto, Freeform, Standard }

enum class LaunchIntentKind { Auto, Launcher, Leanback }

data class TitleOverride(
    val markedGame: Boolean? = null,
    val markedMedia: Boolean? = null,
    val windowing: WindowingMode? = null,
    val launchIntent: LaunchIntentKind? = null,
    val extras: String = "",
    val activity: String = "",
) {
    val isBlank: Boolean
        get() = markedGame == null &&
            markedMedia == null &&
            windowing == null &&
            launchIntent == null &&
            extras.isBlank() &&
            activity.isBlank()
}

class TitleOverridesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("title_overrides", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Map<String, TitleOverride>> = _state

    operator fun get(packageName: String): TitleOverride =
        _state.value[packageName] ?: TitleOverride()

    fun update(packageName: String, block: (TitleOverride) -> TitleOverride) {
        val next = block(this[packageName])
        val map = _state.value.toMutableMap()
        if (next.isBlank) map.remove(packageName) else map[packageName] = next
        write(map)
    }

    private fun read(): Map<String, TitleOverride> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return runCatching { parse(raw) }.getOrDefault(emptyMap())
    }

    private fun write(value: Map<String, TitleOverride>) {
        prefs.edit().putString(KEY, toJson(value).toString()).apply()
        _state.value = value
    }

    private fun parse(raw: String): Map<String, TitleOverride> {
        val json = JSONObject(raw)
        val out = LinkedHashMap<String, TitleOverride>()
        json.keys().forEach { pkg ->
            val obj = json.optJSONObject(pkg) ?: return@forEach
            val override = TitleOverride(
                markedGame = if (obj.has("game")) obj.optBoolean("game") else null,
                markedMedia = if (obj.has("media")) obj.optBoolean("media") else null,
                windowing = enumOrNull<WindowingMode>(obj.optString("windowing")),
                launchIntent = enumOrNull<LaunchIntentKind>(obj.optString("intent")),
                extras = obj.optString("extras"),
                activity = obj.optString("activity"),
            )
            if (!override.isBlank) out[pkg] = override
        }
        return out
    }

    private fun toJson(value: Map<String, TitleOverride>) = JSONObject().apply {
        value.forEach { (pkg, override) ->
            put(
                pkg,
                JSONObject().apply {
                    if (override.markedGame != null) put("game", override.markedGame)
                    if (override.markedMedia != null) put("media", override.markedMedia)
                    if (override.windowing != null) put("windowing", override.windowing.name)
                    if (override.launchIntent != null) put("intent", override.launchIntent.name)
                    if (override.extras.isNotBlank()) put("extras", override.extras)
                    if (override.activity.isNotBlank()) put("activity", override.activity)
                },
            )
        }
    }

    private companion object {
        const val KEY = "overrides"
    }
}

val LocalTitles = staticCompositionLocalOf<TitleOverridesStore> {
    error("TitleOverridesStore not provided")
}

private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
    enumValues<T>().firstOrNull { it.name == raw }

fun LibrarySnapshot.withOverrides(overrides: Map<String, TitleOverride>): LibrarySnapshot {
    if (overrides.isEmpty()) return this
    val installed = installed.map { app ->
        val override = overrides[app.packageName]
        val isGame = override?.markedGame ?: app.detectedGame
        val isMedia = override?.markedMedia ?: app.detectedMedia
        if (isGame == app.isGame && isMedia == app.isMedia) app
        else app.copy(isGame = isGame, isMedia = isMedia)
    }
    return copy(
        installed = installed,
        games = installed.filter { it.isGame }.map { it.toGame() },
    )
}

fun applyAmExtras(intent: Intent, raw: String) {
    val tokens = tokenizeAmArgs(raw)
    var i = 0
    while (i < tokens.size) {
        val flag = tokens[i]
        val key = tokens.getOrNull(i + 1)
        val value = tokens.getOrNull(i + 2)
        when (flag) {
            "--es", "-e", "--e" -> {
                if (key != null && value != null) intent.putExtra(key, value)
                i += 3
            }
            "--ez" -> {
                if (key != null && value != null) intent.putExtra(key, value.toBooleanStrictOrNull() ?: false)
                i += 3
            }
            "--ei" -> {
                if (key != null && value != null) intent.putExtra(key, value.toIntOrNull() ?: 0)
                i += 3
            }
            "--el" -> {
                if (key != null && value != null) intent.putExtra(key, value.toLongOrNull() ?: 0L)
                i += 3
            }
            "--ef" -> {
                if (key != null && value != null) intent.putExtra(key, value.toFloatOrNull() ?: 0f)
                i += 3
            }
            "--esa" -> {
                if (key != null && value != null) {
                    intent.putExtra(key, value.split(',').map { it.trim() }.toTypedArray())
                }
                i += 3
            }
            "-n" -> {
                if (key != null) applyComponent(intent, key)
                i += 2
            }
            else -> i += 1
        }
    }
}

fun applyComponent(intent: Intent, spec: String, fallbackPackage: String? = null) {
    val trimmed = spec.trim().removePrefix("component:").trim()
    val slash = trimmed.indexOf('/')
    val component = when {
        slash >= 0 -> {
            val pkg = trimmed.substring(0, slash).ifBlank { fallbackPackage } ?: return
            var cls = trimmed.substring(slash + 1)
            if (cls.startsWith('.')) cls = pkg + cls
            ComponentName(pkg, cls)
        }
        fallbackPackage != null && trimmed.isNotBlank() -> {
            val cls = if (trimmed.startsWith('.')) fallbackPackage + trimmed else trimmed
            ComponentName(fallbackPackage, cls)
        }
        else -> return
    }
    intent.component = component
}

internal fun tokenizeAmArgs(raw: String): List<String> {
    val out = ArrayList<String>()
    val s = raw.trim()
    var i = 0
    while (i < s.length) {
        while (i < s.length && s[i].isWhitespace()) i++
        if (i >= s.length) break
        val quote = s[i]
        if (quote == '\'' || quote == '"') {
            i++
            val start = i
            while (i < s.length && s[i] != quote) i++
            out += s.substring(start, i.coerceAtMost(s.length))
            if (i < s.length) i++
        } else {
            val start = i
            while (i < s.length && !s[i].isWhitespace()) i++
            out += s.substring(start, i)
        }
    }
    return out
}

const val UNITY_FORCE_GLES = "--es unity -force-gles"
const val UNITY_FORCE_VULKAN = "--es unity -force-vulkan"
