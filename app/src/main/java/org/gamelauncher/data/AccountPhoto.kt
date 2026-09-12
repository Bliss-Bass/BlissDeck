package org.gamelauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AccountPhotoStore(context: Context) {
    private val app = context.applicationContext
    private val file = File(app.filesDir, "account.jpg")
    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> = _epoch

    val hasCustom: Boolean get() = file.isFile && file.length() > 0L

    fun bitmap(): Bitmap? {
        if (hasCustom) {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
        return systemUserBitmap(app)
    }

    fun setFrom(uri: Uri) {
        val original = app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return
        val scaled = original.scaled(256)
        file.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        if (scaled !== original) original.recycle()
        bump()
    }

    fun clearCustom() {
        file.delete()
        bump()
    }

    private fun bump() {
        _epoch.value = _epoch.value + 1
    }
}

val LocalAccountPhoto = staticCompositionLocalOf<AccountPhotoStore> {
    error("AccountPhotoStore not provided")
}

private fun systemUserBitmap(context: Context): Bitmap? {
    val um = context.getSystemService(Context.USER_SERVICE) as UserManager
    val userId = runCatching {
        UserHandle::class.java.getMethod("myUserId").invoke(null) as Int
    }.getOrNull() ?: return null
    return runCatching {
        um.javaClass.getMethod("getUserIcon", Int::class.javaPrimitiveType)
            .invoke(um, userId) as? Bitmap
    }.getOrNull()
}

private fun Bitmap.scaled(max: Int): Bitmap {
    val longest = maxOf(width, height).coerceAtLeast(1)
    if (longest <= max) return this
    val scale = max / longest.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
