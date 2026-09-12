package org.gamelauncher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationCountService : NotificationListenerService() {
    override fun onListenerConnected() {
        connected.value = true
        publish()
    }

    override fun onListenerDisconnected() {
        connected.value = false
        count.value = 0
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publish()
    }

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
        count.value = active.count { countable(it, packageName) }
    }

    companion object {
        private val count = MutableStateFlow(0)
        private val connected = MutableStateFlow(false)

        val unread: StateFlow<Int> = count
        val listening: StateFlow<Boolean> = connected

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            val component = ComponentName(context, NotificationCountService::class.java)
            return enabled.split(':').any {
                it.equals(component.flattenToString(), ignoreCase = true) ||
                    it.equals(component.flattenToShortString(), ignoreCase = true)
            }
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            if (Build.VERSION.SDK_INT >= 30) {
                intent.putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, NotificationCountService::class.java).flattenToString(),
                )
            }
            runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

private fun countable(sbn: StatusBarNotification, self: String): Boolean {
    if (sbn.packageName == self) return false
    val flags = sbn.notification.flags
    if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
    return true
}

data class NotificationStatus(
    val enabled: Boolean,
    val count: Int,
)

@Composable
fun rememberNotificationStatus(): NotificationStatus {
    val context = LocalContext.current
    val resume = rememberResumeTick()
    val enabled = remember(resume) { NotificationCountService.isEnabled(context) }
    val count by NotificationCountService.unread.collectAsState()
    return NotificationStatus(enabled = enabled, count = if (enabled) count else 0)
}
