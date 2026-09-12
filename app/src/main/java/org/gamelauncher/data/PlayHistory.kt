package org.gamelauncher.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PlayHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("play_history", Context.MODE_PRIVATE)
    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> = _epoch

    fun record(packageName: String) {
        if (packageName.isBlank()) return
        val now = System.currentTimeMillis()
        val json = JSONObject(prefs.getString(packageName, "{}") ?: "{}")
        json.put("last", now)
        json.put("launches", json.optInt("launches") + 1)
        prefs.edit().putString(packageName, json.toString()).apply()
        _epoch.value = _epoch.value + 1
    }

    fun lastPlayedMillis(packageName: String): Long =
        runCatching { JSONObject(prefs.getString(packageName, "{}") ?: "{}").optLong("last") }
            .getOrDefault(0L)

    fun launchCount(packageName: String): Int =
        runCatching { JSONObject(prefs.getString(packageName, "{}") ?: "{}").optInt("launches") }
            .getOrDefault(0)

    fun lastPlayedLabel(packageName: String): String {
        val at = lastPlayedMillis(packageName)
        if (at <= 0L) return "Never"
        val then = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when (val days = ChronoUnit.DAYS.between(then, today)) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2..13 -> "$days days ago"
            else -> then.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
    }

    fun playTimeLabel(packageName: String): String {
        val n = launchCount(packageName)
        return when {
            n <= 0 -> "Play Now!"
            n == 1 -> "1 launch"
            else -> "$n launches"
        }
    }
}

val LocalPlayHistory = staticCompositionLocalOf<PlayHistory> {
    error("PlayHistory not provided")
}
