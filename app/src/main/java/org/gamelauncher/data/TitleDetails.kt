package org.gamelauncher.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class TitleDetails(
    val summary: String = "",
    val developer: String = "",
    val publisher: String = "",
    val category: String = "",
    val releaseDate: String = "",
    val players: String = "",
    val controller: String = "",
    val rating: Float = 0f,
    val screenshots: List<String> = emptyList(),
) {
    val hasText: Boolean
        get() = summary.isNotBlank() || developer.isNotBlank()
}

class TitleDetailsRepository(
    context: Context,
    private val playNews: PlayNewsRepository,
) {
    private val cacheDir = File(context.applicationContext.cacheDir, "title-details").also { it.mkdirs() }
    private val steam = SteamStoreClient()
    private val memory = ConcurrentHashMap<String, TitleDetails>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> = _epoch

    fun peek(packageName: String): TitleDetails? = memory[packageName]

    suspend fun resolve(packageName: String, steamAppId: String?): TitleDetails =
        withContext(Dispatchers.IO) {
            val key = steamAppId?.let { "steam:$it" } ?: "play:$packageName"
            val mutex = locks.getOrPut(key) { Mutex() }
            mutex.withLock {
                memory[key]?.let {
                    memory[packageName] = it
                    return@withLock it
                }
                readDisk(key)?.let {
                    memory[key] = it
                    memory[packageName] = it
                    return@withLock it
                }
                val fetched = if (steamAppId != null) {
                    steam.details(steamAppId)
                } else {
                    playNews.details(packageName)
                } ?: TitleDetails()
                memory[key] = fetched
                memory[packageName] = fetched
                writeDisk(key, fetched)
                _epoch.value = _epoch.value + 1
                fetched
            }
        }

    private fun readDisk(key: String): TitleDetails? {
        val file = File(cacheDir, key.replace(':', '_') + ".json")
        if (!file.isFile) return null
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
        val age = System.currentTimeMillis() - json.optLong("queried_at")
        if (age > TTL_MS) return null
        return json.toDetails()
    }

    private fun writeDisk(key: String, details: TitleDetails) {
        val file = File(cacheDir, key.replace(':', '_') + ".json")
        runCatching { file.writeText(details.toJson().put("queried_at", System.currentTimeMillis()).toString()) }
    }

    companion object {
        private const val TTL_MS = 24L * 60 * 60 * 1000
    }
}

val LocalDetails = staticCompositionLocalOf<TitleDetailsRepository> {
    error("TitleDetailsRepository not provided")
}

private fun TitleDetails.toJson() = JSONObject().apply {
    put("summary", summary)
    put("developer", developer)
    put("publisher", publisher)
    put("category", category)
    put("release_date", releaseDate)
    put("players", players)
    put("controller", controller)
    put("rating", rating.toDouble())
    put("screenshots", JSONArray(screenshots))
}

private fun JSONObject.toDetails() = TitleDetails(
    summary = optString("summary"),
    developer = optString("developer"),
    publisher = optString("publisher"),
    category = optString("category"),
    releaseDate = optString("release_date"),
    players = optString("players"),
    controller = optString("controller"),
    rating = optDouble("rating").toFloat(),
    screenshots = buildList {
        val array = optJSONArray("screenshots") ?: return@buildList
        for (i in 0 until array.length()) {
            array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    },
)
