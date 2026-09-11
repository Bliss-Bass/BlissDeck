package org.gamelauncher.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class PlayNewsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client = PlayStoreClient()
    private val cacheDir = File(appContext.cacheDir, "playnews").also { it.mkdirs() }
    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _loading = MutableStateFlow(false)

    val news: StateFlow<List<NewsItem>> = _news
    val loading: StateFlow<Boolean> = _loading

    suspend fun refresh(snapshot: LibrarySnapshot) = withContext(Dispatchers.IO) {
        _loading.value = true
        try {
            val cached = assemble(snapshot, readCached(snapshot))
            if (cached.isNotEmpty()) _news.value = cached

            val fetched = fetchStale(snapshot)
            _news.value = assemble(snapshot, fetched)
        } finally {
            _loading.value = false
        }
    }

    private suspend fun fetchStale(snapshot: LibrarySnapshot): Map<String, CachedListing> {
        val known = readCached(snapshot).toMutableMap()
        val targets = fetchTargets(snapshot).filter { app ->
            val cached = known[app.packageName]
            cached == null || cached.stale()
        }
        if (targets.isEmpty()) return known

        val gate = Semaphore(3)
        coroutineScope {
            targets.map { app ->
                async {
                    gate.withPermit {
                        val listing = runCatching { client.fetch(app.packageName) }.getOrNull()
                        val cached = listing.toCached()
                        writeCache(app.packageName, cached)
                        Log.d(TAG, "listing ${app.packageName} version=${cached.version} date=${cached.updatedDisplay} hero=${!cached.heroUrl.isNullOrBlank()}")
                        known[app.packageName] = cached
                    }
                }
            }.awaitAll()
        }
        return known
    }

    private fun assemble(
        snapshot: LibrarySnapshot,
        listings: Map<String, CachedListing>,
    ): List<NewsItem> {
        val now = System.currentTimeMillis()
        val games = snapshot.installed.filter { it.isGame }.map { it.id }.toSet()
        return snapshot.installed.mapNotNull { app ->
            val listing = listings[app.packageName] ?: return@mapNotNull null
            if (listing.missing) return@mapNotNull null
            if (listing.version.isNullOrBlank() && listing.whatsNew.isNullOrBlank()) return@mapNotNull null
            if (!include(app, listing, now)) return@mapNotNull null
            listing.toNews(app)
        }.sortedWith(
            compareByDescending<NewsItem> { it.gameId in games }
                .thenByDescending { listings[it.gameId]?.updatedMillis ?: 0L },
        ).take(12)
    }

    private fun include(app: InstalledApp, listing: CachedListing, now: Long): Boolean {
        if (isPlaySkipped(app.packageName)) return false
        if (app.isGame) return true
        if ("launcher" in app.title.lowercase()) return false
        if (!looksLikeGame(app, listing)) return false
        if (listing.version.isNullOrBlank() || listing.whatsNew.isNullOrBlank()) return false
        val updated = listing.updatedMillis ?: app.lastUpdateTime
        if (updated <= 0L) return false
        return now - updated <= OTHER_APP_WINDOW_MS
    }

    private fun looksLikeGame(app: InstalledApp, listing: CachedListing): Boolean {
        val blob = "${app.packageName} ${app.title} ${listing.title.orEmpty()}".lowercase()
        return "game" in blob || "steam" in blob || "emulat" in blob
    }

    private fun fetchTargets(snapshot: LibrarySnapshot): List<InstalledApp> {
        val games = snapshot.installed.filter { it.isGame && !isPlaySkipped(it.packageName) }
        val others = snapshot.installed
            .filter { !it.isGame && !isPlaySkipped(it.packageName) }
            .sortedByDescending { it.lastUpdateTime }
            .take(24)
        return (games + others).distinctBy { it.packageName }
    }

    private fun readCached(snapshot: LibrarySnapshot): Map<String, CachedListing> {
        return snapshot.installed.mapNotNull { app ->
            val file = cacheFile(app.packageName)
            if (!file.isFile) return@mapNotNull null
            val parsed = runCatching { CachedListing.fromJson(JSONObject(file.readText())) }.getOrNull()
                ?: return@mapNotNull null
            app.packageName to parsed
        }.toMap()
    }

    private fun writeCache(packageName: String, listing: CachedListing) {
        runCatching { cacheFile(packageName).writeText(listing.toJson().toString()) }
    }

    private fun cacheFile(packageName: String) = File(cacheDir, "$packageName.json")

    fun heroUrl(packageName: String): String? {
        val file = cacheFile(packageName)
        if (!file.isFile) return null
        return runCatching {
            JSONObject(file.readText()).optString("hero").ifBlank { null }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "PlayNews"
        private const val OTHER_APP_WINDOW_MS = 120L * 24 * 60 * 60 * 1000
    }
}

