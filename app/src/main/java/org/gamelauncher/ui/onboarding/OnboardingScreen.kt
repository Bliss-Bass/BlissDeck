package org.gamelauncher.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gamelauncher.data.LauncherPermissions
import org.gamelauncher.data.LocalTheme
import org.gamelauncher.data.SetupGrant
import org.gamelauncher.data.rememberResumeTick
import org.gamelauncher.ui.chrome.CommandHints
import org.gamelauncher.ui.components.DiamondMark
import org.gamelauncher.ui.components.SteamPill
import org.gamelauncher.ui.components.cardShape
import org.gamelauncher.ui.components.tileClick
import org.gamelauncher.ui.components.tileFrame
import org.gamelauncher.ui.theme.Background
import org.gamelauncher.ui.theme.PlayGreen
import org.gamelauncher.ui.theme.TextMuted
import org.gamelauncher.ui.theme.TextPrimary
import org.gamelauncher.ui.theme.Tile

private enum class WizardPage {
    Welcome,
    Home,
    Usage,
    Accessibility,
    Notifications,
    Done,
}

private val WizardPage.grant: SetupGrant?
    get() = when (this) {
        WizardPage.Home -> SetupGrant.Home
        WizardPage.Usage -> SetupGrant.Usage
        WizardPage.Accessibility -> SetupGrant.Accessibility
        WizardPage.Notifications -> SetupGrant.Notifications
        WizardPage.Welcome, WizardPage.Done -> null
    }

class OnboardingSession {
    var back: () -> Unit = {}
    var hints by mutableStateOf(CommandHints(select = "Start", back = "Skip"))
}

@Composable
fun OnboardingScreen(
    session: OnboardingSession,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val resumeTick = rememberResumeTick()
    val grants = remember(resumeTick) { LauncherPermissions.snapshot(context) }
    var page by remember { mutableIntStateOf(0) }
    val pages = WizardPage.entries
    val current = pages[page.coerceIn(0, pages.lastIndex)]
    val grant = current.grant
    val granted = grant != null && grants[grant]
    var grantedWhenOpened by remember(current) { mutableStateOf(granted) }

    fun finish() = onFinished()

    fun goNext() {
        if (page >= pages.lastIndex) finish() else page++
    }

    fun goBack() {
        when (current) {
            WizardPage.Welcome -> finish()
            else -> page--
        }
    }

    fun primary() {
        when {
            current == WizardPage.Welcome -> goNext()
            current == WizardPage.Done -> finish()
            granted -> goNext()
            grant != null -> LauncherPermissions.open(context, grant)
        }
    }

    LaunchedEffect(granted, current) {
        if (grant != null && granted && !grantedWhenOpened) {
            goNext()
        }
    }

    val hints = CommandHints(
        extra = if (grant != null && !granted) "Y" to "Skip" else null,
        select = when (current) {
            WizardPage.Welcome -> "Start"
            WizardPage.Done -> "Finish"
            else -> if (granted) "Next" else "Grant"
        },
        back = if (current == WizardPage.Welcome) "Skip" else "Back",
    )
    SideEffect {
        session.back = { goBack() }
        if (session.hints != hints) session.hints = hints
    }

    BackHandler { goBack() }

    val primaryFocus = remember { FocusRequester() }
    LaunchedEffect(current, granted) {
        runCatching { primaryFocus.requestFocus() }
    }

    val chrome = LocalTheme.current.chrome
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val skip = event.key == Key.Y || event.key == Key.ButtonY
                if (skip && grant != null && !granted) {
                    goNext()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 28.dp, vertical = 32.dp)
            .padding(bottom = chrome.bottomBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .tileFrame(false, cardShape(), width = 2.dp)
                .background(Tile, cardShape())
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DiamondMark(32.dp)
                Spacer(Modifier.size(12.dp))
                Text("BlissDeck", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    "${page + 1} / ${pages.size}",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(18.dp))
            when (current) {
                WizardPage.Welcome -> {
                    Text("Set up BlissDeck", color = TextPrimary, fontSize = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Android keeps a few grants behind system screens. This walk-through opens each one so Home, Last Played, closing games, and the notification badge can work.",
                        color = TextMuted,
                        fontSize = 16.sp,
                    )
                }
                WizardPage.Done -> {
                    Text("You're ready", color = TextPrimary, fontSize = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    val missing = SetupGrant.entries.filter { !grants[it] }
                    if (missing.isEmpty()) {
                        Text(
                            "Home, usage access, accessibility, and notification access are on. You can run this again from Settings.",
                            color = TextMuted,
                            fontSize = 16.sp,
                        )
                    } else {
                        Text(
                            "Skipped for now: ${missing.joinToString { it.title }}. Open Settings -> Permissions any time, or run this wizard again.",
                            color = TextMuted,
                            fontSize = 16.sp,
                        )
                    }
                }
                else -> {
                    val step = grant!!
                    Text(step.title, color = TextPrimary, fontSize = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(step.why, color = TextMuted, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(step.hint, color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (granted) "Granted" else if (step.required) "Needed" else "Optional",
                        color = if (granted) PlayGreen else TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SteamPill(
                    label = when (current) {
                        WizardPage.Welcome -> "Get started"
                        WizardPage.Done -> "Start using BlissDeck"
                        else -> if (granted) "Continue" else "Grant"
                    },
                    selected = true,
                    modifier = Modifier.focusRequester(primaryFocus),
                    onClick = { primary() },
                )
                if (grant != null && !granted) {
                    Text(
                        "Skip",
                        color = TextMuted,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .tileClick { goNext() }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
                if (current == WizardPage.Welcome) {
                    Text(
                        "Skip for now",
                        color = TextMuted,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .tileClick { finish() }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
