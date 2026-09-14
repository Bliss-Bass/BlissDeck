package org.gamelauncher.data

import android.content.Context
import java.io.File

/**
 * Credential-encrypted [Context.filesDir] is missing on some first-boot / HOME
 * installs (`ceDataInode=0`). Theme writes then throw FileNotFoundException.
 * Prefer CE when it exists; otherwise use device-protected storage.
 */
fun Context.appStorage(): Context {
    val preferred = filesDir
    if (preferred.mkdirs() || preferred.isDirectory) return this
    val de = createDeviceProtectedStorageContext()
    de.filesDir.mkdirs()
    return de
}

internal fun pickWritableFilesDir(preferred: File, fallback: File): File {
    if (preferred.mkdirs() || preferred.isDirectory) return preferred
    fallback.mkdirs()
    return fallback
}
