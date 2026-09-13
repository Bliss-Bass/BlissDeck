package org.gamelauncher.data

import android.content.pm.ApplicationInfo
import android.os.Build

object MediaApps {
    private val packages = setOf(
        "com.google.android.youtube",
        "com.google.android.youtube.tv",
        "com.google.android.apps.youtube.unplugged",
        "com.google.android.apps.youtube.kids",
        "com.google.android.videos",
        "com.netflix.mediaclient",
        "com.netflix.ninja",
        "com.plexapp.android",
        "com.hulu.plus",
        "com.hulu.livingroomplus",
        "com.disney.disneyplus",
        "com.disney.datg.videoplatforms.android.amazon",
        "com.amazon.avod.thirdpartyclient",
        "com.amazon.amazonvideo.livingroom",
        "com.hbo.hbonow",
        "com.wbd.stream",
        "com.max.android",
        "com.peacocktv.peacockandroid",
        "com.paramount.android.pplus",
        "com.discovery.discoveryplus.mobile",
        "com.tubitv",
        "com.crunchyroll.crunchyroid",
        "tv.twitch.android.app",
        "org.videolan.vlc",
        "org.xbmc.kodi",
        "org.jellyfin.mobile",
        "org.jellyfin.androidtv",
        "com.mb.android",
        "com.stremio.one",
        "com.google.android.apps.tv.launcherx",
    )

    private val hints = listOf(
        "youtube",
        "netflix",
        "plexapp",
        "hulu",
        "disneyplus",
        "disney.plus",
        "amazon.avod",
        "amazonvideo",
        "hbonow",
        "hbo.max",
        "peacock",
        "paramount",
        "discoveryplus",
        "crunchyroll",
        "tubitv",
        "jellyfin",
        "videolan.vlc",
        "xbmc.kodi",
        "stremio",
        "twitch.android",
        "android.videos",
    )

    fun detect(info: ApplicationInfo): Boolean {
        if (isVideoCategory(info)) return true
        return isKnown(info.packageName)
    }

    fun isKnown(packageName: String): Boolean {
        if (packageName in packages) return true
        val lower = packageName.lowercase()
        return hints.any { it in lower }
    }

    private fun isVideoCategory(info: ApplicationInfo): Boolean {
        if (Build.VERSION.SDK_INT < 26) return false
        return info.category == ApplicationInfo.CATEGORY_VIDEO
    }
}
