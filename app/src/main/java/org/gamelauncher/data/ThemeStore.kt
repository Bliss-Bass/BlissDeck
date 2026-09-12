package org.gamelauncher.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class ThemeStore(context: Context) {
    private val app = context.applicationContext
    private val dir = File(app.filesDir, "themes").apply { mkdirs() }
    private val prefs = app.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val _resolved = MutableStateFlow(ThemePack.Default)
    private val _entries = MutableStateFlow<List<ThemeEntry>>(emptyList())
    val resolved: StateFlow<ThemePack> = _resolved
    val entries: StateFlow<List<ThemeEntry>> = _entries

    init {
        refresh()
    }

    fun refresh() {
        val enabled = enabledIds()
        val listed = catalog()
        _entries.value = listed.map {
            it.copy(enabled = it.id == "default" || it.id in enabled)
        }
        val maps = mutableListOf(loadBuiltin("default.ini"))
        enabled.forEach { id ->
            loadSections(id)?.let { maps += it }
        }
        var merged: Map<String, Map<String, String>> = emptyMap()
        maps.forEach { merged = mergeIni(merged, it) }
        _resolved.value = ThemePack.fromIni(merged)
    }

    fun toggle(id: String) {
        if (id == "default") return
        val current = enabledIds().toMutableList()
        if (id in current) current.remove(id) else current.add(id)
        prefs.edit().putString(KEY_ENABLED, current.joinToString(",")).apply()
        refresh()
    }

    fun importFrom(uri: Uri): Boolean {
        val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return false
        val sections = parseIni(text)
        val parsed = ThemePack.fromIni(sections)
        val id = parsed.id.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "imported" }
        val unique = uniqueId(id)
        val named = parsed.copy(id = unique, name = parsed.name.ifBlank { unique })
        File(dir, "$unique.ini").writeText(named.toIni())
        val current = enabledIds().toMutableList()
        if (unique !in current) current.add(unique)
        prefs.edit().putString(KEY_ENABLED, current.joinToString(",")).apply()
        refresh()
        return true
    }

    fun exportCustom(): String = customFile().readText()

    fun patchCustom(block: (ThemePack) -> ThemePack) {
        val next = block(_resolved.value).copy(id = "custom", name = "Custom", builtin = false)
        customFile().writeText(next.toIni())
        val current = enabledIds().toMutableList()
        current.remove("custom")
        current.add("custom")
        prefs.edit().putString(KEY_ENABLED, current.joinToString(",")).apply()
        refresh()
    }

    fun delete(id: String) {
        if (id == "default" || builtinOverlay(id) != null) return
        File(dir, "$id.ini").delete()
        File(dir, "$id.cfg").delete()
        val current = enabledIds().filter { it != id }
        prefs.edit().putString(KEY_ENABLED, current.joinToString(",")).apply()
        refresh()
    }

    private fun catalog(): List<ThemeEntry> {
        val out = ArrayList<ThemeEntry>()
        out += ThemeEntry("default", "Default", "BlissDeck", builtin = true, enabled = true)
        out += ThemeEntry("midnight", "Midnight", "BlissDeck", builtin = true, enabled = false)
        if (customFile().exists()) {
            val custom = ThemePack.fromIni(parseIni(customFile().readText()))
            out += ThemeEntry("custom", custom.name.ifBlank { "Custom" }, custom.author, builtin = false, enabled = false)
        }
        dir.listFiles()?.filter { it.extension.equals("ini", true) || it.extension.equals("cfg", true) }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val id = file.nameWithoutExtension
                if (id == "custom") return@forEach
                val pack = runCatching { ThemePack.fromIni(parseIni(file.readText())) }.getOrNull()
                    ?: return@forEach
                out += ThemeEntry(id, pack.name, pack.author, builtin = false, enabled = false)
            }
        return out.distinctBy { it.id }
    }

    private fun enabledIds(): List<String> =
        prefs.getString(KEY_ENABLED, "")
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "default" }

    private fun loadSections(id: String): Map<String, Map<String, String>>? {
        builtinOverlay(id)?.let { return loadBuiltin(it) }
        val file = File(dir, "$id.ini").takeIf { it.exists() }
            ?: File(dir, "$id.cfg").takeIf { it.exists() }
            ?: return null
        return parseIni(file.readText())
    }

    private fun loadBuiltin(fileName: String): Map<String, Map<String, String>> {
        val text = app.assets.open("themes/$fileName").bufferedReader().use { it.readText() }
        return parseIni(text)
    }

    private fun builtinOverlay(id: String): String? = when (id) {
        "midnight" -> "midnight.ini"
        else -> null
    }

    private fun customFile(): File = File(dir, "custom.ini")

    private fun uniqueId(base: String): String {
        var id = base
        var n = 2
        while (File(dir, "$id.ini").exists() || id == "default" || id == "midnight") {
            id = "${base}_$n"
            n++
        }
        return id
    }

    private companion object {
        const val KEY_ENABLED = "enabled_themes"
    }
}

val LocalThemeStore = staticCompositionLocalOf<ThemeStore> {
    error("ThemeStore not provided")
}
