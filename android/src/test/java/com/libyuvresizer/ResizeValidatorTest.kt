package com.libyuvresizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResizeValidatorTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private fun validParams(filePath: String, outputPath: String = "") = ResizeParams(
    filePath = filePath,
    targetWidth = 100,
    targetHeight = 100,
    quality = 80,
    rotation = 0,
    mode = "contain",
    outputPath = outputPath,
    filterMode = "bilinear"
  )

  // --- happy path ---

  @Test
  fun `valid params with empty outputPath returns Valid`() {
    val file = tmp.newFile("image.jpg")
    assertEquals(ValidationResult.Valid, ResizeValidator.validate(validParams(file.absolutePath)))
  }

  @Test
  fun `valid params with directory outputPath returns Valid`() {
    val file = tmp.newFile("image.jpg")
    val dir = tmp.newFolder("out")
    assertEquals(ValidationResult.Valid, ResizeValidator.validate(validParams(file.absolutePath, dir.absolutePath)))
  }

  @Test
  fun `all valid rotations accepted`() {
    val file = tmp.newFile("image.jpg")
    for (rot in listOf(0, 90, 180, 270)) {
      val result = ResizeValidator.validate(validParams(file.absolutePath).copy(rotation = rot))
      assertEquals("rotation $rot should be Valid", ValidationResult.Valid, result)
    }
  }

  @Test
  fun `all valid modes accepted`() {
    val file = tmp.newFile("image.jpg")
    for (mode in listOf("contain", "cover", "stretch")) {
      val result = ResizeValidator.validate(validParams(file.absolutePath).copy(mode = mode))
      assertEquals("mode $mode should be Valid", ValidationResult.Valid, result)
    }
  }

  @Test
  fun `all valid filterModes accepted`() {
    val file = tmp.newFile("image.jpg")
    for (fm in listOf("none", "linear", "bilinear", "box")) {
      val result = ResizeValidator.validate(validParams(file.absolutePath).copy(filterMode = fm))
      assertEquals("filterMode $fm should be Valid", ValidationResult.Valid, result)
    }
  }

  @Test
  fun `all valid formats accepted`() {
    val file = tmp.newFile("image.jpg")
    for (fmt in listOf("jpeg", "png", "webp")) {
      val result = ResizeValidator.validate(validParams(file.absolutePath).copy(format = fmt))
      assertEquals("format $fmt should be Valid", ValidationResult.Valid, result)
    }
  }

  @Test
  fun `quality boundary 1 accepted`() {
    val file = tmp.newFile("image.jpg")
    assertEquals(ValidationResult.Valid, ResizeValidator.validate(validParams(file.absolutePath).copy(quality = 1)))
  }

  @Test
  fun `quality boundary 100 accepted`() {
    val file = tmp.newFile("image.jpg")
    assertEquals(ValidationResult.Valid, ResizeValidator.validate(validParams(file.absolutePath).copy(quality = 100)))
  }

  // --- error codes ---

  private fun assertCode(expected: String, params: ResizeParams) {
    val result = ResizeValidator.validate(params)
    assertTrue("expected Invalid, got $result", result is ValidationResult.Invalid)
    assertEquals(expected, (result as ValidationResult.Invalid).code)
  }

  @Test
  fun `missing file returns E_FILE_NOT_FOUND`() {
    assertCode("E_FILE_NOT_FOUND", validParams("/no/such/file.jpg"))
  }

  @Test
  fun `zero width returns E_INVALID_DIMS`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_DIMS", validParams(file.absolutePath).copy(targetWidth = 0))
  }

  @Test
  fun `negative height returns E_INVALID_DIMS`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_DIMS", validParams(file.absolutePath).copy(targetHeight = -1))
  }

  @Test
  fun `quality 0 returns E_INVALID_QUALITY`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_QUALITY", validParams(file.absolutePath).copy(quality = 0))
  }

  @Test
  fun `quality 101 returns E_INVALID_QUALITY`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_QUALITY", validParams(file.absolutePath).copy(quality = 101))
  }

  @Test
  fun `rotation 45 returns E_INVALID_ROTATION`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_ROTATION", validParams(file.absolutePath).copy(rotation = 45))
  }

  @Test
  fun `invalid mode returns E_INVALID_MODE`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_MODE", validParams(file.absolutePath).copy(mode = "fill"))
  }

  @Test
  fun `invalid filterMode returns E_INVALID_FILTER_MODE`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_FILTER_MODE", validParams(file.absolutePath).copy(filterMode = "cubic"))
  }

  @Test
  fun `invalid format returns E_INVALID_FORMAT`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_FORMAT", validParams(file.absolutePath).copy(format = "gif"))
  }

  @Test
  fun `empty format returns E_INVALID_FORMAT`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_FORMAT", validParams(file.absolutePath).copy(format = ""))
  }

  @Test
  fun `nonexistent outputPath returns E_INVALID_OUTPUT_PATH`() {
    val file = tmp.newFile("image.jpg")
    assertCode("E_INVALID_OUTPUT_PATH", validParams(file.absolutePath, "/nonexistent/dir"))
  }

  @Test
  fun `outputPath pointing to file returns E_INVALID_OUTPUT_PATH`() {
    val file = tmp.newFile("image.jpg")
    val notDir = tmp.newFile("output.jpg")
    assertCode("E_INVALID_OUTPUT_PATH", validParams(file.absolutePath, notDir.absolutePath))
  }

  // --- error message content ---

  @Test
  fun `E_FILE_NOT_FOUND message includes path`() {
    val result = ResizeValidator.validate(validParams("/bad/path.jpg")) as ValidationResult.Invalid
    assertTrue(result.message.contains("/bad/path.jpg"))
  }

  @Test
  fun `E_INVALID_ROTATION message includes got value`() {
    val file = tmp.newFile("image.jpg")
    val result = ResizeValidator.validate(validParams(file.absolutePath).copy(rotation = 45)) as ValidationResult.Invalid
    assertTrue(result.message.contains("45"))
  }

  @Test
  fun `E_INVALID_MODE message includes got value`() {
    val file = tmp.newFile("image.jpg")
    val result = ResizeValidator.validate(validParams(file.absolutePath).copy(mode = "fill")) as ValidationResult.Invalid
    assertTrue(result.message.contains("fill"))
  }

  @Test
  fun `E_INVALID_FILTER_MODE message includes got value`() {
    val file = tmp.newFile("image.jpg")
    val result = ResizeValidator.validate(validParams(file.absolutePath).copy(filterMode = "cubic")) as ValidationResult.Invalid
    assertTrue(result.message.contains("cubic"))
  }

  @Test
  fun `E_INVALID_FORMAT message includes got value`() {
    val file = tmp.newFile("image.jpg")
    val result = ResizeValidator.validate(validParams(file.absolutePath).copy(format = "gif")) as ValidationResult.Invalid
    assertTrue(result.message.contains("gif"))
  }
}
