package org.gamelauncher.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.ArtworkBackdrop
import org.gamelauncher.data.ArtworkCoverStyle
import org.gamelauncher.data.ArtworkIconSource
import org.gamelauncher.data.CloseGameService
import org.gamelauncher.data.GameSession
import org.gamelauncher.data.LocalAccountPhoto
import org.gamelauncher.data.LocalArtwork
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.data.LocalThemeStore
import org.gamelauncher.data.ThemeBlurLevel
import org.gamelauncher.data.ThemeIconStyle
import org.gamelauncher.data.toIni
import org.gamelauncher.data.isDefaultHomeApp
import org.gamelauncher.data.rememberResumeTick
import org.gamelauncher.data.RecentsArt
import org.gamelauncher.data.RecentsLayout
import org.gamelauncher.data.RecentsShape
import org.gamelauncher.data.RecentsSize
import org.gamelauncher.data.StoreApps
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.StopRed
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.chromeContentPadding
import org.gamelauncher.ui.theme.label

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
    val usageOn = remember(resumeTick) { GameSession.hasUsageAccess(context) }
    val stores = remember(resumeTick) { StoreApps.installed(context) }
    val themeStore = LocalThemeStore.current
    val theme by themeStore.resolved.collectAsState()
    val themeEntries by themeStore.entries.collectAsState()
    val importTheme = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { themeStore.importFrom(it) }
    }
    val exportTheme = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri?.let { dest ->
            context.contentResolver.openOutputStream(dest)?.use { it.write(theme.toIni().toByteArray()) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .chromeContentPadding(extraTop = 20.dp, extraBottom = 20.dp, horizontal = 28.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 24.sp)
        Spacer(Modifier.height(18.dp))

        SettingsCard("Themes") {
            Text(
                "Enable packs to stack them on Default. Later packs win. Import a .ini or .cfg, or export the current mix.",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            themeEntries.forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        val suffix = when {
                            entry.id == "default" -> "  · always on"
                            entry.builtin -> "  · built-in"
                            entry.author.isNotBlank() -> "  · ${entry.author}"
                            else -> ""
                        }
                        SettingToggle(
                            label = entry.name + suffix,
                            checked = entry.enabled,
                        ) {
                            if (entry.id != "default") themeStore.toggle(entry.id)
                        }
                    }
                    if (!entry.builtin) {
                        Text(
                            "Remove",
                            color = StopRed,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .tileClick { themeStore.delete(entry.id) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Import .ini",
                    color = PlayGreen,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                        .tileClick { importTheme.launch("*/*") }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(
                    "Export current",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                        .tileClick { exportTheme.launch("launcher-theme.ini") }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        SettingsCard("Account") {
            val photos = LocalAccountPhoto.current
            val epoch by photos.epoch.collectAsState()
            val bitmap = remember(epoch) { photos.bitmap() }
            val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { photos.setFrom(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Account photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Account photo",
                        tint = TextPrimary,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        if (photos.hasCustom) "Custom photo" else "System user photo when available",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Choose photo",
                            color = PlayGreen,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                                .tileClick { pickPhoto.launch("image/*") }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                        if (photos.hasCustom) {
                            Text(
                                "Use system",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                                    .tileClick { photos.clearCustom() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        SettingsCard("Bars") {
            ChoiceRow(
                "Top bar",
                listOf(40 to "Compact", 52 to "Regular", 64 to "Tall", 80 to "Large"),
                theme.chrome.topBarHeight.value.toInt(),
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(chrome = pack.chrome.copy(topBarHeight = value.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Bottom bar",
                listOf(44 to "Compact", 56 to "Regular", 68 to "Tall", 84 to "Large"),
                theme.chrome.bottomBarHeight.value.toInt(),
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(chrome = pack.chrome.copy(bottomBarHeight = value.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Top opacity",
                listOf(100 to "100%", 85 to "85%", 70 to "70%", 50 to "50%"),
                nearest(theme.chrome.topAlpha, listOf(100, 85, 70, 50)),
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(chrome = pack.chrome.copy(topAlpha = value / 100f))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Bottom opacity",
                listOf(100 to "100%", 85 to "85%", 70 to "70%", 50 to "50%"),
                nearest(theme.chrome.bottomAlpha, listOf(100, 85, 70, 50)),
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(chrome = pack.chrome.copy(bottomAlpha = value / 100f))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Bar color",
                listOf(
                    0xFF000000.toInt() to "Black",
                    0xFF0E1820.toInt() to "Navy",
                    0xFF2B333C.toInt() to "Steel",
                ),
                theme.colors.topBar.toArgb(),
            ) { argb ->
                val color = Color(argb)
                themeStore.patchCustom { pack ->
                    pack.copy(colors = pack.colors.copy(topBar = color, footer = color))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Menu opacity",
                listOf(100 to "100%", 85 to "85%", 70 to "70%", 50 to "50%"),
                nearest(theme.chrome.menuAlpha, listOf(100, 85, 70, 50)),
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(chrome = pack.chrome.copy(menuAlpha = value / 100f))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Blur",
                ThemeBlurLevel.entries.map { it to it.label() },
                theme.chrome.blurLevel,
            ) { level ->
                themeStore.patchCustom { pack ->
                    pack.copy(
                        chrome = pack.chrome.copy(
                            blur = level.enabled,
                            blurRadius = level.radius,
                        ),
                    )
                }
            }
            Text(
                "Blur frosts the page behind bars, menus, and dim overlays. Drop opacity below 100% to see it.",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SettingsCard("Borders & icons") {
            ChoiceRow(
                "Border",
                listOf(2 to "Thin", 4 to "Regular", 6 to "Thick"),
                theme.borders.width.value.toInt().let { n ->
                    listOf(2, 4, 6).minBy { kotlin.math.abs(it - n) }
                },
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(borders = pack.borders.copy(width = value.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Corners",
                listOf(2 to "Sharp", 6 to "Regular", 12 to "Round"),
                theme.borders.radius.value.toInt().let { n ->
                    listOf(2, 6, 12).minBy { kotlin.math.abs(it - n) }
                },
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(borders = pack.borders.copy(radius = value.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Border color",
                listOf(
                    0xFFFFFFFF.toInt() to "White",
                    0xFF5CB030.toInt() to "Accent",
                    0xFF8A939C.toInt() to "Muted",
                ),
                theme.borders.color.toArgb(),
            ) { argb ->
                themeStore.patchCustom { pack ->
                    pack.copy(borders = pack.borders.copy(color = Color(argb)))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Icons",
                listOf(ThemeIconStyle.Filled to "Filled", ThemeIconStyle.Outlined to "Outlined"),
                theme.icons.style,
            ) { value ->
                themeStore.patchCustom { pack ->
                    pack.copy(icons = pack.icons.copy(style = value))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Icon tint",
                listOf(
                    0xFFFFFFFF.toInt() to "White",
                    0xFF5CB030.toInt() to "Accent",
                    0xFF8A939C.toInt() to "Muted",
                ),
                theme.icons.tint.toArgb(),
            ) { argb ->
                themeStore.patchCustom { pack ->
                    pack.copy(icons = pack.icons.copy(tint = Color(argb)))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Accent",
                listOf(
                    0xFF5CB030.toInt() to "Green",
                    0xFF4EA0F3.toInt() to "Blue",
                    0xFFE39B2E.toInt() to "Amber",
                    0xFFE85D4C.toInt() to "Red",
                ),
                theme.colors.playGreen.toArgb(),
            ) { argb ->
                themeStore.patchCustom { pack ->
                    pack.copy(colors = pack.colors.copy(playGreen = Color(argb)))
                }
            }
        }

        SettingsCard("Pages") {
            Text("Home", color = TextPrimary, fontSize = 15.sp)
            SettingToggle("Last played", theme.layouts.homeLastPlayed) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(homeLastPlayed = !pack.layouts.homeLastPlayed))
                }
            }
            SettingToggle("Play now", theme.layouts.homePlayNow) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(homePlayNow = !pack.layouts.homePlayNow))
                }
            }
            SettingToggle("What's New / Favorites / Recommended", theme.layouts.homeFeed) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(homeFeed = !pack.layouts.homeFeed))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Library", color = TextPrimary, fontSize = 15.sp)
            SettingToggle("Collections tab", theme.layouts.libraryCollections) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(libraryCollections = !pack.layouts.libraryCollections))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Game page", color = TextPrimary, fontSize = 15.sp)
            SettingToggle("Activity", theme.layouts.gameActivity) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(gameActivity = !pack.layouts.gameActivity))
                }
            }
            SettingToggle("Community", theme.layouts.gameCommunity) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(gameCommunity = !pack.layouts.gameCommunity))
                }
            }
            SettingToggle("Game Info", theme.layouts.gameInfo) {
                themeStore.patchCustom { pack ->
                    pack.copy(layouts = pack.layouts.copy(gameInfo = !pack.layouts.gameInfo))
                }
            }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "What's New entries",
                listOf(8 to "8", 16 to "16", 32 to "32"),
                prefs.whatsNewCount,
            ) { settings.update { p -> p.copy(whatsNewCount = it) } }
        }

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
            SettingToggle("Show captions on hover", prefs.hoverCaptions) {
                settings.update { p -> p.copy(hoverCaptions = !p.hoverCaptions) }
            }
        }

        SettingsCard("Store") {
            if (stores.isEmpty()) {
                Text(
                    "No store app installed. Add Play Store, Aurora Store, Droid-ify, or Neo Store.",
                    color = TextMuted,
                    fontSize = 14.sp,
                )
            } else {
                val selected = prefs.storePackage.takeIf { pkg -> stores.any { it.packageName == pkg } }.orEmpty()
                ChoiceRow(
                    "Open with",
                    listOf("" to "Ask each time") + stores.map { it.packageName to it.label },
                    selected,
                ) { settings.update { p -> p.copy(storePackage = it) } }
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
            Spacer(Modifier.height(10.dp))
            PermissionRow(
                title = "Usage access (running apps)",
                status = if (usageOn) "Enabled" else "Off",
                action = "Open",
            ) { GameSession.openUsageAccessSettings(context) }
        }

        SettingsCard("SteamGridDB") {
            ChoiceRow(
                "Icons",
                listOf(
                    ArtworkIconSource.App to "App icon",
                    ArtworkIconSource.SteamGrid to "SteamGridDB",
                ),
                prefs.iconSource,
            ) { settings.update { p -> p.copy(iconSource = it) } }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Library covers",
                listOf(
                    ArtworkCoverStyle.Tall to "Tall card",
                    ArtworkCoverStyle.Wide to "Wide banner",
                ),
                prefs.coverStyle,
            ) { settings.update { p -> p.copy(coverStyle = it) } }
            Spacer(Modifier.height(14.dp))
            ChoiceRow(
                "Game backdrop",
                listOf(
                    ArtworkBackdrop.Hero to "Hero",
                    ArtworkBackdrop.Tall to "Tall card",
                    ArtworkBackdrop.Play to "Play art",
                ),
                prefs.backdrop,
            ) { settings.update { p -> p.copy(backdrop = it) } }
            Spacer(Modifier.height(12.dp))
            Text(
                if (apiKey.isBlank()) {
                    "App icons show immediately. Paste an API key to fetch covers, heroes, and icons. Pick a specific image on a game’s Info page."
                } else {
                    "API key saved. Open a game’s Info page to choose among SteamGridDB covers, heroes, and icons."
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Text(label, color = TextMuted, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

private fun nearest(alpha: Float, options: List<Int>): Int {
    val n = (alpha * 100f).toInt()
    return options.minBy { kotlin.math.abs(it - n) }
}
