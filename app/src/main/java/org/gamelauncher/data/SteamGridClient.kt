package org.gamelauncher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class SteamGridHit(
    val id: String,
    val name: String,
    val verified: Boolean,
)

internal data class SteamGridArt(
    val coverUrl: String?,
    val heroUrl: String?,
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

    fun artwork(apiKey: String, gameId: String): SteamGridArt {
        val cover = firstUrl(
            get(
                apiKey,
                "$BASE/grids/game/$gameId?dimensions=600x900,342x482,660x930&types=static&nsfw=false",
            ),
        )
        val hero = firstUrl(
            get(
                apiKey,
                "$BASE/heroes/game/$gameId?dimensions=1920x620,1600x650&types=static&nsfw=false",
            ),
        )
        return SteamGridArt(cover, hero)
    }

    private fun firstUrl(json: JSONObject?): String? {
        val data = json?.optJSONArray("data") ?: return null
        if (data.length() == 0) return null
        return data.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
    }

    private fun get(apiKey: String, url: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "GameLauncher/0.1")
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

    companion object {
        private const val BASE = "https://www.steamgriddb.com/api/v2"
    }
}

internal class SteamGridAuthException : RuntimeException("SteamGridDB API key rejected")
