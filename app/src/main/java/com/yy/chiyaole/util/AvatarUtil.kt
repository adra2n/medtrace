package com.yy.chiyaole.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.util.UUID

// 将选中头像复制到 app 私有 filesDir/avatars/ 下，返回绝对路径。
// 复制而非引用 Uri，避免相册/相机 Uri 失效导致头像丢失。
fun copyAvatarToInternal(context: Context, uri: Uri): String? = try {
    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } ?: return null

    val dir = File(context.applicationContext.filesDir, "avatars")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "avatar_${UUID.randomUUID()}.jpg")

    val w = bmp.width
    val h = bmp.height
    val scale = minOf(1f, 512f / maxOf(w, h))
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (w * scale).toInt(), (h * scale).toInt(), true) else bmp
    file.outputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    if (scaled != bmp) scaled.recycle()
    file.absolutePath
} catch (e: Exception) {
    null
}
