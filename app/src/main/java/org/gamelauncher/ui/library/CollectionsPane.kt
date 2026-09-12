package org.gamelauncher.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.gamelauncher.data.CollectionsStore
import org.gamelauncher.data.GameCollection
import org.gamelauncher.data.LibrarySnapshot
import org.gamelauncher.data.LocalCollections
import org.gamelauncher.data.findEntry
import org.gamelauncher.ui.components.CoverArt
import org.gamelauncher.ui.components.cardShape
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

@Composable
fun CollectionsPane(
    snapshot: LibrarySnapshot,
    onOpenGame: (String) -> Unit,
) {
    val store = LocalCollections.current
    val collections by store.state.collectAsState()
    var openId by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<GameCollection?>(null) }
    var creating by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<GameCollection?>(null) }
    val open = collections.firstOrNull { it.id == openId }

    if (open == null) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Collections", color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text(
                    "New",
                    color = PlayGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                        .tileClick { creating = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            collections.forEach { collection ->
                CollectionRow(
                    collection = collection,
                    onOpen = { openId = collection.id },
                    onRename = { renaming = collection }.takeIf { collection.id != CollectionsStore.FAVORITES_ID },
                    onDelete = { store.delete(collection.id) }.takeIf { collection.id != CollectionsStore.FAVORITES_ID },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    } else {
        val games = open.packageNames.mapNotNull { snapshot.findEntry(it) }
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Back",
                    color = PlayGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .tileClick { openId = null }
                        .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                )
                Text(open.name, color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text(
                    "Add games",
                    color = PlayGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .tileFrame(false, RoundedCornerShape(4.dp), width = 2.dp)
                        .tileClick { picking = open }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (games.isEmpty()) {
                Text("No games in this collection yet.", color = TextMuted, fontSize = 16.sp)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    itemsIndexed(games, key = { _, game -> game.id }) { _, game ->
                        CoverArt(
                            title = game.title,
                            hue = game.coverHue,
                            showTitle = true,
                            packageName = game.packageName,
                            isGame = true,
                            modifier = Modifier
                                .height(230.dp)
                                .tileClick { onOpenGame(game.id) },
                        )
                    }
                }
            }
        }
    }

    picking?.let { collection ->
        AddGamesSheet(
            snapshot = snapshot,
            collection = collection,
            onDismiss = { picking = null },
            onToggle = { pkg -> store.toggle(collection.id, pkg) },
        )
    }
    if (creating) {
        NameSheet(
            title = "New collection",
            initial = "",
            onDismiss = { creating = false },
            onSave = {
                store.create(it)
                creating = false
            },
        )
    }
    renaming?.let { collection ->
        NameSheet(
            title = "Rename collection",
            initial = collection.name,
            onDismiss = { renaming = null },
            onSave = {
                store.rename(collection.id, it)
                renaming = null
            },
        )
    }
}

@Composable
private fun CollectionRow(
    collection: GameCollection,
    onOpen: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tileFrame(false, RoundedCornerShape(6.dp), width = 2.dp)
            .background(Tile)
            .tileClick(onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(collection.name, color = TextPrimary, fontSize = 18.sp)
            Text("${collection.size} games", color = TextMuted, fontSize = 14.sp)
        }
        if (onRename != null) {
            Text("Rename", color = PlayGreen, fontSize = 14.sp, modifier = Modifier.tileClick(onRename).padding(8.dp))
        }
        if (onDelete != null) {
            Text("Delete", color = TextMuted, fontSize = 14.sp, modifier = Modifier.tileClick(onDelete).padding(8.dp))
        }
    }
}

@Composable
private fun NameSheet(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(cardShape())
                .background(Tile)
                .padding(24.dp),
        ) {
            Text(title, color = TextPrimary, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 18.sp),
                cursorBrush = SolidColor(TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background, cardShape())
                    .padding(12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cancel", color = TextMuted, modifier = Modifier.tileClick(onDismiss).padding(8.dp))
                Text("Save", color = PlayGreen, modifier = Modifier.tileClick { onSave(value) }.padding(8.dp))
            }
        }
    }
}

@Composable
private fun AddGamesSheet(
    snapshot: LibrarySnapshot,
    collection: GameCollection,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    val latest = LocalCollections.current.state.collectAsState().value
        .firstOrNull { it.id == collection.id } ?: collection
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .height(420.dp)
                .clip(cardShape())
                .background(Tile)
                .padding(20.dp),
        ) {
            Text("Add games — ${collection.name}", color = TextPrimary, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                snapshot.libraryGames.forEach { game ->
                    val included = latest.contains(game.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tileClick { onToggle(game.packageName) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (included) "Added" else "Add",
                            color = if (included) PlayGreen else TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.width(64.dp),
                        )
                        Text(game.title, color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }
            Text(
                "Done",
                color = PlayGreen,
                modifier = Modifier
                    .align(Alignment.End)
                    .tileClick(onDismiss)
                    .padding(8.dp),
            )
        }
    }
}
