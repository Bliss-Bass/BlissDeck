package org.gamelauncher.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.gamelauncher.ui.components.DiamondMark
import org.gamelauncher.ui.components.FaceButton
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.navigation.MenuItem
import org.gamelauncher.ui.navigation.Screen
import org.gamelauncher.ui.theme.Footer
import org.gamelauncher.ui.theme.Menu
import org.gamelauncher.ui.theme.MenuHighlight
import org.gamelauncher.ui.theme.SearchField
import org.gamelauncher.ui.theme.TextDisabled
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.TopBar
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CommandHints(
    val extra: Pair<String, String>? = null,
    val select: String = "Select",
    val back: String = "Back",
)

@Composable
fun TopStatusBar(
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQuery: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onUserMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val status = rememberDeviceStatus()
    val focusRequester = remember { FocusRequester() }
    var clock by remember { mutableStateOf(nowLabel()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = nowLabel()
            delay(15_000)
        }
    }
    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            delay(40)
            runCatching { focusRequester.requestFocus() }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TopBar)
            .then(modifier)
            .height(52.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (searchOpen) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SearchField)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF4A4A4A))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF222222), fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search for games or apps…",
                                color = Color(0xFF888888),
                                fontSize = 16.sp,
                            )
                        }
                        inner()
                    },
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close search",
                    tint = Color(0xFF4A4A4A),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onToggleSearch),
                )
            }
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.weight(1f))
            TopBarIcon(Icons.Default.Search, "Search", onClick = onToggleSearch)
            Spacer(Modifier.width(8.dp))
        }
        TopBarIcon(
            icon = if (status.wifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
            label = if (status.wifiConnected) "Wi-Fi connected" else "Wi-Fi",
            onClick = { expandNotificationShade(context) },
        )
        Spacer(Modifier.width(4.dp))
        TopBarIcon(
            icon = batteryIcon(status),
            label = "Battery ${status.batteryPercent}%",
            onClick = { expandNotificationShade(context) },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            clock,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .tileFrame(false, RoundedCornerShape(6.dp), width = 3.dp)
                .tileClick { expandNotificationShade(context) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .tileFrame(false, RoundedCornerShape(8.dp), width = 3.dp)
                .tileClick(onUserMenu)
                .padding(4.dp),
        ) {
            DiamondMark(32.dp)
        }
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .tileFrame(false, RoundedCornerShape(8.dp), width = 3.dp)
            .tileClick(onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun batteryIcon(status: DeviceStatus) = when {
    status.charging -> Icons.Default.BatteryChargingFull
    status.batteryPercent <= 15 -> Icons.Default.BatteryAlert
    status.batteryPercent >= 85 -> Icons.Default.BatteryFull
    else -> Icons.Default.BatteryStd
}

@Composable
fun CommandBar(
    hints: CommandHints,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Footer)
            .then(modifier)
            .height(56.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .tileFrame(false, RoundedCornerShape(6.dp), width = 3.dp)
                .tileClick(onMenu)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiamondMark(22.dp)
            Spacer(Modifier.width(10.dp))
            Text("MENU", color = TextPrimary, fontSize = 14.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        hints.extra?.let { (letter, caption) ->
            FaceButton(letter, caption)
            Spacer(Modifier.width(22.dp))
        }
        FaceButton("A", hints.select)
        Spacer(Modifier.width(22.dp))
        Box(
            modifier = Modifier
                .tileFrame(false, RoundedCornerShape(6.dp), width = 3.dp)
                .tileClick(onBack)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            FaceButton("B", hints.back)
        }
    }
}

@Composable
fun SideMenu(
    current: Screen,
    onSelect: (MenuItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Menu)
            .padding(top = 28.dp, bottom = 12.dp),
    ) {
        MenuItem.entries.forEach { item ->
            val selected = when (item) {
                MenuItem.Home -> current is Screen.Home
                MenuItem.Library -> current is Screen.Library
                MenuItem.Store -> current is Screen.Store
                MenuItem.Settings -> current is Screen.Settings
                else -> false
            }
            val icon = when (item) {
                MenuItem.Home -> Icons.Default.Home
                MenuItem.Library -> Icons.Default.GridView
                MenuItem.Store -> Icons.Default.Storefront
                MenuItem.Friends -> Icons.Default.People
                MenuItem.Media -> Icons.Default.VideoLibrary
                MenuItem.Downloads -> Icons.Default.CloudDownload
                MenuItem.Settings -> Icons.Default.Settings
                MenuItem.Close -> Icons.Default.Close
            }
            MenuRow(item.label, icon, selected, item.enabled) { onSelect(item) }
        }
    }
}

@Composable
private fun MenuRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MenuHighlight else Color.Transparent
    val tint = when {
        !enabled -> TextDisabled
        else -> TextPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tileFrame(selected, RoundedCornerShape(0.dp), width = 2.dp)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DimScrim(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
    )
}

private fun nowLabel(): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")).uppercase()
