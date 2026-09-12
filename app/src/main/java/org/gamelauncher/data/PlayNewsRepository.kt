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
    private val steam = SteamStoreClient()
    private val cacheDir = File(appContext.cacheDir, "playnews").also { it.mkdirs() }
    private val steamDir = File(appContext.cacheDir, "steamnews").also { it.mkdirs() }
    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _loading = MutableStateFlow(false)

    val news: StateFlow<List<NewsItem>> = _news
    val loading: StateFlow<Boolean> = _loading

    suspend fun refresh(snapshot: LibrarySnapshot, limit: Int = 16) = withContext(Dispatchers.IO) {
        _loading.value = true
        try {
            val cap = limit.coerceIn(8, 32)
            val cached = assemble(snapshot, readCached(snapshot), readSteamCached(snapshot), cap)
            if (cached.isNotEmpty()) _news.value = cached

            val fetched = fetchStale(snapshot)
            val steamNews = fetchSteamStale(snapshot)
            _news.value = assemble(snapshot, fetched, steamNews, cap)
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
                        val previous = known[app.packageName]
                        val cached = listing.toCached(previous)
                        writeCache(app.packageName, cached)
                        Log.d(TAG, "listing ${app.packageName} version=${cached.version} date=${cached.updatedDisplay} hero=${!cached.heroUrl.isNullOrBlank()}")
                        known[app.packageName] = cached
                    }
                }
            }.awaitAll()
        }
        return known
    }

    private suspend fun fetchSteamStale(snapshot: LibrarySnapshot): Map<String, List<SteamNewsHit>> {
        val known = readSteamCached(snapshot).toMutableMap()
        val stubs = snapshot.installed.filter { SteamNative.steamAppId(it.packageName) != null }
        val gate = Semaphore(3)
        coroutineScope {
            stubs.map { app ->
                async {
                    gate.withPermit {
                        val appId = SteamNative.steamAppId(app.packageName) ?: return@withPermit
                        val file = steamFile(appId)
                        val fresh = file.isFile && System.currentTimeMillis() - file.lastModified() < STEAM_TTL_MS
                        if (fresh && known.containsKey(app.packageName)) return@withPermit
                        val items = runCatching { steam.news(appId, 8) }.getOrDefault(emptyList())
                        writeSteamCache(appId, items)
                        known[app.packageName] = items
                    }
                }
            }.awaitAll()
        }
        return known
    }

    private fun assemble(
        snapshot: LibrarySnapshot,
        listings: Map<String, CachedListing>,
        steamNews: Map<String, List<SteamNewsHit>>,
        limit: Int,
    ): List<NewsItem> {
        val now = System.currentTimeMillis()
        val games = snapshot.installed.filter { it.isGame }.map { it.id }.toSet()
        val playItems = snapshot.installed.flatMap { app ->
            val listing = listings[app.packageName] ?: return@flatMap emptyList()
            if (listing.missing) return@flatMap emptyList()
            if (!include(app, listing, now)) return@flatMap emptyList()
            listing.toNewsItems(app)
        }
        val steamItems = snapshot.installed.flatMap { app ->
            val hits = steamNews[app.packageName].orEmpty()
            hits.map { it.toNews(app) }
        }
        return (playItems + steamItems).sortedWith(
            compareByDescending<NewsItem> { it.gameId in games }
                .thenByDescending { it.sortMillis },
        ).distinctBy { it.id }.take(limit)
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
    private fun steamFile(appId: String) = File(steamDir, "$appId.json")

    private fun readSteamCached(snapshot: LibrarySnapshot): Map<String, List<SteamNewsHit>> {
        return snapshot.installed.mapNotNull { app ->
            val appId = SteamNative.steamAppId(app.packageName) ?: return@mapNotNull null
            val file = steamFile(appId)
            if (!file.isFile) return@mapNotNull null
            val items = runCatching { steamHitsFromJson(file.readText()) }.getOrNull() ?: return@mapNotNull null
            app.packageName to items
        }.toMap()
    }

    private fun writeSteamCache(appId: String, items: List<SteamNewsHit>) {
        val json = org.json.JSONArray()
        items.forEach { hit ->
            json.put(
                JSONObject().apply {
                    put("gid", hit.gid)
                    put("title", hit.title)
                    put("body", hit.body)
                    put("date", hit.dateMillis)
                    put("date_display", hit.dateDisplay)
                },
            )
        }
        runCatching { steamFile(appId).writeText(json.toString()) }
    }

    fun heroUrl(packageName: String): String? {
        val file = cacheFile(packageName)
        if (!file.isFile) return null
        return runCatching {
            JSONObject(file.readText()).optString("hero").ifBlank { null }
        }.getOrNull()
    }

    fun details(packageName: String): TitleDetails? {
        val cached = runCatching {
            val file = cacheFile(packageName)
            if (!file.isFile) null else CachedListing.fromJson(JSONObject(file.readText()))
        }.getOrNull()
        if (cached != null && !cached.stale() && cached.hasDetailsField && !cached.missing) {
            return cached.toDetails()
        }
        val next = client.fetch(packageName).toCached(cached)
        writeCache(packageName, next)
        return next.takeIf { !it.missing }?.toDetails()
    }

    companion object {
        private const val TAG = "PlayNews"
        private const val OTHER_APP_WINDOW_MS = 120L * 24 * 60 * 60 * 1000
        private const val STEAM_TTL_MS = 12L * 60 * 60 * 1000
    }
}

