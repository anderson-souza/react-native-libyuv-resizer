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
class LibyuvResizerModuleIntegrationTest {

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
    filePath: String,
    targetW: Double,
    targetH: Double,
    quality: Double,
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
  fun resize_landscape_contain_returnsScaledJpeg() {
    // 200x100 → target 300x300 contain → scale=min(1.5,3.0)=1.5 → floor(300)x floor(150) = 300x150
    val src = TestFixtures.createJpeg(reactContext, 200, 100, "landscape.jpg")
    createdFiles += src

    val promise = resize(src, 300.0, 300.0, 80.0)

    assertTrue("expected resolved, got error=${promise.errorCode}: ${promise.errorMessage}", promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output file must exist", File(outPath).exists())
    assertTrue("output must be .jpg", outPath.endsWith(".jpg"))
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(300, w)
    assertEquals(150, h)
  }

  @Test
  fun resize_portrait_cover_producesLargerThanTarget() {
    // 100x200 → target 150x150 cover → scale=max(1.5,0.75)=1.5 → round(150)x round(300) = 150x300
    val src = TestFixtures.createJpeg(reactContext, 100, 200, "portrait.jpg")
    createdFiles += src

    val promise = resize(src, 150.0, 150.0, 80.0, mode = "cover")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(150, w)
    assertEquals(300, h)
  }

  @Test
  fun resize_stretch_ignoresAspectRatio() {
    // 200x100 → target 300x150 stretch → output exactly 300x150
    val src = TestFixtures.createJpeg(reactContext, 200, 100, "landscape_stretch.jpg")
    createdFiles += src

    val promise = resize(src, 300.0, 150.0, 80.0, mode = "stretch")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(300, w)
    assertEquals(150, h)
  }

  @Test
  fun resize_quality100_writesPng() {
    // quality=100 → PNG format, extension .png in output path (empty outputPath → cacheDir with UUID)
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "square.jpg")
    createdFiles += src

    val promise = resize(src, 50.0, 50.0, 100.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output must be .png", outPath.endsWith(".png"))
    val decoded = TestFixtures.decodeDimensions(outPath)
    assertTrue("decoded PNG must be readable", decoded.first > 0 && decoded.second > 0)
  }

  @Test
  fun resize_quality80_writesJpeg() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "square_jpeg.jpg")
    createdFiles += src

    val promise = resize(src, 50.0, 50.0, 80.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output must be .jpg", outPath.endsWith(".jpg"))
    assertTrue("decoded JPEG must be readable", TestFixtures.decodeDimensions(outPath).first > 0)
  }

  @Test
  fun resize_smallImageToLargeTarget_contain_upscales() {
    // 64x64 → 400x400 contain → scale=min(400/64, 400/64)=6.25 → floor(400)x floor(400) = 400x400
    val src = TestFixtures.createJpeg(reactContext, 64, 64, "tiny.jpg")
    createdFiles += src

    val promise = resize(src, 400.0, 400.0, 80.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(400, w)
    assertEquals(400, h)
  }

  @Test
  fun resize_pngSourceFile_succeeds() {
    val src = TestFixtures.createPng(reactContext, 120, 80, "source.png")
    createdFiles += src

    val promise = resize(src, 60.0, 60.0, 80.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue(File(outPath).exists())
  }
}
