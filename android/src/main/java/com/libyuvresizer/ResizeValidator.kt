package com.libyuvresizer

import java.io.File

data class ResizeParams(
  val filePath: String,
  val targetWidth: Int,
  val targetHeight: Int,
  val quality: Int,
  val rotation: Int,
  val mode: String,
  val outputPath: String,
  val filterMode: String,
  val keepMeta: Boolean = false,
  val format: String = "jpeg"
)

sealed class ValidationResult {
  object Valid : ValidationResult()
  data class Invalid(val code: String, val message: String) : ValidationResult()
}

object ResizeValidator {
  private val VALID_MODES = setOf("contain", "cover", "stretch")
  private val VALID_FILTER_MODES = setOf("none", "linear", "bilinear", "box")
  private val VALID_ROTATIONS = setOf(0, 90, 180, 270)
  private val VALID_FORMATS = setOf("jpeg", "png", "webp")

  fun validate(params: ResizeParams): ValidationResult {
    if (!File(params.filePath).exists())
      return ValidationResult.Invalid("E_FILE_NOT_FOUND", "File not found: ${params.filePath}")
    if (params.targetWidth <= 0 || params.targetHeight <= 0)
      return ValidationResult.Invalid("E_INVALID_DIMS", "Invalid dimensions")
    if (params.quality !in 1..100)
      return ValidationResult.Invalid("E_INVALID_QUALITY", "Quality must be between 1 and 100")
    if (params.rotation !in VALID_ROTATIONS)
      return ValidationResult.Invalid(
        "E_INVALID_ROTATION",
        "rotation must be 0, 90, 180 or 270, got: ${params.rotation}"
      )
    if (params.mode !in VALID_MODES)
      return ValidationResult.Invalid(
        "E_INVALID_MODE",
        "mode must be contain, cover, or stretch, got: ${params.mode}"
      )
    if (params.filterMode !in VALID_FILTER_MODES)
      return ValidationResult.Invalid(
        "E_INVALID_FILTER_MODE",
        "filterMode must be none, linear, bilinear, or box, got: ${params.filterMode}"
      )
    if (params.format !in VALID_FORMATS)
      return ValidationResult.Invalid(
        "E_INVALID_FORMAT",
        "format must be jpeg, png, or webp, got: ${params.format}"
      )
    if (params.outputPath.isNotEmpty()) {
      val dir = File(params.outputPath)
      if (!dir.exists())
        return ValidationResult.Invalid(
          "E_INVALID_OUTPUT_PATH",
          "Output directory does not exist: ${params.outputPath}"
        )
      if (!dir.isDirectory)
        return ValidationResult.Invalid(
          "E_INVALID_OUTPUT_PATH",
          "outputPath must be a directory, not a file: ${params.outputPath}"
        )
    }
    return ValidationResult.Valid
  }
}
