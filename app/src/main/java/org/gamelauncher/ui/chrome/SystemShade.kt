package org.gamelauncher.ui.chrome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

enum class NetworkKind { Offline, Wifi, Ethernet, Cellular }

data class DeviceStatus(
    val batteryPercent: Int,
    val charging: Boolean,
    val network: NetworkKind,
    val wifiLevel: Int = 0,
    val wifiSsid: String? = null,
)

@Composable
fun rememberDeviceStatus(): DeviceStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readStatus(context)) }
    DisposableEffect(context) {
        val app = context.applicationContext
        fun refreshNetwork() {
            status = status.copyNetwork(readNetwork(app))
        }
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
            app.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            app.registerReceiver(
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
        val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshNetwork()
            }
        }
        val wifiFilter = IntentFilter().apply {
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(wifiReceiver, wifiFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(wifiReceiver, wifiFilter)
        }
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshNetwork()
            override fun onLost(network: Network) = refreshNetwork()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refreshNetwork()
        }
        cm.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        refreshNetwork()
        onDispose {
            runCatching { app.unregisterReceiver(batteryReceiver) }
            runCatching { app.unregisterReceiver(wifiReceiver) }
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

private fun readStatus(context: Context): DeviceStatus {
    val app = context.applicationContext
    val bm = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    val charging = if (Build.VERSION.SDK_INT >= 23) bm.isCharging else false
    val network = readNetwork(app)
    return DeviceStatus(
        batteryPercent = percent,
        charging = charging,
        network = network.kind,
        wifiLevel = network.wifiLevel,
        wifiSsid = network.wifiSsid,
    )
}

private data class NetworkSnapshot(
    val kind: NetworkKind,
    val wifiLevel: Int = 0,
    val wifiSsid: String? = null,
)

private fun DeviceStatus.copyNetwork(network: NetworkSnapshot) = copy(
    network = network.kind,
    wifiLevel = network.wifiLevel,
    wifiSsid = network.wifiSsid,
)

private fun readNetwork(context: Context): NetworkSnapshot {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return NetworkSnapshot(NetworkKind.Offline)
    val caps = cm.getNetworkCapabilities(network) ?: return NetworkSnapshot(NetworkKind.Offline)
    val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
            NetworkSnapshot(NetworkKind.Ethernet)
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            val wifi = wifiDetails(context, caps)
            NetworkSnapshot(
                kind = if (online || wifi.second > 0) NetworkKind.Wifi else NetworkKind.Offline,
                wifiLevel = wifi.second,
                wifiSsid = wifi.first,
            )
        }
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && online ->
            NetworkSnapshot(NetworkKind.Cellular)
        else -> NetworkSnapshot(if (online) NetworkKind.Cellular else NetworkKind.Offline)
    }
}

private fun wifiDetails(context: Context, caps: NetworkCapabilities): Pair<String?, Int> {
    val info = if (Build.VERSION.SDK_INT >= 31) {
        caps.transportInfo as? WifiInfo
    } else {
        null
    } ?: run {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wm.connectionInfo
    }
    val rssi = info.rssi
    @Suppress("DEPRECATION")
    val level = WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
    val ssid = info.ssid
        ?.trim('"')
        ?.takeIf { it.isNotBlank() && !it.equals("<unknown ssid>", ignoreCase = true) }
    return ssid to level
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

