package com.libyuvresizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.ReadableMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibyuvResizerModuleFormatTest {

  private lateinit var module: LibyuvResizerModule
  private lateinit var reactContext: FakeReactContext
  private val createdFiles = mutableListOf<String>()

  @Before
  fun setUp() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    reactContext = FakeReactContext(ctx)
    module = LibyuvResizerModule(reactContext)
  }

  @After
  fun tearDown() {
    createdFiles.forEach { TestFixtures.deleteIfExists(it) }
    createdFiles.clear()
  }

  private fun resize(filePath: String, format: String, quality: Double = 80.0, keepMeta: Boolean = false): FakePromise {
    val promise = FakePromise()
    module.resize(filePath, 100.0, 100.0, quality, 0.0, "contain", "", "box", keepMeta, format, promise)
    return promise
  }

  @Test
  fun resize_formatWebp_outputPathEndsWithWebp() {
    val src = TestFixtures.createJpeg(reactContext, 200, 150, "webp_test.jpg")
    createdFiles += src

    val promise = resize(src, format = "webp")

    assertTrue("expected resolved, got error=${promise.errorCode}: ${promise.errorMessage}", promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output path must end with .webp", outPath.endsWith(".webp"))
  }

  @Test
  fun resize_formatWebp_outputFileIsDecodable() {
    val src = TestFixtures.createJpeg(reactContext, 200, 150, "webp_decode.jpg")
    createdFiles += src

    val promise = resize(src, format = "webp")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output file must exist", File(outPath).exists())
    assertTrue("output file must be non-empty", File(outPath).length() > 0)
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertTrue("decoded WebP width must be > 0", w > 0)
    assertTrue("decoded WebP height must be > 0", h > 0)
  }

  @Test
  fun resize_formatWebp_resultSizeIsPositive() {
    val src = TestFixtures.createJpeg(reactContext, 200, 150, "webp_size.jpg")
    createdFiles += src

    val promise = resize(src, format = "webp")

    assertTrue(promise.resolved)
    val map = promise.result as ReadableMap
    assertTrue("result size must be > 0", map.getDouble("size") > 0)
  }

  @Test
  fun resize_formatWebp_resultNameEndsWithWebp() {
    val src = TestFixtures.createJpeg(reactContext, 200, 150, "webp_name.jpg")
    createdFiles += src

    val promise = resize(src, format = "webp")

    assertTrue(promise.resolved)
    val name = (promise.result as ReadableMap).getString("name")!!
    assertTrue("result name must end with .webp", name.endsWith(".webp"))
  }

  @Test
  fun resize_formatWebp_keepMetaTrue_resolvesWithoutError() {
    val src = TestFixtures.createJpeg(reactContext, 200, 150, "webp_keepmeta.jpg")
    createdFiles += src

    val promise = resize(src, format = "webp", keepMeta = true)

    assertTrue("keepMeta+webp must resolve, got: ${promise.errorCode}: ${promise.errorMessage}", promise.resolved)
    assertFalse(promise.rejected)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output must be .webp", outPath.endsWith(".webp"))
  }

  @Test
  fun resize_formatJpeg_explicit_outputPathEndsWithJpg() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "jpeg_explicit.jpg")
    createdFiles += src

    val promise = resize(src, format = "jpeg")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output path must end with .jpg", outPath.endsWith(".jpg"))
  }

  @Test
  fun resize_formatPng_explicit_outputPathEndsWithPng() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "png_explicit.jpg")
    createdFiles += src

    val promise = resize(src, format = "png")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output path must end with .png", outPath.endsWith(".png"))
  }

  @Test
  fun resize_invalidFormat_rejectsWithInvalidFormat() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "fmt_err.jpg")
    createdFiles += src

    val promise = resize(src, format = "gif")

    assertTrue(promise.rejected)
    assertFalse(promise.resolved)
    assertEquals("E_INVALID_FORMAT", promise.errorCode)
  }
}
