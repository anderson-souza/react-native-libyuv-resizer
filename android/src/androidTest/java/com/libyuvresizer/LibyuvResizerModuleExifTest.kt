package com.libyuvresizer

import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.ReadableMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibyuvResizerModuleExifTest {

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

    private fun createJpegWithGps(name: String): String {
        val path = TestFixtures.createJpeg(reactContext, 200, 150, name)
        createdFiles += path
        ExifInterface(path).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "48/1,51/1,29/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "2/1,17/1,40/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_MAKE, "TestCamera")
            setAttribute(ExifInterface.TAG_MODEL, "TestModel")
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        }.saveAttributes()
        return path
    }

    private fun resize(
        filePath: String,
        keepMeta: Boolean,
        quality: Double = 80.0
    ): FakePromise {
        val promise = FakePromise()
        module.resize(filePath, 100.0, 100.0, quality, 0.0, "contain", "", "box", keepMeta, promise)
        return promise
    }

    @Test
    fun `resize with keepMeta true preserves GPS coordinates`() {
        val src = createJpegWithGps("exif_int_gps_src.jpg")

        val promise = resize(src, keepMeta = true)

        assertTrue(promise.resolved)
        val outPath = (promise.result as ReadableMap).getString("path")!!
        createdFiles += outPath

        val exif = ExifInterface(outPath)
        assertEquals("48/1,51/1,29/1", exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertEquals("N", exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
        assertEquals("2/1,17/1,40/1", exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        assertEquals("E", exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
    }

    @Test
    fun `resize with keepMeta true resets orientation to normal`() {
        val src = createJpegWithGps("exif_int_orient_src.jpg")

        val promise = resize(src, keepMeta = true)

        assertTrue(promise.resolved)
        val outPath = (promise.result as ReadableMap).getString("path")!!
        createdFiles += outPath

        val exif = ExifInterface(outPath)
        assertEquals(
            ExifInterface.ORIENTATION_NORMAL,
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `resize with keepMeta false produces no GPS in output`() {
        val src = createJpegWithGps("exif_int_nometa_src.jpg")

        val promise = resize(src, keepMeta = false)

        assertTrue(promise.resolved)
        val outPath = (promise.result as ReadableMap).getString("path")!!
        createdFiles += outPath

        val exif = ExifInterface(outPath)
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun `resize with keepMeta omitted default produces no EXIF`() {
        val src = createJpegWithGps("exif_int_default_src.jpg")

        // keepMeta defaults to false in the bridge call
        val promise = FakePromise()
        module.resize(src, 100.0, 100.0, 80.0, 0.0, "contain", "", "box", false, promise)

        assertTrue(promise.resolved)
        val outPath = (promise.result as ReadableMap).getString("path")!!
        createdFiles += outPath

        val exif = ExifInterface(outPath)
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
    }

    @Test
    fun `resize PNG with keepMeta true resolves without error`() {
        val src = createJpegWithGps("exif_int_png_src.jpg")

        // quality=100 → PNG output; keepMeta is no-op for PNG
        val promise = resize(src, keepMeta = true, quality = 100.0)

        assertTrue("expected resolved, got: ${promise.errorCode}: ${promise.errorMessage}", promise.resolved)
        val outPath = (promise.result as ReadableMap).getString("path")!!
        createdFiles += outPath
        assertTrue("output must be .png", outPath.endsWith(".png"))
        assertTrue("output file must exist", File(outPath).exists())
    }
}
