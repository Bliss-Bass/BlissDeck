package org.gamelauncher.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.LocalSettings
import org.gamelauncher.data.StoreApp
import org.gamelauncher.data.StoreApps
import org.gamelauncher.ui.components.AppIconImage
import org.gamelauncher.ui.components.cardShape
import org.gamelauncher.ui.components.rememberArtwork
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile
import org.gamelauncher.ui.theme.chromeContentPadding

@Composable
fun StoreScreen() {
    val context = LocalContext.current
    val prefs by LocalSettings.current.state.collectAsState()
    val stores = remember(context) { StoreApps.installed(context) }
    val preferred = remember(stores, prefs.storePackage) {
        StoreApps.preferred(context, prefs.storePackage)
    }
    LaunchedEffect(preferred) {
        if (preferred != null) StoreApps.launch(context, preferred.packageName)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .chromeContentPadding(extraTop = 28.dp, extraBottom = 28.dp, horizontal = 28.dp),
    ) {
        when {
            stores.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No store app found", color = TextPrimary, fontSize = 24.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Install Play Store, Aurora Store, Droid-ify, or Neo Store to use this shortcut.",
                    color = TextMuted,
                    fontSize = 16.sp,
                )
            }
            preferred != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Opening ${preferred.label}", color = TextPrimary, fontSize = 24.sp)
                Spacer(Modifier.height(16.dp))
                StoreTile(preferred) { StoreApps.launch(context, preferred.packageName) }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Text("Store", color = TextPrimary, fontSize = 24.sp)
                Spacer(Modifier.height(8.dp))
                Text("Choose a store, or set a default in Settings.", color = TextMuted, fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(stores, key = { it.packageName }) { store ->
                        StoreTile(store) { StoreApps.launch(context, store.packageName) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreTile(store: StoreApp, onClick: () -> Unit) {
    val artwork = rememberArtwork(store.packageName, store.label, isGame = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tileFrame(false, RoundedCornerShape(8.dp), width = 2.dp)
            .background(Tile)
            .tileClick(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(cardShape())
                .background(Background),
            contentAlignment = Alignment.Center,
        ) {
            val icon = artwork.icon
            if (icon != null) AppIconImage(icon, Modifier.fillMaxSize())
            else Text(store.label.take(1), color = TextPrimary, fontSize = 20.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(store.label, color = TextPrimary, fontSize = 18.sp)
    }
}
