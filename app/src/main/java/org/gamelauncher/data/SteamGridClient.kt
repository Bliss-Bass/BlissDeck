package org.gamelauncher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.min

enum class ArtSlot { Cover, Hero, Icon }

data class ArtCandidate(
    val id: String,
    val url: String,
    val thumbUrl: String,
    val width: Int = 0,
    val height: Int = 0,
    val source: String = "SteamGridDB",
) {
    val isAppIcon: Boolean get() = url == APP_ICON

    companion object {
        const val APP_ICON = "__app_icon__"
    }
}

internal data class SteamGridHit(
    val id: String,
    val name: String,
    val verified: Boolean,
)

internal data class SteamGridArt(
    val coverUrl: String?,
    val heroUrl: String?,
    val iconUrl: String?,
)

internal class SteamGridClient {
    fun gameIdForSteamApp(apiKey: String, steamAppId: String): String? {
        if (steamAppId.isBlank()) return null
        val json = get(apiKey, "$BASE/games/steam/$steamAppId") ?: return null
        json.optJSONObject("data")?.let { data ->
            val id = data.optInt("id", -1)
            return id.takeIf { it > 0 }?.toString()
        }
        val data = json.optJSONArray("data") ?: return null
        val id = data.optJSONObject(0)?.optInt("id", -1) ?: return null
        return id.takeIf { it > 0 }?.toString()
    }

    fun search(apiKey: String, title: String): List<SteamGridHit> {
        val term = URLEncoder.encode(title.trim(), Charsets.UTF_8.name()).replace("+", "%20")
        if (term.isBlank()) return emptyList()
        val json = get(apiKey, "$BASE/search/autocomplete/$term") ?: return emptyList()
        val data = json.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optInt("id", -1)
                val name = item.optString("name")
                if (id <= 0 || name.isBlank()) continue
                add(SteamGridHit(id.toString(), name, item.optBoolean("verified")))
            }
        }
    }

    fun artwork(apiKey: String, gameId: String, tallCover: Boolean): SteamGridArt {
        val coverDims = if (tallCover) TALL_GRIDS else WIDE_GRIDS
        return SteamGridArt(
            coverUrl = assets(apiKey, "grids", gameId, coverDims).firstOrNull()?.url,
            heroUrl = assets(apiKey, "heroes", gameId, HEROES).firstOrNull()?.url,
            iconUrl = assets(apiKey, "icons", gameId, dimensions = null).firstOrNull()?.url,
        )
    }

    fun list(apiKey: String, gameId: String, slot: ArtSlot): List<ArtCandidate> = when (slot) {
        ArtSlot.Cover -> (assets(apiKey, "grids", gameId, TALL_GRIDS) + assets(apiKey, "grids", gameId, WIDE_GRIDS))
            .distinctBy { it.url }
        ArtSlot.Hero -> assets(apiKey, "heroes", gameId, HEROES)
        ArtSlot.Icon -> assets(apiKey, "icons", gameId, dimensions = null)
    }

    private fun assets(
        apiKey: String,
        kind: String,
        gameId: String,
        dimensions: String?,
    ): List<ArtCandidate> {
        val query = buildString {
            append("$BASE/$kind/game/$gameId?types=static&nsfw=false")
            if (!dimensions.isNullOrBlank()) append("&dimensions=").append(dimensions)
        }
        return parseAssets(get(apiKey, query))
    }

    private fun parseAssets(json: JSONObject?): List<ArtCandidate> {
        val data = json?.optJSONArray("data") ?: return emptyList()
        return buildList {
            val n = min(data.length(), 24)
            for (i in 0 until n) {
                val item = data.optJSONObject(i) ?: continue
                val url = item.optString("url")
                if (url.isBlank()) continue
                val id = item.optInt("id", i)
                add(
                    ArtCandidate(
                        id = id.toString(),
                        url = url,
                        thumbUrl = item.optString("thumb").ifBlank { url },
                        width = item.optInt("width"),
                        height = item.optInt("height"),
                    ),
                )
            }
        }
    }

    private fun get(apiKey: String, url: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "BlissDeck/0.1")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 401) throw SteamGridAuthException()
            if (code !in 200..299) return null
            JSONObject(body).takeIf { it.optBoolean("success", true) }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE = "https://www.steamgriddb.com/api/v2"
        const val TALL_GRIDS = "600x900,342x482,660x930"
        const val WIDE_GRIDS = "920x430,460x215"
        const val HEROES = "1920x620,1600x650,3840x1240"
    }
}

internal class SteamGridAuthException : RuntimeException("SteamGridDB API key rejected")
