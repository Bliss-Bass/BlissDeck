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
    val iconUrl: String?,
    val icon: Drawable?,
) {
    fun imageUrl(landscape: Boolean): String? =
        if (landscape) heroUrl ?: coverUrl else coverUrl ?: heroUrl

    val usesAppIcon: Boolean get() = iconUrl.isNullOrBlank() || iconUrl == ArtCandidate.APP_ICON
}

class ArtworkRepository(
    context: Context,
    private val playNews: PlayNewsRepository? = null,
    private val settings: LauncherSettings? = null,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("artwork", Context.MODE_PRIVATE)
    private val client = SteamGridClient()
    private val memory = ConcurrentHashMap<String, Artwork>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val lists = ConcurrentHashMap<String, List<ArtCandidate>>()
    private val _epoch = MutableStateFlow(0)
    private var authFailed = false

    val epoch: StateFlow<Int> = _epoch

    var apiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_API, value.trim()).apply()
            authFailed = false
            memory.clear()
            lists.clear()
            bump()
        }

    fun steamGridId(packageName: String): String? =
        prefs.getString(idKey(packageName), null)?.takeIf { it.isNotBlank() }

    fun peek(packageName: String): Artwork? = memory[packageName]

    fun iconFor(packageName: String): Drawable? =
        runCatching { appContext.packageManager.getApplicationIcon(packageName) }.getOrNull()

    fun onPlayArtUpdated() {
        var changed = false
        memory.replaceAll { pkg, art ->
            if (!art.heroUrl.isNullOrBlank()) return@replaceAll art
            val playHero = playNews?.heroUrl(pkg)
            if (playHero.isNullOrBlank()) art else {
                changed = true
                art.copy(heroUrl = playHero)
            }
        }
        if (changed) bump()
    }

    suspend fun resolve(packageName: String, title: String, isGame: Boolean): Artwork =
        withContext(Dispatchers.IO) {
            val mutex = locks.getOrPut(packageName) { Mutex() }
            mutex.withLock {
                memory[packageName]?.let { cached ->
                    return@withLock cached.withPlayHero(packageName).also { memory[packageName] = it }
                }
                val icon = iconFor(packageName)
                val savedId = steamGridId(packageName)
                val savedCover = prefs.getString(coverKey(packageName), null)
                val savedHero = prefs.getString(heroKey(packageName), null)
                val savedIcon = prefs.getString(iconKey(packageName), null)
                val playHero = playNews?.heroUrl(packageName)
                val steamAppId = SteamNative.steamAppId(packageName)
                val defaults = settings?.state?.value
                if (savedCover != null || savedHero != null || savedIcon != null) {
                    return@withLock Artwork(
                        packageName,
                        savedId,
                        savedCover ?: steamAppId?.let(SteamNative::cdnCover),
                        savedHero ?: playHero ?: steamAppId?.let(SteamNative::cdnHero),
                        savedIcon,
                        icon,
                    ).also { memory[packageName] = it }
                }
                val key = apiKey
                val wantsRemote = isGame || steamAppId != null || savedId != null
                if (key.isNotBlank() && !authFailed && wantsRemote) {
                    val resolved = runCatching {
                        val id = savedId
                            ?: steamAppId?.let { client.gameIdForSteamApp(key, it) }
                            ?: matchId(key, title)
                            ?: return@runCatching null
                        val art = client.artwork(key, id, tallCover = defaults?.coverStyle != ArtworkCoverStyle.Wide)
                        val cover = art.coverUrl ?: steamAppId?.let(SteamNative::cdnCover)
                        val hero = when (defaults?.backdrop ?: ArtworkBackdrop.Hero) {
                            ArtworkBackdrop.Hero -> art.heroUrl ?: playHero ?: steamAppId?.let(SteamNative::cdnHero)
                            ArtworkBackdrop.Tall -> cover ?: art.heroUrl ?: playHero
                            ArtworkBackdrop.Play -> playHero ?: art.heroUrl ?: steamAppId?.let(SteamNative::cdnHero)
                        }
                        val iconUrl = when {
                            defaults?.iconSource == ArtworkIconSource.SteamGrid -> art.iconUrl
                            else -> null
                        }
                        persist(packageName, id, cover, hero, iconUrl)
                        Artwork(packageName, id, cover, hero, iconUrl, icon)
                    }.getOrElse { error ->
                        if (error is SteamGridAuthException) authFailed = true
                        null
                    }
                    if (resolved != null) {
                        memory[packageName] = resolved
                        return@withLock resolved
                    }
                }
                Artwork(
                    packageName,
                    savedId,
                    steamAppId?.let(SteamNative::cdnCover),
                    playHero ?: steamAppId?.let(SteamNative::cdnHero),
                    null,
                    icon,
                ).also { memory[packageName] = it }
            }
        }

    suspend fun candidates(
        packageName: String,
        title: String,
        isGame: Boolean,
        slot: ArtSlot,
    ): List<ArtCandidate> = withContext(Dispatchers.IO) {
        val art = resolve(packageName, title, isGame)
        val cacheKey = "$packageName.$slot"
        lists[cacheKey]?.let { cached ->
            return@withContext withLocal(packageName, slot, cached)
        }
        val key = apiKey
        val id = art.steamGridId
        val remote = if (key.isNotBlank() && !authFailed && !id.isNullOrBlank()) {
            runCatching { client.list(key, id, slot) }.getOrElse { error ->
                if (error is SteamGridAuthException) authFailed = true
                emptyList()
            }
        } else {
            emptyList()
        }
        lists[cacheKey] = remote
        withLocal(packageName, slot, remote)
    }

    fun select(packageName: String, slot: ArtSlot, url: String?) {
        val value = url?.takeIf { it.isNotBlank() }
        val editor = prefs.edit()
        when (slot) {
            ArtSlot.Cover -> editor.putString(coverKey(packageName), value)
            ArtSlot.Hero -> editor.putString(heroKey(packageName), value)
            ArtSlot.Icon -> editor.putString(iconKey(packageName), value)
        }
        editor.apply()
        val current = memory[packageName]
        if (current != null) {
            memory[packageName] = when (slot) {
                ArtSlot.Cover -> current.copy(coverUrl = value)
                ArtSlot.Hero -> current.copy(heroUrl = value)
                ArtSlot.Icon -> current.copy(iconUrl = value)
            }
        } else {
            memory.remove(packageName)
        }
        bump()
    }

    fun setSteamGridId(packageName: String, id: String) {
        val trimmed = id.trim()
        prefs.edit()
            .putString(idKey(packageName), trimmed.ifBlank { null })
            .remove(coverKey(packageName))
            .remove(heroKey(packageName))
            .remove(iconKey(packageName))
            .apply()
        memory.remove(packageName)
        lists.keys.filter { it.startsWith("$packageName.") }.forEach { lists.remove(it) }
        bump()
    }

    private fun withLocal(
        packageName: String,
        slot: ArtSlot,
        remote: List<ArtCandidate>,
    ): List<ArtCandidate> {
        val extras = buildList {
            when (slot) {
                ArtSlot.Cover -> {
                    SteamNative.steamAppId(packageName)?.let { steam ->
                        add(ArtCandidate("steam-cover", SteamNative.cdnCover(steam), SteamNative.cdnCover(steam), 600, 900, "Steam"))
                    }
                }
                ArtSlot.Hero -> {
                    playNews?.heroUrl(packageName)?.let { url ->
                        add(ArtCandidate("play-hero", url, url, 0, 0, "Play Store"))
                    }
                    SteamNative.steamAppId(packageName)?.let { steam ->
                        add(ArtCandidate("steam-hero", SteamNative.cdnHero(steam), SteamNative.cdnHero(steam), 1920, 620, "Steam"))
                    }
                }
                ArtSlot.Icon -> {
                    add(ArtCandidate("app", ArtCandidate.APP_ICON, "", 0, 0, "App"))
                }
            }
        }
        return (extras + remote).distinctBy { it.url }
    }

    private fun Artwork.withPlayHero(packageName: String): Artwork {
        if (!heroUrl.isNullOrBlank()) return this
        val playHero = playNews?.heroUrl(packageName) ?: return this
        return copy(heroUrl = playHero)
    }

    private fun persist(packageName: String, id: String, cover: String?, hero: String?, icon: String?) {
        prefs.edit()
            .putString(idKey(packageName), id)
            .putString(coverKey(packageName), cover)
            .putString(heroKey(packageName), hero)
            .putString(iconKey(packageName), icon)
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
    private fun iconKey(pkg: String) = "icon.$pkg"

    private companion object {
        const val KEY_API = "api_key"
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
