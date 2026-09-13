package org.gamelauncher.data

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

enum class SetupGrant(
    val title: String,
    val why: String,
    val hint: String,
    val required: Boolean,
) {
    Home(
        title = "Set as Home",
        why = "BlissDeck can own the Home button and the landscape desktop. Pick BlissDeck in the chooser.",
        hint = "Choose BlissDeck, then return here.",
        required = true,
    ),
    Usage(
        title = "Usage access",
        why = "Last Played and the sidebar list of running games need usage access.",
        hint = "Find BlissDeck, allow usage access, then return here.",
        required = true,
    ),
    Accessibility(
        title = "Accessibility",
        why = "Lets BlissDeck see open freeform windows, switch to them, and close a game from its page.",
        hint = "Find BlissDeck in Accessibility, turn it on, then return here.",
        required = true,
    ),
    Notifications(
        title = "Notification access",
        why = "Shows an unread count next to Wi-Fi and battery. Skip this if you do not want a badge.",
        hint = "Allow BlissDeck to read notifications, then return here.",
        required = false,
    ),
}

data class SetupGrants(
    val home: Boolean,
    val usage: Boolean,
    val accessibility: Boolean,
    val notifications: Boolean,
) {
    operator fun get(grant: SetupGrant): Boolean = when (grant) {
        SetupGrant.Home -> home
        SetupGrant.Usage -> usage
        SetupGrant.Accessibility -> accessibility
        SetupGrant.Notifications -> notifications
    }

    val allOn: Boolean
        get() = home && usage && accessibility && notifications
}

object LauncherPermissions {
    fun snapshot(context: Context) = SetupGrants(
        home = isDefaultHomeApp(context),
        usage = GameSession.hasUsageAccess(context),
        accessibility = CloseGameService.isEnabled(context),
        notifications = NotificationCountService.isEnabled(context),
    )

    fun open(context: Context, grant: SetupGrant) {
        when (grant) {
            SetupGrant.Home -> openHomeChooser(context)
            SetupGrant.Usage -> openUsageAccess(context)
            SetupGrant.Accessibility -> openAccessibility(context)
            SetupGrant.Notifications -> openNotificationListener(context)
        }
    }

    fun openHomeChooser(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val roles = context.getSystemService(RoleManager::class.java)
                if (roles != null &&
                    roles.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roles.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    context.startSettings(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                }
            }
            context.startSettings(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    fun openUsageAccess(context: Context) {
        context.startSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openAccessibility(context: Context) {
        context.startSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openNotificationListener(context: Context) {
        NotificationCountService.openSettings(context)
    }
}

internal fun Context.startSettings(intent: Intent) {
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
