package org.gamelauncher.data

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppRunState { Stopped, Running, Closing }

data class PresenceSnapshot(
    val connected: Boolean = false,
    val open: List<RunningApp> = emptyList(),
    val closing: Set<String> = emptySet(),
    val starting: Set<String> = emptySet(),
)

object AppPresence {
    private val main = Handler(Looper.getMainLooper())
    private val closeTimeouts = HashMap<String, Runnable>()
    private val startTimeouts = HashMap<String, Runnable>()
    private val _snapshot = MutableStateFlow(PresenceSnapshot())
    val snapshot: StateFlow<PresenceSnapshot> = _snapshot

    val connected: Boolean get() = _snapshot.value.connected

    fun state(packageName: String, snapshot: PresenceSnapshot = _snapshot.value): AppRunState {
        if (packageName in snapshot.closing) return AppRunState.Closing
        if (snapshot.open.any { it.packageName == packageName } || packageName in snapshot.starting) {
            return AppRunState.Running
        }
        return AppRunState.Stopped
    }

    fun setConnected(value: Boolean) {
        main.post {
            if (!value) {
                closeTimeouts.values.forEach { main.removeCallbacks(it) }
                startTimeouts.values.forEach { main.removeCallbacks(it) }
                closeTimeouts.clear()
                startTimeouts.clear()
                _snapshot.value = PresenceSnapshot(connected = false)
            } else {
                _snapshot.value = _snapshot.value.copy(connected = true)
            }
        }
    }

    fun publishOpen(apps: List<RunningApp>) {
        main.post {
            val pkgs = apps.map { it.packageName }.toSet()
            val current = _snapshot.value
            current.closing.filterNot { it in pkgs }.forEach { cancelCloseTimeout(it) }
            current.starting.filter { it in pkgs }.forEach { cancelStartTimeout(it) }
            _snapshot.value = current.copy(
                connected = true,
                open = apps,
                starting = current.starting.filterNot { it in pkgs }.toSet(),
                closing = current.closing.intersect(pkgs),
            )
        }
    }

    fun markStarting(packageName: String) {
        if (packageName.isBlank()) return
        main.post {
            val current = _snapshot.value
            _snapshot.value = current.copy(
                starting = current.starting + packageName,
                closing = current.closing - packageName,
            )
            cancelStartTimeout(packageName)
            val timeout = Runnable {
                val now = _snapshot.value
                _snapshot.value = now.copy(starting = now.starting - packageName)
                startTimeouts.remove(packageName)
            }
            startTimeouts[packageName] = timeout
            main.postDelayed(timeout, START_TIMEOUT_MS)
        }
    }

    fun markClosing(packageName: String) {
        if (packageName.isBlank()) return
        main.post {
            val current = _snapshot.value
            _snapshot.value = current.copy(
                closing = current.closing + packageName,
                starting = current.starting - packageName,
            )
            cancelCloseTimeout(packageName)
            val timeout = Runnable {
                val now = _snapshot.value
                _snapshot.value = now.copy(closing = now.closing - packageName)
                closeTimeouts.remove(packageName)
            }
            closeTimeouts[packageName] = timeout
            main.postDelayed(timeout, CLOSE_TIMEOUT_MS)
        }
    }

    private fun cancelCloseTimeout(packageName: String) {
        closeTimeouts.remove(packageName)?.let { main.removeCallbacks(it) }
    }

    private fun cancelStartTimeout(packageName: String) {
        startTimeouts.remove(packageName)?.let { main.removeCallbacks(it) }
    }

    private const val CLOSE_TIMEOUT_MS = 6_000L
    private const val START_TIMEOUT_MS = 8_000L
}
