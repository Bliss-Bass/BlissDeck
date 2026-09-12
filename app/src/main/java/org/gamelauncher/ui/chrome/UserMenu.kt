package org.gamelauncher.ui.chrome

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.rememberIsDefaultHome
import org.gamelauncher.ui.theme.Menu
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary

@Composable
fun UserMenu(
    onLauncherSettings: () -> Unit,
    onCloseLauncher: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "dev" }
    }
    val isDefaultHome = rememberIsDefaultHome()
    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Menu)
            .padding(vertical = 10.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text("Game Launcher", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(version, color = TextMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        UserMenuRow("Launcher settings", Icons.Default.Tune) {
            onDismiss()
            onLauncherSettings()
        }
        UserMenuRow("Set as Home app", Icons.Default.Home) {
            onDismiss()
            start(context, Intent(Settings.ACTION_HOME_SETTINGS))
        }
        UserMenuRow("System settings", Icons.Default.Settings) {
            onDismiss()
            start(context, Intent(Settings.ACTION_SETTINGS))
        }
        UserMenuRow("Display settings", Icons.Default.Tv) {
            onDismiss()
            start(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        UserMenuRow("App info", Icons.Default.Info) {
            onDismiss()
            start(
                context,
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        }
        UserMenuRow("About tablet", Icons.Default.Smartphone) {
            onDismiss()
            start(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        }
        if (!isDefaultHome) {
            UserMenuRow("Close launcher", Icons.Default.Close) {
                onDismiss()
                onCloseLauncher()
            }
        }
    }
}

@Composable
private fun UserMenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextPrimary, fontSize = 16.sp)
    }
}

private fun start(context: android.content.Context, intent: Intent) {
    runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
