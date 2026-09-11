package org.gamelauncher.data

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class Artwork(
    val packageName: String,
    val steamGridId: String?,
    val coverUrl: String?,
    val heroUrl: String?,
    val icon: Drawable?,
) {
    fun imageUrl(landscape: Boolean): String? =
        if (landscape) heroUrl ?: coverUrl else coverUrl ?: heroUrl
}

class ArtworkRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("artwork", Context.MODE_PRIVATE)
    private val client = SteamGridClient()
    private val memory = ConcurrentHashMap<String, Artwork>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val _epoch = MutableStateFlow(0)
    private var authFailed = false

    val epoch: StateFlow<Int> = _epoch

    var apiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_API, value.trim()).apply()
            authFailed = false
            memory.clear()
            bump()
        }

    fun steamGridId(packageName: String): String? =
        prefs.getString(idKey(packageName), null)?.takeIf { it.isNotBlank() }

    fun peek(packageName: String): Artwork? = memory[packageName]

    fun iconFor(packageName: String): Drawable? =
        runCatching { appContext.packageManager.getApplicationIcon(packageName) }.getOrNull()

    suspend fun resolve(packageName: String, title: String, isGame: Boolean): Artwork =
        withContext(Dispatchers.IO) {
            val mutex = locks.getOrPut(packageName) { Mutex() }
            mutex.withLock {
                memory[packageName]?.let { return@withLock it }
                val icon = iconFor(packageName)
                val savedId = steamGridId(packageName)
                val savedCover = prefs.getString(coverKey(packageName), null)
                val savedHero = prefs.getString(heroKey(packageName), null)
                if (savedCover != null || savedHero != null) {
                    return@withLock Artwork(packageName, savedId, savedCover, savedHero, icon).also {
                        memory[packageName] = it
                    }
                }
                val key = apiKey
                if (key.isBlank() || authFailed || (!isGame && savedId == null)) {
                    return@withLock Artwork(packageName, savedId, null, null, icon).also {
                        memory[packageName] = it
                    }
                }
                val resolved = runCatching {
                    val id = savedId ?: matchId(key, title)
                        ?: return@runCatching Artwork(packageName, null, null, null, icon)
                    val art = client.artwork(key, id)
                    persist(packageName, id, art.coverUrl, art.heroUrl)
                    Artwork(packageName, id, art.coverUrl, art.heroUrl, icon)
                }.getOrElse { error ->
                    if (error is SteamGridAuthException) authFailed = true
                    Artwork(packageName, savedId, null, null, icon)
                }
                memory[packageName] = resolved
                resolved
            }
        }

    fun setSteamGridId(packageName: String, id: String) {
        val trimmed = id.trim()
        prefs.edit()
            .putString(idKey(packageName), trimmed.ifBlank { null })
            .remove(coverKey(packageName))
            .remove(heroKey(packageName))
            .apply()
        memory.remove(packageName)
        bump()
    }

    private fun persist(packageName: String, id: String, cover: String?, hero: String?) {
        prefs.edit()
            .putString(idKey(packageName), id)
            .putString(coverKey(packageName), cover)
            .putString(heroKey(packageName), hero)
            .apply()
    }

    private fun matchId(apiKey: String, title: String): String? {
        val hits = client.search(apiKey, title)
        val needle = title.trim().lowercase()
        hits.firstOrNull { it.name.lowercase() == needle }?.let { return it.id }
        hits.firstOrNull { it.verified && it.name.lowercase().startsWith(needle) }?.let { return it.id }
        val best = hits.maxByOrNull { similarity(needle, it.name.lowercase()) } ?: return null
        return best.id.takeIf { similarity(needle, best.name.lowercase()) >= 0.5f }
    }

    private fun bump() {
        _epoch.value = _epoch.value + 1
    }

    private fun idKey(pkg: String) = "id.$pkg"
    private fun coverKey(pkg: String) = "cover.$pkg"
    private fun heroKey(pkg: String) = "hero.$pkg"

    companion object {
        private const val KEY_API = "api_key"
    }
}

val LocalArtwork = staticCompositionLocalOf<ArtworkRepository> {
    error("ArtworkRepository not provided")
}

private fun similarity(a: String, b: String): Float {
    if (a == b) return 1f
    if (a.startsWith(b) || b.startsWith(a)) return 0.82f
    val ta = a.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }.toSet()
    val tb = b.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }.toSet()
    if (ta.isEmpty() || tb.isEmpty()) return 0f
    val overlap = (ta intersect tb).size
    return (2f * overlap) / (ta.size + tb.size).toFloat()
}
