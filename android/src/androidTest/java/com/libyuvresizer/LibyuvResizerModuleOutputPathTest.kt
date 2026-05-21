package com.libyuvresizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.react.bridge.ReadableMap
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibyuvResizerModuleOutputPathTest {

  private lateinit var module: LibyuvResizerModule
  private lateinit var reactContext: FakeReactContext
  private val createdFiles = mutableListOf<String>()
  private lateinit var srcPath: String

  @Before
  fun setUp() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    reactContext = FakeReactContext(ctx)
    module = LibyuvResizerModule(reactContext)
    srcPath = TestFixtures.createJpeg(reactContext, 120, 80, "output_path_src.jpg")
    createdFiles += srcPath
  }

  @After
  fun tearDown() {
    createdFiles.forEach { TestFixtures.deleteIfExists(it) }
    createdFiles.clear()
  }

  private fun resize(outputPath: String = "", quality: Double = 80.0, format: String = "jpeg"): FakePromise {
    val promise = FakePromise()
    module.resize(srcPath, 60.0, 60.0, quality, 0.0, "contain", outputPath, "box", false, format, promise)
    return promise
  }

  @Test
  fun resize_emptyOutputPath_writesToCacheDir() {
    val promise = resize(outputPath = "")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue(
      "output must be inside cacheDir",
      outPath.startsWith(reactContext.cacheDir.absolutePath)
    )
    assertTrue(File(outPath).exists())
  }

  @Test
  fun resize_customOutputPath_writesToExactPath() {
    val destFile = File(reactContext.filesDir, "custom-output.jpg")
    val promise = resize(outputPath = destFile.absolutePath)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertEquals("output must be at exact path", destFile.absolutePath, outPath)
    assertTrue(File(outPath).exists())
  }

  @Test
  fun resize_customOutputPath_usesExactFilename() {
    val destFile = File(reactContext.filesDir, "my-resized.jpg")
    val promise = resize(outputPath = destFile.absolutePath)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertEquals("my-resized.jpg", File(outPath).name)
  }

  @Test
  fun resize_formatPng_emptyOutputPath_usesPngExtension() {
    val promise = resize(outputPath = "", format = "png")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output must end with .png", outPath.endsWith(".png"))
    assertTrue(File(outPath).exists())
  }
}
