package org.gamelauncher.ui.chrome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.os.BatteryManager

data class DeviceStatus(
    val batteryPercent: Int,
    val charging: Boolean,
    val wifiConnected: Boolean,
)

@Composable
fun rememberDeviceStatus(): DeviceStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(initialStatus(context)) }
    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                status = status.copy(
                    batteryPercent = batteryPercent(intent),
                    charging = isCharging(intent),
                )
            }
        }
        val sticky = if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
        }
        if (sticky != null) {
            status = status.copy(
                batteryPercent = batteryPercent(sticky),
                charging = isCharging(sticky),
            )
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                status = status.copy(wifiConnected = wifiConnected(cm))
            }

            override fun onLost(network: Network) {
                status = status.copy(wifiConnected = wifiConnected(cm))
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                status = status.copy(wifiConnected = wifiConnected(cm))
            }
        }
        cm.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        status = status.copy(wifiConnected = wifiConnected(cm))
        onDispose {
            runCatching { context.unregisterReceiver(batteryReceiver) }
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
    return status
}

fun expandNotificationShade(context: Context) {
    val expanded = invokeStatusBar(context, "expandNotificationsPanel") ||
        invokeStatusBar(context, "expandSettingsPanel")
    if (expanded) return
    val fallback = if (Build.VERSION.SDK_INT >= 29) {
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
        Intent(Settings.ACTION_WIFI_SETTINGS)
    }
    runCatching {
        context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun invokeStatusBar(context: Context, method: String): Boolean {
    return runCatching {
        val service = context.getSystemService("statusbar") ?: return false
        service.javaClass.getMethod(method).invoke(service)
        true
    }.getOrDefault(false)
}

private fun initialStatus(context: Context): DeviceStatus {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return DeviceStatus(100, charging = false, wifiConnected = wifiConnected(cm))
}

private fun batteryPercent(intent: Intent): Int {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    return ((level.coerceAtLeast(0) * 100f) / scale).toInt().coerceIn(0, 100)
}

private fun isCharging(intent: Intent): Boolean {
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}

private fun wifiConnected(cm: ConnectivityManager): Boolean {
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}
