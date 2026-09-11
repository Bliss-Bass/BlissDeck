package org.gamelauncher.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary

@Composable
fun StoreScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Store", color = TextPrimary, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Vapor opened the Play Store from this item. We’ll wire a store later; this is the slot in the shell.",
                color = TextMuted,
                fontSize = 16.sp,
            )
        }
    }
}