internal data class CachedUpdate(
    val version: String?,
    val whatsNew: String?,
    val updatedDisplay: String?,
    val updatedMillis: Long?,
)

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
    val description: String? = null,
    val developer: String? = null,
    val category: String? = null,
    val rating: Float = 0f,
    val screenshots: List<String> = emptyList(),
    val hasDetailsField: Boolean = true,
    val history: List<CachedUpdate> = emptyList(),
) {
    fun stale(): Boolean {
        if (!missing && !hasHeroField) return true
        if (!missing && !hasDetailsField) return true
        val ttl = if (missing) MISS_TTL_MS else HIT_TTL_MS
        return System.currentTimeMillis() - queriedAt > ttl
    }

    fun toNewsItems(app: InstalledApp): List<NewsItem> {
        val current = toNews(app, version, whatsNew, updatedDisplay, updatedMillis, suffix = "current")
            ?: return emptyList()
        val older = history.mapNotNull { entry ->
            toNews(app, entry.version, entry.whatsNew, entry.updatedDisplay, entry.updatedMillis, suffix = entry.version ?: entry.updatedMillis.toString())
        }
        return listOf(current) + older
    }

    private fun toNews(
        app: InstalledApp,
        version: String?,
        whatsNew: String?,
        updatedDisplay: String?,
        updatedMillis: Long?,
        suffix: String,
    ): NewsItem? {
        if (version.isNullOrBlank() && whatsNew.isNullOrBlank()) return null
        val versionLabel = listOfNotNull(app.title, version).joinToString(" ")
        return NewsItem(
            id = "${app.id}.$suffix",
            kind = PlayStoreClient.classify(whatsNew),
            body = PlayStoreClient.cardBody(whatsNew),
            date = updatedDisplay.orEmpty(),
            version = versionLabel,
            gameId = app.id,
            gameTitle = app.title,
            imageUrl = heroUrl ?: backgroundUrl,
            sortMillis = updatedMillis ?: 0L,
        )
    }

    fun toDetails(): TitleDetails = TitleDetails(
        summary = description ?: whatsNew.orEmpty(),
        developer = developer.orEmpty(),
        publisher = developer.orEmpty(),
        category = category.orEmpty(),
        releaseDate = updatedDisplay.orEmpty(),
        players = "",
        controller = "",
        rating = rating,
        screenshots = screenshots,
    )

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
        put("description", description)
        put("developer", developer)
        put("category", category)
        put("rating", rating.toDouble())
        put("screenshots", org.json.JSONArray(screenshots))
        put(
            "history",
            org.json.JSONArray().also { array ->
                history.forEach { entry ->
                    array.put(
                        JSONObject().apply {
                            put("version", entry.version)
                            put("whats_new", entry.whatsNew)
                            put("updated_display", entry.updatedDisplay)
                            put("updated_millis", entry.updatedMillis)
                        },
                    )
                }
            },
        )
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
            description = json.optString("description").ifBlank { null },
            developer = json.optString("developer").ifBlank { null },
            category = json.optString("category").ifBlank { null },
            rating = json.optDouble("rating").toFloat(),
            screenshots = buildList {
                val array = json.optJSONArray("screenshots") ?: return@buildList
                for (i in 0 until array.length()) {
                    array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                }
            },
            hasDetailsField = json.has("description"),
            history = buildList {
                val array = json.optJSONArray("history") ?: return@buildList
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(
                        CachedUpdate(
                            version = item.optString("version").ifBlank { null },
                            whatsNew = item.optString("whats_new").ifBlank { null },
                            updatedDisplay = item.optString("updated_display").ifBlank { null },
                            updatedMillis = item.optLong("updated_millis").takeIf { it > 0L },
                        ),
                    )
                }
            },
        )
    }
}

private fun PlayListing?.toCached(previous: CachedListing? = null): CachedListing {
    val now = System.currentTimeMillis()
    if (this == null) {
        return previous?.copy(queriedAt = now, missing = true)
            ?: CachedListing(null, null, null, null, null, null, null, now, missing = true)
    }
    val changed = previous != null &&
        !previous.missing &&
        (previous.version != version || previous.whatsNew != whatsNew) &&
        !previous.whatsNew.isNullOrBlank()
    val history = buildList {
        if (changed) {
            add(
                CachedUpdate(
                    previous.version,
                    previous.whatsNew,
                    previous.updatedDisplay,
                    previous.updatedMillis,
                ),
            )
        }
        previous?.history.orEmpty().forEach { add(it) }
    }.distinctBy { it.version to it.whatsNew }.take(8)
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
        description = description,
        developer = developer,
        category = category,
        rating = rating,
        screenshots = screenshots,
        hasDetailsField = true,
        history = history,
    )
}

private fun steamHitsFromJson(raw: String): List<SteamNewsHit> {
    val array = org.json.JSONArray(raw)
    return buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            add(
                SteamNewsHit(
                    gid = item.optString("gid").ifBlank { i.toString() },
                    title = item.optString("title"),
                    body = SteamStoreClient.cleanNews(item.optString("body")),
                    dateMillis = item.optLong("date"),
                    dateDisplay = item.optString("date_display"),
                ),
            )
        }
    }
}

private fun SteamNewsHit.toNews(app: InstalledApp): NewsItem {
    val kind = when {
        title.contains("fix", ignoreCase = true) || title.contains("hotfix", ignoreCase = true) -> "BUGFIX"
        title.contains("update", ignoreCase = true) || title.contains("patch", ignoreCase = true) -> "UPDATE"
        else -> "NEWS"
    }
    return NewsItem(
        id = "${app.id}.steam.$gid",
        kind = kind,
        body = body.ifBlank { title },
        date = dateDisplay,
        version = title,
        gameId = app.id,
        gameTitle = app.title,
        imageUrl = SteamNative.steamAppId(app.packageName)?.let(SteamNative::cdnHero),
        sortMillis = dateMillis,
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
