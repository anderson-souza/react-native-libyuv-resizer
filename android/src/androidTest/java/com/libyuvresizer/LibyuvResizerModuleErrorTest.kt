package com.libyuvresizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibyuvResizerModuleErrorTest {

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

  private fun resize(
    filePath: String = "/nonexistent/file.jpg",
    targetW: Double = 100.0,
    targetH: Double = 100.0,
    quality: Double = 80.0,
    rotation: Double = 0.0,
    mode: String = "contain",
    outputPath: String = "",
    filterMode: String = "box"
  ): FakePromise {
    val promise = FakePromise()
    module.resize(filePath, targetW, targetH, quality, rotation, mode, outputPath, filterMode, false, promise)
    return promise
  }

  @Test
  fun resize_nonexistentFile_rejectsWithFileNotFound() {
    val promise = resize(filePath = "/nonexistent/path/image.jpg")

    assertTrue(promise.rejected)
    assertFalse(promise.resolved)
    assertEquals("E_FILE_NOT_FOUND", promise.errorCode)
  }

  @Test
  fun resize_corruptFile_rejectsWithDecodeFailed() {
    val corrupt = TestFixtures.createCorruptFile(reactContext, "corrupt.jpg")
    createdFiles += corrupt

    val promise = resize(filePath = corrupt)

    assertTrue(promise.rejected)
    assertEquals("E_DECODE_FAILED", promise.errorCode)
  }

  @Test
  fun resize_zeroDimensions_rejectsWithInvalidDims() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "dims_test.jpg")
    createdFiles += src

    val promise = resize(filePath = src, targetW = 0.0, targetH = 100.0)

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_DIMS", promise.errorCode)
  }

  @Test
  fun resize_outOfRangeQuality_rejectsWithInvalidQuality() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "quality_test.jpg")
    createdFiles += src

    val promise = resize(filePath = src, quality = 0.0)

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_QUALITY", promise.errorCode)
  }

  @Test
  fun resize_invalidMode_rejectsWithInvalidMode() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "mode_test.jpg")
    createdFiles += src

    val promise = resize(filePath = src, mode = "fill")

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_MODE", promise.errorCode)
  }

  @Test
  fun resize_invalidFilterMode_rejectsWithInvalidFilterMode() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "filter_test.jpg")
    createdFiles += src

    val promise = resize(filePath = src, filterMode = "lanczos")

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_FILTER_MODE", promise.errorCode)
  }

  @Test
  fun resize_invalidRotation_rejectsWithInvalidRotation() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "rot_test.jpg")
    createdFiles += src

    val promise = resize(filePath = src, rotation = 45.0)

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_ROTATION", promise.errorCode)
  }

  @Test
  fun resize_outputPathIsFile_rejectsWithInvalidOutputPath() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "outpath_test.jpg")
    createdFiles += src

    // outputPath points to a file, not a directory
    val promise = resize(filePath = src, outputPath = src)

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_OUTPUT_PATH", promise.errorCode)
  }
}
