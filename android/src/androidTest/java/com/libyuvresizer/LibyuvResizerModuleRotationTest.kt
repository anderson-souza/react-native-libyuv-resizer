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
class LibyuvResizerModuleRotationTest {

  private lateinit var module: LibyuvResizerModule
  private lateinit var reactContext: FakeReactContext
  private val createdFiles = mutableListOf<String>()

  // 200x100 landscape source used in all rotation tests (no inSampleSize reduction at small targets)
  private lateinit var landscapeSrc: String

  @Before
  fun setUp() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    reactContext = FakeReactContext(ctx)
    module = LibyuvResizerModule(reactContext)
    landscapeSrc = TestFixtures.createJpeg(reactContext, 200, 100, "rotation_src.jpg")
    createdFiles += landscapeSrc
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
    rotation: Double,
    mode: String = "contain"
  ): FakePromise {
    val promise = FakePromise()
    module.resize(filePath, targetW, targetH, 80.0, rotation, mode, "", "box", false, "jpeg", promise)
    return promise
  }

  @Test
  fun resize_rotation0_usesNativeResizeBranch() {
    // rot == 0 → nativeResize path. 200x100 → 300x300 contain → 300x150
    val promise = resize(landscapeSrc, 300.0, 300.0, 0.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue("output file must exist", File(outPath).exists())
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(300, w)
    assertEquals(150, h)
  }

  @Test
  fun resize_rotation90_swapsDimensions() {
    // rot=90 → nativeResizeAndRotate path.
    // srcBitmap 200x100, effectiveW=srcH=100, effectiveH=srcW=200.
    // contain: scale=min(300/100, 300/200)=min(3.0,1.5)=1.5 → (floor(150), floor(300)) = 150x300
    val promise = resize(landscapeSrc, 300.0, 300.0, 90.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(150, w)
    assertEquals(300, h)
  }

  @Test
  fun resize_rotation180_doesNotSwapDimensions() {
    // rot=180 → effectiveW=srcW=200, effectiveH=srcH=100 (no swap).
    // contain: scale=min(300/200, 300/100)=1.5 → (floor(300), floor(150)) = 300x150 (same as rot=0)
    val promise = resize(landscapeSrc, 300.0, 300.0, 180.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(300, w)
    assertEquals(150, h)
  }

  @Test
  fun resize_rotation270_swapsDimensions() {
    // rot=270 → same swap logic as 90 → 150x300
    val promise = resize(landscapeSrc, 300.0, 300.0, 270.0)

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(150, w)
    assertEquals(300, h)
  }

  @Test
  fun resize_rotation90_stretch_targetDimsIgnoreRotation() {
    // stretch with rotation: target dims are returned as-is (ignores rotation).
    // 200x100 → target 120x80 stretch → output 120x80 regardless of rotation.
    val promise = resize(landscapeSrc, 120.0, 80.0, 90.0, mode = "stretch")

    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertEquals(120, w)
    assertEquals(80, h)
  }
}
