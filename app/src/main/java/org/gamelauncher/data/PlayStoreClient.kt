package org.gamelauncher.data

import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

internal data class PlayListing(
    val title: String?,
    val whatsNew: String?,
    val version: String?,
    val updatedDisplay: String?,
    val updatedMillis: Long?,
    val backgroundUrl: String?,
    val heroUrl: String?,
    val description: String?,
    val developer: String?,
    val category: String?,
    val rating: Float,
    val screenshots: List<String>,
)

internal class PlayStoreClient {
    fun fetch(packageName: String): PlayListing? {
        val url = "$DETAILS?id=$packageName&hl=en&gl=US"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val html = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299 || html.isBlank()) null else parse(html)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val DETAILS = "https://play.google.com/store/apps/details"
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val CLUSTER = Regex(
            """\[\[\["([^"]+)"\]\],\[\[\[36\]\],\[\[\[\d+,"[^"]+"\]\]\]\]\],null,null,null,\[null,\[null,"((?:\\.|[^"\\])*)"\]\],\[\["([^"]+)"""",
        )
        private val VERSION = Regex("""\[\[\["(\d[^"]*)"\]\],\[\[\[36\]\]""")
        private val UPDATED_ON = Regex("""Updated on</div><div class="[^"]+">([^<]+)""")
        private val WHATS_NEW_DIV = Regex("""itemprop="description">([^<].+?)</div>""")
        private val OG_TITLE = Regex("""property="og:title" content="([^"]+)"""")
        private val OG_IMAGE = Regex("""property="og:image" content="([^"]+)"""")
        private val PLAY_LH = Regex(
            """https://play-lh\.googleusercontent\.com/[A-Za-z0-9_\-]+(?:=[A-Za-z0-9_=.,\-]*)?""",
        )
        private val PLAY_SIZE = Regex("""w(\d+)-h(\d+)""")

        fun parse(html: String): PlayListing? {
            val cluster = CLUSTER.find(html)
            val version = cluster?.groupValues?.get(1) ?: VERSION.find(html)?.groupValues?.get(1)
            val rawWhats = cluster?.groupValues?.getOrNull(2)
                ?.let { decodeJsonString(it) }
                ?: WHATS_NEW_DIV.find(html)?.groupValues?.get(1)
            val rawDate = cluster?.groupValues?.getOrNull(3)
                ?: UPDATED_ON.find(html)?.groupValues?.get(1)?.trim()
            val title = OG_TITLE.find(html)?.groupValues?.get(1)
                ?.substringBefore(" - Apps on Google Play")
                ?.substringBefore(" - Apps on Google")
                ?.trim()
            val background = OG_IMAGE.find(html)?.groupValues?.get(1)
                ?.replace("&amp;", "&")
                ?.let { fullSizePlayImage(it) }
            val hero = pickLandscapeHero(html) ?: background?.takeIf { looksWide(it, html) }
            val whatsNew = htmlToPlain(rawWhats.orEmpty()).ifBlank { null }
            val description = ogDescription(html) ?: whatsNew
            if (title.isNullOrBlank() && version.isNullOrBlank() && rawDate.isNullOrBlank()) {
                return null
            }
            val parsedDate = rawDate?.let { parsePlayDate(it) }
            return PlayListing(
                title = title,
                whatsNew = whatsNew,
                version = version,
                updatedDisplay = parsedDate?.second ?: rawDate,
                updatedMillis = parsedDate?.first,
                backgroundUrl = hero ?: background,
                heroUrl = hero,
                description = description,
                developer = playDeveloper(html),
                category = playCategory(html),
                rating = playRating(html),
                screenshots = landscapeShots(html),
            )
        }

        internal fun pickLandscapeHero(html: String): String? {
            val seen = LinkedHashSet<String>()
            var bestToken: String? = null
            for (match in PLAY_LH.findAll(html)) {
                val url = match.value.replace("&amp;", "&").replace("\\u003d", "=")
                val token = url.substringBefore("=")
                if (!seen.add(token)) continue
                val size = PLAY_SIZE.find(url) ?: continue
                val width = size.groupValues[1].toInt()
                val height = size.groupValues[2].toInt()
                if (width < 400 || height < 200) continue
                if (width < height * 1.2f) continue
                bestToken = token
                break
            }
            return bestToken?.let { fullSizePlayImage(it) }
        }

        internal fun landscapeShots(html: String, limit: Int = 6): List<String> {
            val seen = LinkedHashSet<String>()
            for (match in PLAY_LH.findAll(html)) {
                val url = match.value.replace("&amp;", "&").replace("\\u003d", "=")
                val token = url.substringBefore("=")
                val size = PLAY_SIZE.find(url) ?: continue
                val width = size.groupValues[1].toInt()
                val height = size.groupValues[2].toInt()
                if (width < 400 || height < 200 || width < height * 1.15f) continue
                if (seen.add(fullSizePlayImage(token)) && seen.size >= limit) break
            }
            return seen.toList()
        }

        private fun ogDescription(html: String): String? {
            val raw = Regex("""property="og:description" content="([^"]+)"""")
                .find(html)?.groupValues?.get(1) ?: return null
            return htmlToPlain(raw.replace("&amp;", "&")).takeIf { it.isNotBlank() }
        }

        private fun playDeveloper(html: String): String? {
            val named = Regex("""/store/apps/developer\?id=([^"&]+)"[^>]*>([^<]+)""")
                .find(html)
            if (named != null) {
                val label = htmlToPlain(named.groupValues[2]).ifBlank {
                    named.groupValues[1].replace("+", " ")
                }
                return label.takeIf { it.isNotBlank() }
            }
            val idOnly = Regex("""/store/apps/dev(?:eloper)?\?id=([^"&]+)""")
                .find(html)?.groupValues?.get(1) ?: return null
            return idOnly.replace("+", " ").takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } }
        }

        private fun playCategory(html: String): String? {
            val match = Regex("""/store/apps/category/([A-Z0-9_]+)"[^>]*>([^<]+)""")
                .find(html) ?: return null
            val label = htmlToPlain(match.groupValues[2])
            return label.ifBlank { match.groupValues[1].replace('_', ' ').lowercase(Locale.US) }
        }

        private fun playRating(html: String): Float {
            val labeled = Regex("""Rated ([0-9.]+) stars""").find(html)?.groupValues?.get(1)
            val json = Regex(""""starRating":\{"value":([0-9.]+)""").find(html)?.groupValues?.get(1)
            val item = Regex("""itemprop="ratingValue" content="([0-9.]+)"""").find(html)?.groupValues?.get(1)
            val raw = labeled ?: json ?: item ?: return 0f
            return raw.toFloatOrNull()?.coerceIn(0f, 5f) ?: 0f
        }

        private fun looksWide(url: String, html: String): Boolean {
            val token = url.substringBefore("=")
            return PLAY_LH.findAll(html).any { match ->
                val candidate = match.value.replace("&amp;", "&")
                if (!candidate.startsWith(token)) return@any false
                val size = PLAY_SIZE.find(candidate) ?: return@any false
                size.groupValues[1].toInt() >= size.groupValues[2].toInt() * 1.2f
            }
        }

        private fun fullSizePlayImage(url: String): String {
            val token = url.substringBefore("=").substringBefore("\\")
            return "$token=s0"
        }

        internal fun classify(whatsNew: String?): String {
            val first = whatsNew.orEmpty().lineSequence().firstOrNull().orEmpty().trim()
            val lower = first.lowercase(Locale.US)
            return when {
                lower.startsWith("minor bug") ||
                    lower.startsWith("bug") ||
                    lower.startsWith("hotfix") ||
                    Regex("""^(minor )?bugs?\b""").containsMatchIn(lower) -> "BUGFIX"
                "major update" in lower || lower.startsWith("major") -> "MAJOR UPDATE"
                else -> "REGULAR UPDATE"
            }
        }

        internal fun cardBody(whatsNew: String?): String {
            if (whatsNew.isNullOrBlank()) return ""
            return whatsNew.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("\n")
                .take(220)
        }

        private fun parsePlayDate(raw: String): Pair<Long, String>? {
            val patterns = arrayOf("MMM d, yyyy", "d MMM yyyy")
            for (pattern in patterns) {
                val parsed = runCatching {
                    SimpleDateFormat(pattern, Locale.US).parse(raw.trim())
                }.getOrNull() ?: continue
                val display = SimpleDateFormat("d MMMM yyyy", Locale.US).format(parsed)
                return parsed.time to display
            }
            return null
        }

        private fun decodeJsonString(raw: String): String {
            return try {
                JSONTokener("\"$raw\"").nextValue() as String
            } catch (_: Exception) {
                raw.replace("\\u003c", "<")
                    .replace("\\u003e", ">")
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
            }
        }

        private fun htmlToPlain(source: String): String {
            return source
                .replace(Regex("(?i)<br\\s*/?>"), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace(Regex("[ \\t]+"), " ")
                .replace(Regex("\n{2,}"), "\n")
                .trim()
        }
    }
}
