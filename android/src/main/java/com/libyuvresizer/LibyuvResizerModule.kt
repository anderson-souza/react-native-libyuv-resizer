package com.libyuvresizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import androidx.core.graphics.createBitmap

@ReactModule(name = LibyuvResizerModule.NAME)
class LibyuvResizerModule(reactContext: ReactApplicationContext) :
  NativeLibyuvResizerSpec(reactContext) {

  companion object {
    const val NAME = NativeLibyuvResizerSpec.NAME

    private val FILTER_MODE_MAP = mapOf("none" to 0, "linear" to 1, "bilinear" to 2, "box" to 3)

    init {
      System.loadLibrary("libyuvresizer")
    }
  }

  private external fun nativeResize(srcBitmap: Bitmap, dstBitmap: Bitmap, filterMode: Int)
  private external fun nativeResizeAndRotate(
    srcBitmap: Bitmap,
    dstBitmap: Bitmap,
    rotation: Int,
    filterMode: Int
  )

  override fun resize(
    filePath: String,
    targetWidth: Double,
    targetHeight: Double,
    quality: Double,
    rotation: Double,
    mode: String,
    outputPath: String,
    filterMode: String,
    keepMeta: Boolean,
    promise: Promise
  ) {
    try {
      val targetW = targetWidth.toInt()
      val targetH = targetHeight.toInt()
      val q = quality.toInt()
      val rot = rotation.toInt()

      val params = ResizeParams(filePath, targetW, targetH, q, rot, mode, outputPath, filterMode, keepMeta)
      when (val result = ResizeValidator.validate(params)) {
        is ValidationResult.Invalid -> {
          promise.reject(result.code, result.message)
          return
        }
        is ValidationResult.Valid -> Unit
      }

      val bitmapConfig = Bitmap.Config.ARGB_8888

      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(filePath, boundsOpts)

      val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize =
          DimensionCalculator.calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetW, targetH)
        inPreferredConfig = bitmapConfig
      }
      val srcBitmap = BitmapFactory.decodeFile(filePath, decodeOpts)
        ?: run {
          promise.reject("E_DECODE_FAILED", "Failed to decode image")
          return
        }

      // srcBitmap must be recycled even if dstBitmap allocation throws OOM
      try {
        val srcW = srcBitmap.width.toDouble()
        val srcH = srcBitmap.height.toDouble()

        // cover scales to fill target on both axes — no cropping applied by design
        val (dstW, dstH) = DimensionCalculator.computeDstDims(srcW, srcH, targetW, targetH, rot, mode)

        val dstBitmap = createBitmap(dstW, dstH, bitmapConfig)
        try {
          val filterModeInt = FILTER_MODE_MAP.getValue(filterMode)
          if (rot == 0) {
            nativeResize(srcBitmap, dstBitmap, filterModeInt)
          } else {
            nativeResizeAndRotate(srcBitmap, dstBitmap, rot, filterModeInt)
          }

          val ext = if (q == 100) "png" else "jpg"
          val outFile = resolveOutputFile(filePath, outputPath, ext)
          FileOutputStream(outFile).use { fos ->
            val fmt = if (q == 100) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            dstBitmap.compress(fmt, q, fos)
          }

          if (params.keepMeta && ext == "jpg") {
            try {
              ExifCopier.copy(filePath, outFile.absolutePath)
            } catch (e: IOException) {
              promise.reject("E_EXIF_WRITE_FAILED", e.message ?: "Failed to write EXIF metadata")
              return
            }
          }

          val result = Arguments.createMap().apply {
            putString("path", outFile.absolutePath)
            putString("uri", Uri.fromFile(outFile).toString())
            putDouble("size", outFile.length().toDouble())
            putString("name", outFile.name)
            putInt("width", dstBitmap.width)
            putInt("height", dstBitmap.height)
          }
          promise.resolve(result)
        } finally {
          dstBitmap.recycle()
        }
      } finally {
        srcBitmap.recycle()
      }
    } catch (e: Exception) {
      promise.reject("E_UNKNOWN", e.message ?: "Unknown error")
    }
  }

  private fun resolveOutputFile(inputFilePath: String, outputPath: String, ext: String): File {
    if (outputPath.isEmpty()) {
      return File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
    }
    return File(outputPath, File(inputFilePath).name)
  }

}
