package com.libyuvresizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object TestFixtures {

  fun createJpeg(context: Context, width: Int, height: Int, name: String): String {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.BLUE)
    }
    val file = File(context.cacheDir, name)
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    bmp.recycle()
    return file.absolutePath
  }

  fun createPng(context: Context, width: Int, height: Int, name: String): String {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.GREEN)
    }
    val file = File(context.cacheDir, name)
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 0, it) }
    bmp.recycle()
    return file.absolutePath
  }

  fun createCorruptFile(context: Context, name: String): String {
    val file = File(context.cacheDir, name)
    file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00, 0x01, 0x02))
    return file.absolutePath
  }

  fun decodeDimensions(path: String): Pair<Int, Int> {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    return Pair(opts.outWidth, opts.outHeight)
  }

  fun deleteIfExists(path: String?) {
    path?.let { File(it).delete() }
  }
}
