package org.gamelauncher.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GameCollection(
    val id: String,
    val name: String,
    val packageNames: List<String>,
) {
    val size: Int get() = packageNames.size
    fun contains(packageName: String) = packageName in packageNames
}

class CollectionsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("collections", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<List<GameCollection>> = _state

    val favorites: GameCollection
        get() = _state.value.first { it.id == FAVORITES_ID }

    fun create(name: String): GameCollection {
        val trimmed = name.trim().ifBlank { "Collection" }
        val next = GameCollection(UUID.randomUUID().toString(), trimmed, emptyList())
        write(_state.value + next)
        return next
    }

    fun rename(id: String, name: String) {
        val trimmed = name.trim().ifBlank { return }
        write(_state.value.map { if (it.id == id) it.copy(name = trimmed) else it })
    }

    fun delete(id: String) {
        if (id == FAVORITES_ID) return
        write(_state.value.filterNot { it.id == id })
    }

    fun setMembership(id: String, packageName: String, included: Boolean) {
        write(
            _state.value.map { collection ->
                if (collection.id != id) collection
                else {
                    val names = collection.packageNames.toMutableList()
                    if (included && packageName !in names) names += packageName
                    if (!included) names.remove(packageName)
                    collection.copy(packageNames = names)
                }
            },
        )
    }

    fun toggle(id: String, packageName: String) {
        val collection = _state.value.firstOrNull { it.id == id } ?: return
        setMembership(id, packageName, packageName !in collection.packageNames)
    }

    private fun read(): List<GameCollection> {
        val raw = prefs.getString(KEY, null)
        val parsed = parse(raw)
        return if (parsed.any { it.id == FAVORITES_ID }) parsed
        else listOf(GameCollection(FAVORITES_ID, "Favorites", emptyList())) + parsed
    }

    private fun write(value: List<GameCollection>) {
        val ordered = if (value.any { it.id == FAVORITES_ID }) {
            listOf(value.first { it.id == FAVORITES_ID }) + value.filterNot { it.id == FAVORITES_ID }
        } else {
            listOf(GameCollection(FAVORITES_ID, "Favorites", emptyList())) + value
        }
        prefs.edit().putString(KEY, toJson(ordered).toString()).apply()
        _state.value = ordered
    }

    private fun parse(raw: String?): List<GameCollection> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val name = obj.optString("name")
                if (id.isBlank() || name.isBlank()) continue
                val pkgs = obj.optJSONArray("packages") ?: JSONArray()
                val names = buildList {
                    for (j in 0 until pkgs.length()) {
                        pkgs.optString(j).takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
                add(GameCollection(id, name, names))
            }
        }
    }

    private fun toJson(value: List<GameCollection>) = JSONArray().apply {
        value.forEach { collection ->
            put(
                JSONObject().apply {
                    put("id", collection.id)
                    put("name", collection.name)
                    put("packages", JSONArray(collection.packageNames))
                },
            )
        }
    }

    companion object {
        const val FAVORITES_ID = "favorites"
        private const val KEY = "collections"
    }
}

val LocalCollections = staticCompositionLocalOf<CollectionsStore> {
    error("CollectionsStore not provided")
}