internal data class CachedListing(
    val title: String?,
    val whatsNew: String?,
    val version: String?,
    val updatedDisplay: String?,
    val updatedMillis: Long?,
    val backgroundUrl: String?,
    val heroUrl: String?,
    val queriedAt: Long,
    val missing: Boolean,
    val hasHeroField: Boolean = true,
) {
    fun stale(): Boolean {
        if (!missing && !hasHeroField) return true
        val ttl = if (missing) MISS_TTL_MS else HIT_TTL_MS
        return System.currentTimeMillis() - queriedAt > ttl
    }

    fun toNews(app: InstalledApp): NewsItem {
        val versionLabel = listOfNotNull(app.title, version).joinToString(" ")
        return NewsItem(
            id = app.id,
            kind = PlayStoreClient.classify(whatsNew),
            body = PlayStoreClient.cardBody(whatsNew),
            date = updatedDisplay ?: "",
            version = versionLabel,
            gameId = app.id,
            gameTitle = app.title,
            imageUrl = heroUrl ?: backgroundUrl,
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("whats_new", whatsNew)
        put("current_version", version)
        put("updated_display", updatedDisplay)
        put("updated_millis", updatedMillis)
        put("background", backgroundUrl)
        put("hero", heroUrl ?: "")
        put("queried_at", queriedAt)
        put("missing", missing)
    }

    companion object {
        private const val HIT_TTL_MS = 12L * 60 * 60 * 1000
        private const val MISS_TTL_MS = 24L * 60 * 60 * 1000

        fun fromJson(json: JSONObject) = CachedListing(
            title = json.optString("title").ifBlank { null },
            whatsNew = json.optString("whats_new").ifBlank { null },
            version = json.optString("current_version").ifBlank { null },
            updatedDisplay = json.optString("updated_display").ifBlank { null },
            updatedMillis = json.optLong("updated_millis").takeIf { it > 0L },
            backgroundUrl = json.optString("background").ifBlank { null },
            heroUrl = json.optString("hero").ifBlank { null },
            queriedAt = json.optLong("queried_at"),
            missing = json.optBoolean("missing"),
            hasHeroField = json.has("hero"),
        )
    }
}

private fun PlayListing?.toCached(): CachedListing {
    val now = System.currentTimeMillis()
    if (this == null) {
        return CachedListing(null, null, null, null, null, null, null, now, missing = true)
    }
    return CachedListing(
        title = title,
        whatsNew = whatsNew,
        version = version,
        updatedDisplay = updatedDisplay,
        updatedMillis = updatedMillis,
        backgroundUrl = backgroundUrl,
        heroUrl = heroUrl,
        queriedAt = now,
        missing = title.isNullOrBlank() && version.isNullOrBlank() && updatedDisplay.isNullOrBlank(),
    )
}

internal fun isPlaySkipped(packageName: String): Boolean {
    return packageName.startsWith("com.android.") ||
        packageName.startsWith("com.google.android.") ||
        packageName.startsWith("org.lineageos.") ||
        packageName.startsWith("org.blissroms.") ||
        packageName.startsWith("org.blissos.") ||
        packageName.startsWith("app.gamenative.stub") ||
        packageName == "app.gamenative.stubinstaller" ||
        packageName.startsWith("com.bass.")
}
