package org.gamelauncher.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.CloseGameService
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.data.isDefaultHomeApp
import org.gamelauncher.data.rememberResumeTick
import org.gamelauncher.data.RecentsArt
import org.gamelauncher.data.RecentsLayout
import org.gamelauncher.data.RecentsShape
import org.gamelauncher.data.RecentsSize
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val artwork = LocalArtwork.current
    val settings = LocalSettings.current
    val prefs by settings.state.collectAsState()
    var apiKey by remember { mutableStateOf(artwork.apiKey) }
    val resumeTick = rememberResumeTick()
    val isDefaultHome = remember(resumeTick) { isDefaultHomeApp(context) }
    val accessibilityOn = remember(resumeTick) { CloseGameService.isEnabled(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 24.sp)
        Spacer(Modifier.height(18.dp))

        SettingsCard("Recents") {
            ChoiceRow(
                "Layout",
                RecentsLayout.entries.map { it to it.label },
                prefs.recentsLayout,
            ) { settings.update { p -> p.copy(recentsLayout = it) } }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Size",
                RecentsSize.entries.map { it to it.label },
                prefs.recentsSize,
            ) { settings.update { p -> p.copy(recentsSize = it) } }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Unselected cards",
                RecentsShape.entries.map { it to it.label },
                prefs.recentsShape,
            ) { settings.update { p -> p.copy(recentsShape = it) } }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Card art",
                RecentsArt.entries.map { it to it.label },
                prefs.recentsArt,
            ) { settings.update { p -> p.copy(recentsArt = it) } }
            if (prefs.recentsLayout == RecentsLayout.Coverflow) {
                Spacer(Modifier.height(8.dp))
                SettingToggle("Tilt unselected cards", prefs.recentsTilt) {
                    settings.update { p -> p.copy(recentsTilt = !p.recentsTilt) }
                }
            }
            SettingToggle("Show title under Recents", prefs.showSelectedTitle) {
                settings.update { p -> p.copy(showSelectedTitle = !p.showSelectedTitle) }
            }
        }

        SettingsCard("Appearance") {
            SettingToggle("Ambient backdrop", prefs.ambientBackdrop) {
                settings.update { p -> p.copy(ambientBackdrop = !p.ambientBackdrop) }
            }
        }

        SettingsCard("Input") {
            SettingToggle("Esc goes Back", prefs.escAsBack) {
                settings.update { p -> p.copy(escAsBack = !p.escAsBack) }
            }
            SettingToggle("B goes Back", prefs.bAsBack) {
                settings.update { p -> p.copy(bAsBack = !p.bAsBack) }
            }
            SettingToggle("Win / Home opens menu", prefs.winOpensMenu) {
                settings.update { p -> p.copy(winOpensMenu = !p.winOpensMenu) }
            }
        }

        SettingsCard("Permissions") {
            PermissionRow(
                title = "Default Home launcher",
                status = if (isDefaultHome) "This app is Home" else "Not set",
                action = if (isDefaultHome) "Change" else "Set as Home",
            ) { openHomeChooser(context) }
            Spacer(Modifier.height(10.dp))
            PermissionRow(
                title = "Accessibility (close games)",
                status = if (accessibilityOn) "Enabled" else "Off",
                action = "Open",
            ) { openAccessibilitySettings(context) }
        }

        SettingsCard("SteamGridDB") {
            Text(
                if (apiKey.isBlank()) {
                    "App icons show immediately. Paste an API key to fetch covers and heroes."
                } else {
                    "API key saved. Covers refresh for games as they appear."
                },
                color = TextMuted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background, RoundedCornerShape(4.dp))
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (apiKey.isEmpty()) Text("API key", color = TextMuted, fontSize = 16.sp)
                    inner()
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Save key",
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier
                    .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                    .background(Background)
                    .tileClick { artwork.apiKey = apiKey }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Get a key at steamgriddb.com/profile/preferences/api",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.tileClick {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.steamgriddb.com/profile/preferences/api"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val RecentsLayout.label: String
    get() = when (this) {
        RecentsLayout.Coverflow -> "Coverflow"
        RecentsLayout.Row -> "Row"
    }

private val RecentsSize.label: String
    get() = when (this) {
        RecentsSize.Compact -> "Compact"
        RecentsSize.Comfortable -> "Comfortable"
        RecentsSize.Large -> "Large"
    }

private val RecentsShape.label: String
    get() = when (this) {
        RecentsShape.Square -> "Square"
        RecentsShape.Wide -> "Wide"
    }

private val RecentsArt.label: String
    get() = when (this) {
        RecentsArt.Icon -> "App icon"
        RecentsArt.Cover -> "Cover art"
    }

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Tile, RoundedCornerShape(6.dp))
            .padding(18.dp),
    ) {
        Text(title, color = TextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Text(label, color = TextMuted, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (value, title) ->
            SteamPill(title, selected == value) { onSelect(value) }
        }
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .tileFrame(false, RoundedCornerShape(6.dp), width = 2.dp)
            .tileClick(onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            if (checked) "On" else "Off",
            color = if (checked) PlayGreen else TextMuted,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PermissionRow(title: String, status: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tileFrame(false, RoundedCornerShape(6.dp), width = 2.dp)
            .tileClick(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp)
            Text(status, color = TextMuted, fontSize = 13.sp)
        }
        Text(action, color = PlayGreen, fontSize = 15.sp)
    }
}

private fun openHomeChooser(context: Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                context.startActivity(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }
}

private fun openAccessibilitySettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
