package com.libyuvresizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.react.bridge.ReadableMap
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibyuvResizerModuleFilterModeTest {

  private lateinit var module: LibyuvResizerModule
  private lateinit var reactContext: FakeReactContext
  private val createdFiles = mutableListOf<String>()
  private lateinit var srcPath: String

  @Before
  fun setUp() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    reactContext = FakeReactContext(ctx)
    module = LibyuvResizerModule(reactContext)
    srcPath = TestFixtures.createJpeg(reactContext, 120, 80, "filter_src.jpg")
    createdFiles += srcPath
  }

  @After
  fun tearDown() {
    createdFiles.forEach { TestFixtures.deleteIfExists(it) }
    createdFiles.clear()
  }

  private fun resizeWithFilter(filterMode: String): FakePromise {
    val promise = FakePromise()
    module.resize(srcPath, 60.0, 60.0, 80.0, 0.0, "contain", "", filterMode, false, "jpeg", promise)
    return promise
  }

  @Test
  fun resize_filterModeBox_producesValidOutput() {
    val promise = resizeWithFilter("box")
    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue(File(outPath).exists())
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertTrue(w > 0 && h > 0)
  }

  @Test
  fun resize_filterModeBilinear_producesValidOutput() {
    val promise = resizeWithFilter("bilinear")
    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue(File(outPath).exists())
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertTrue(w > 0 && h > 0)
  }

  @Test
  fun resize_filterModeLinear_producesValidOutput() {
    val promise = resizeWithFilter("linear")
    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue(File(outPath).exists())
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertTrue(w > 0 && h > 0)
  }

  @Test
  fun resize_filterModeNone_producesValidOutput() {
    val promise = resizeWithFilter("none")
    assertTrue(promise.resolved)
    val outPath = (promise.result as ReadableMap).getString("path")!!
    createdFiles += outPath
    assertTrue(File(outPath).exists())
    val (w, h) = TestFixtures.decodeDimensions(outPath)
    assertTrue(w > 0 && h > 0)
  }
}
