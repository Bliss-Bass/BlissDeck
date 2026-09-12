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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.gamelauncher.data.GameSession
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.RunningApp
import org.gamelauncher.data.ThemeIconStyle
import org.gamelauncher.data.rememberIsDefaultHome
import org.gamelauncher.ui.components.AppIconImage
import org.gamelauncher.ui.components.DiamondMark
import org.gamelauncher.ui.components.FaceButton
import org.gamelauncher.ui.components.HoverCaption
import org.gamelauncher.ui.components.columnFocus
import org.gamelauncher.ui.components.rememberArtwork
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Menu
import org.gamelauncher.ui.theme.MenuDivider
import org.gamelauncher.ui.theme.MenuHighlight
import org.gamelauncher.ui.theme.MenuSunken
import org.gamelauncher.ui.theme.SearchField
import org.gamelauncher.ui.theme.TextDisabled
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.frosted
import org.gamelauncher.ui.navigation.MenuItem
import org.gamelauncher.ui.navigation.Screen
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
    val theme = LocalTheme.current
    val chrome = theme.chrome
    val barColor = theme.colors.topBar.copy(alpha = chrome.topAlpha)
    val iconTint = theme.icons.tint
    Box(
        modifier = modifier
            .fillMaxWidth()
            .frosted(barColor),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chrome.topBarHeight)
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
                HoverCaption("Close search") {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close search",
                        tint = Color(0xFF4A4A4A),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onToggleSearch),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.weight(1f))
            TopBarIcon(Icons.Default.Search, "Search", iconTint, onClick = onToggleSearch)
            Spacer(Modifier.width(8.dp))
        }
        TopBarIcon(
            icon = if (status.wifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
            label = if (status.wifiConnected) "Wi-Fi connected" else "Wi-Fi off",
            tint = iconTint,
            onClick = { expandNotificationShade(context) },
        )
        Spacer(Modifier.width(4.dp))
        TopBarIcon(
            icon = batteryIcon(status),
            label = if (status.charging) {
                "Battery ${status.batteryPercent}%, charging"
            } else {
                "Battery ${status.batteryPercent}%"
            },
            tint = iconTint,
            onClick = { expandNotificationShade(context) },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            clock,
            color = iconTint,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .tileFrame(false, RoundedCornerShape(6.dp), width = 3.dp)
                .tileClick { expandNotificationShade(context) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(12.dp))
        HoverCaption("Account") {
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
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    HoverCaption(label) {
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
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
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
    val theme = LocalTheme.current
    val chrome = theme.chrome
    val barColor = theme.colors.footer.copy(alpha = chrome.bottomAlpha)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .frosted(barColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chrome.bottomBarHeight)
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
}

@Composable
fun SideMenu(
    current: Screen,
    running: List<RunningApp>,
    onSelect: (MenuItem) -> Unit,
    onSwitch: (RunningApp) -> Unit,
) {
    val hideClose = rememberIsDefaultHome()
    val visible = remember(hideClose) {
        MenuItem.entries.filter { it != MenuItem.Close || !hideClose }
    }
    val enabled = remember(visible) { visible.filter { it.enabled } }
    val focusCount = running.size + enabled.size
    val requesters = remember(focusCount, running.map { it.packageName }, enabled) {
        List(focusCount) { FocusRequester() }
    }
    val selectedIndex = (
        running.size + enabled.indexOfFirst { item ->
            when (item) {
                MenuItem.Home -> current is Screen.Home
                MenuItem.Library -> current is Screen.Library
                MenuItem.Store -> current is Screen.Store
                MenuItem.Settings -> current is Screen.Settings
                else -> false
            }
        }
        ).coerceIn(0, (focusCount - 1).coerceAtLeast(0))
    LaunchedEffect(requesters, selectedIndex) {
        if (requesters.isEmpty()) return@LaunchedEffect
        delay(40)
        runCatching { requesters[selectedIndex].requestFocus() }
    }
    val chrome = LocalTheme.current.chrome
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .frosted(Menu.copy(alpha = chrome.menuAlpha))
            .verticalScroll(rememberScrollState())
            .padding(top = chrome.topBarHeight, bottom = chrome.bottomBarHeight),
    ) {
        val showOpen = running.isNotEmpty() || !GameSession.hasUsageAccess(LocalContext.current)
        if (showOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MenuSunken.copy(alpha = chrome.menuAlpha))
                    .padding(top = 28.dp, bottom = 10.dp),
            ) {
                if (running.isNotEmpty()) {
                    Text(
                        "Open",
                        color = TextMuted,
                        fontSize = 13.sp,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                    )
                    running.forEachIndexed { index, app ->
                        RunningRow(
                            app = app,
                            modifier = Modifier.columnFocus(requesters, index),
                        ) { onSwitch(app) }
                    }
                } else {
                    Text(
                        "Turn on Usage access in Settings to list apps running in the background.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MenuDivider),
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(28.dp))
        }
        visible.forEach { item ->
            val selected = when (item) {
                MenuItem.Home -> current is Screen.Home
                MenuItem.Library -> current is Screen.Library
                MenuItem.Store -> current is Screen.Store
                MenuItem.Settings -> current is Screen.Settings
                else -> false
            }
            val outlined = LocalTheme.current.icons.style == ThemeIconStyle.Outlined
            val icon = when (item) {
                MenuItem.Home -> if (outlined) Icons.Outlined.Home else Icons.Default.Home
                MenuItem.Library -> if (outlined) Icons.Outlined.GridView else Icons.Default.GridView
                MenuItem.Store -> if (outlined) Icons.Outlined.Storefront else Icons.Default.Storefront
                MenuItem.Settings -> if (outlined) Icons.Outlined.Settings else Icons.Default.Settings
                MenuItem.Close -> if (outlined) Icons.Outlined.Close else Icons.Default.Close
            }
            val focusIndex = enabled.indexOf(item).let { if (it < 0) -1 else it + running.size }
            MenuRow(
                label = item.label,
                icon = icon,
                selected = selected,
                enabled = item.enabled,
                modifier = if (focusIndex >= 0) Modifier.columnFocus(requesters, focusIndex) else Modifier,
            ) { onSelect(item) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RunningRow(
    app: RunningApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val artwork = rememberArtwork(app.packageName, app.title, isGame = false)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tileFrame(false, RoundedCornerShape(0.dp), width = 2.dp)
            .tileClick(onClick)
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Tile),
            contentAlignment = Alignment.Center,
        ) {
            val icon = artwork.icon
            if (icon != null) {
                AppIconImage(icon, Modifier.fillMaxSize())
            } else {
                Text(app.title.take(1).uppercase(), color = TextPrimary, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            app.title,
            color = TextPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) MenuHighlight else Color.Transparent
    val tint = when {
        !enabled -> TextDisabled
        else -> TextPrimary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tileFrame(selected, RoundedCornerShape(0.dp), width = 2.dp)
            .background(bg)
            .then(if (enabled) Modifier.tileClick(onClick) else Modifier)
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
    val blur = LocalTheme.current.chrome.blurLevel
    Box(
        modifier = Modifier
            .fillMaxSize()
            .frosted(
                Color.Black.copy(alpha = 0.45f),
                blurRadius = if (blur.enabled) (blur.radius / 2) else 0.dp,
            )
            .clickable(onClick = onClick)
            .focusProperties { canFocus = false },
    )
}

private fun nowLabel(): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")).uppercase()
