package com.libyuvresizer

import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExifCopierTest {

    private lateinit var cacheDir: File
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    }

    @After
    fun tearDown() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    private fun createJpegWithExif(name: String, block: ExifInterface.() -> Unit): String {
        val path = TestFixtures.createJpeg(
            InstrumentationRegistry.getInstrumentation().targetContext,
            100, 100, name
        )
        tempFiles += File(path)
        ExifInterface(path).apply(block).saveAttributes()
        return path
    }

    private fun tempDest(name: String): String {
        val f = File(cacheDir, name)
        tempFiles += f
        return f.absolutePath
    }

    @Test
    fun `copy GPS tags are preserved in destination`() {
        val src = createJpegWithExif("exif_src_gps.jpg") {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "48/1,51/1,29/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "2/1,17/1,40/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
        }
        val dest = tempDest("exif_dst_gps.jpg")
        TestFixtures.createJpeg(
            InstrumentationRegistry.getInstrumentation().targetContext,
            50, 50, "exif_dst_gps.jpg"
        ).also { File(it).copyTo(File(dest), overwrite = true) }

        ExifCopier.copy(src, dest)

        val result = ExifInterface(dest)
        assertEquals("48/1,51/1,29/1", result.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertEquals("N", result.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
        assertEquals("2/1,17/1,40/1", result.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        assertEquals("E", result.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
    }

    @Test
    fun `copy make and model are preserved`() {
        val src = createJpegWithExif("exif_src_cam.jpg") {
            setAttribute(ExifInterface.TAG_MAKE, "Google")
            setAttribute(ExifInterface.TAG_MODEL, "Pixel 8")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2024:01:15 10:30:00")
        }
        val dest = tempDest("exif_dst_cam.jpg")
        File(src).copyTo(File(dest), overwrite = true)

        ExifCopier.copy(src, dest)

        val result = ExifInterface(dest)
        assertEquals("Google", result.getAttribute(ExifInterface.TAG_MAKE))
        assertEquals("Pixel 8", result.getAttribute(ExifInterface.TAG_MODEL))
        assertEquals("2024:01:15 10:30:00", result.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
    }

    @Test
    fun `copy orientation is always reset to normal`() {
        val src = createJpegWithExif("exif_src_rot.jpg") {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        }
        val dest = tempDest("exif_dst_rot.jpg")
        File(src).copyTo(File(dest), overwrite = true)

        ExifCopier.copy(src, dest)

        val result = ExifInterface(dest)
        assertEquals(
            ExifInterface.ORIENTATION_NORMAL,
            result.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `copy source with no EXIF succeeds without error`() {
        val src = TestFixtures.createJpeg(
            InstrumentationRegistry.getInstrumentation().targetContext,
            50, 50, "exif_src_noexif.jpg"
        ).also { tempFiles += File(it) }
        val dest = tempDest("exif_dst_noexif.jpg")
        File(src).copyTo(File(dest), overwrite = true)

        ExifCopier.copy(src, dest)

        val result = ExifInterface(dest)
        assertNull(result.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
    }

    @Test
    fun `copy source path does not exist returns silently`() {
        val dest = tempDest("exif_dst_silent.jpg")
        TestFixtures.createJpeg(
            InstrumentationRegistry.getInstrumentation().targetContext,
            50, 50, "exif_dst_silent.jpg"
        ).also { File(it).copyTo(File(dest), overwrite = true); tempFiles += File(it) }

        // Must not throw
        ExifCopier.copy("/nonexistent/path/source.jpg", dest)
    }
}
