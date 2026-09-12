package org.gamelauncher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class SteamStoreClient {
    fun details(appId: String): TitleDetails? {
        if (appId.isBlank()) return null
        val root = get("$STORE/appdetails?appids=$appId&l=english&cc=us") ?: return null
        val wrapper = root.optJSONObject(appId) ?: return null
        if (!wrapper.optBoolean("success")) return null
        val data = wrapper.optJSONObject("data") ?: return null
        val developers = stringList(data.optJSONArray("developers"))
        val publishers = stringList(data.optJSONArray("publishers"))
        val genres = stringList(data.optJSONArray("genres"), key = "description")
        val categories = stringList(data.optJSONArray("categories"), key = "description")
        val screenshots = buildList {
            val shots = data.optJSONArray("screenshots") ?: return@buildList
            for (i in 0 until shots.length()) {
                val url = shots.optJSONObject(i)?.optString("path_full").orEmpty()
                if (url.isNotBlank()) add(url)
                if (size >= 6) break
            }
        }
        val release = data.optJSONObject("release_date")?.optString("date").orEmpty()
        val players = categories.firstOrNull {
            it.contains("player", ignoreCase = true) || it.contains("co-op", ignoreCase = true)
        } ?: "Single-Player"
        val controller = categories.firstOrNull { it.contains("controller", ignoreCase = true) }
            ?: "Unknown"
        return TitleDetails(
            summary = htmlToPlain(data.optString("short_description")),
            developer = developers.joinToString(", "),
            publisher = publishers.joinToString(", ").ifBlank { developers.joinToString(", ") },
            category = genres.joinToString(", ").ifBlank { "Games" },
            releaseDate = release.ifBlank { "—" },
            players = players,
            controller = controller,
            rating = reviewStars(appId),
            screenshots = screenshots,
        )
    }

    private fun reviewStars(appId: String): Float {
        val json = get("$STORE/appreviews/$appId?json=1&purchase_type=all&num_per_page=0&language=all")
        val summary = json?.optJSONObject("query_summary") ?: return 0f
        val positive = summary.optInt("total_positive")
        val negative = summary.optInt("total_negative")
        val total = positive + negative
        if (total <= 0) return 0f
        return ((5.0 * positive) / total).toFloat().coerceIn(0f, 5f)
    }

    private fun get(url: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299 || body.isBlank()) null else JSONObject(body)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun stringList(array: org.json.JSONArray?, key: String? = null): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = if (key == null) array.optString(i)
                else array.optJSONObject(i)?.optString(key).orEmpty()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun htmlToPlain(source: String): String =
        source.replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    companion object {
        private const val STORE = "https://store.steampowered.com"
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
