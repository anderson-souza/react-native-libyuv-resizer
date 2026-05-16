# WebP Output Format Design

**Spec**: `.specs/features/webp-output-format/spec.md`
**Status**: Draft

---

## Architecture Overview

No new layers. Change propagates through the existing fixed-arity bridge:

```
ResizeOptions.format?  ← new optional field
       │
resizer.native.tsx     ← resolves format (explicit OR quality-fallback), validates
       │ (string 'jpeg'|'png'|'webp' added to bridge call)
NativeLibyuvResizer.ts ← bridge spec: +1 positional param `format: string`
       │ (JSI / legacy bridge)
LibyuvResizerModule.kt ← maps format → CompressFormat + extension
       │
Bitmap.compress(WEBP_LOSSY|PNG|JPEG, quality, fos)
```

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
|---|---|---|
| `ResizeParams` | `android/.../ResizeValidator.kt` | Add `format: String` field |
| `ResizeValidator.validate()` | same file | Add format validation rule (VALID_FORMATS set) |
| `LibyuvResizerModule.resize()` | `android/.../LibyuvResizerModule.kt` | Replace quality-based `CompressFormat` branch with format-based map |
| `resolveOutputFile()` | same file | Already receives `ext: String` — no change needed |
| `resizer.native.tsx` | `src/resizer.native.tsx` | Add `OutputFormat` type, `format` option, resolution logic |
| `NativeLibyuvResizer.ts` | `src/NativeLibyuvResizer.ts` | Add `format: string` positional param to `Spec.resize()` |
| `LibyuvResizer.mm` | `ios/LibyuvResizer.mm` | Accept new `format` param; existing JPEG encode is fine |

### Integration Points

| System | Integration Method |
|---|---|
| JS→Native bridge (codegen) | Add `format: string` as positional arg #10; codegen regenerates glue |
| `keepMeta` guard | Change `ext == "jpg"` check to `format == "jpeg"` (more explicit) |
| Android API level | `Bitmap.CompressFormat.WEBP_LOSSY` requires API 30; use `@RequiresApi` + `Build.VERSION.SDK_INT` branch for `WEBP` fallback |

---

## Components

### `OutputFormat` type  (TS)

- **Purpose**: Union type constraining the `format` option
- **Location**: `src/resizer.native.tsx` (alongside `ResizeMode`, `FilterMode`)
- **Interface**:
  ```typescript
  export type OutputFormat = 'jpeg' | 'png' | 'webp';
  ```
- **Reuses**: Same pattern as `ResizeMode` and `FilterMode`

### `ResizeOptions.format` (TS)

- **Purpose**: Explicit output format; when absent, existing quality-based detection applies
- **Location**: `src/resizer.native.tsx` — `ResizeOptions` interface
- **Interface**:
  ```typescript
  /** Output image format. When omitted: quality===100 → png, else → jpeg. */
  format?: OutputFormat;
  ```

### Format resolution logic (TS)

- **Purpose**: Produce an explicit format string for the bridge; never undefined
- **Location**: `src/resizer.native.tsx` — inside `resize()`, before bridge call
- **Interface**:
  ```typescript
  // resolution order: explicit format > quality-fallback
  const resolvedFormat: OutputFormat =
    options?.format ?? (quality === 100 ? 'png' : 'jpeg');
  ```
- **Validation**: Add `VALID_FORMATS` set; reject with `TypeError` if unknown string passed

### `NativeLibyuvResizer.ts` bridge spec

- **Purpose**: Bridge contract — add `format: string` as 10th positional parameter
- **Location**: `src/NativeLibyuvResizer.ts`
- **Interface** (full updated signature):
  ```typescript
  resize(
    filePath: string,
    targetWidth: number,
    targetHeight: number,
    quality: number,
    rotation: number,
    mode: string,
    outputPath: string,
    filterMode: string,
    keepMeta: boolean,
    format: string        // ← new, position 10
  ): Promise<ResizeResult>;
  ```
- **Note**: Adding a positional param breaks every `toHaveBeenCalledWith` in `index.test.tsx` — all expected arg lists must be updated.

### `ResizeParams` (Kotlin)

- **Purpose**: Value object passed to `ResizeValidator` — add `format` field
- **Location**: `android/.../ResizeValidator.kt`
- **Interface**:
  ```kotlin
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
    val format: String = "jpeg"   // ← new
  )
  ```

### `ResizeValidator` (Kotlin)

- **Purpose**: Add `format` validation
- **Location**: `android/.../ResizeValidator.kt`
- **Interface**:
  ```kotlin
  private val VALID_FORMATS = setOf("jpeg", "png", "webp")
  // new validation rule in validate():
  if (params.format !in VALID_FORMATS)
    return ValidationResult.Invalid("E_INVALID_FORMAT",
      "format must be jpeg, png, or webp, got: ${params.format}")
  ```

### `LibyuvResizerModule.kt` encode path

- **Purpose**: Replace quality-based `CompressFormat` branch with format-driven map
- **Location**: `android/.../LibyuvResizerModule.kt`
- **Interface** (encode block replaces lines 98–103 of current file):
  ```kotlin
  val (ext, compressFmt) = formatToExtAndCompressFormat(params.format, q)
  val outFile = resolveOutputFile(filePath, outputPath, ext)
  FileOutputStream(outFile).use { fos ->
    dstBitmap.compress(compressFmt, q, fos)
  }
  ```
- **New private helper** (companion object):
  ```kotlin
  @Suppress("DEPRECATION")
  private fun formatToExtAndCompressFormat(
    format: String,
    quality: Int
  ): Pair<String, Bitmap.CompressFormat> = when (format) {
    "png"  -> "png" to Bitmap.CompressFormat.PNG
    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "webp" to Bitmap.CompressFormat.WEBP_LOSSY
              } else {
                "webp" to Bitmap.CompressFormat.WEBP   // deprecated API 30, still works
              }
    else   -> "jpg" to Bitmap.CompressFormat.JPEG      // "jpeg" + any unvalidated fallback
  }
  ```
- **`keepMeta` guard update**: Change `ext == "jpg"` → `params.format == "jpeg"` (cleaner, format-driven)
- **Note**: `@Suppress("DEPRECATION")` required for the WEBP path on pre-API-30 to avoid compiler warning. The validator ensures only valid strings reach this point.

### `LibyuvResizer.mm` (iOS)

- **Purpose**: Accept new `format` param; silently ignore it (produce JPEG as today)
- **Location**: `ios/LibyuvResizer.mm`
- **Change**: Add `format:(NSString *)format` to both Turbo and legacy bridge signatures; existing JPEG encode unchanged

---

## Data Models

No new models. `ResizeParams` gains one field; `ResizeOptions` gains one optional field.

---

## Error Handling Strategy

| Error Scenario | Handling | User Sees |
|---|---|---|
| `format: 'gif'` (unknown string) | TS: `TypeError` before bridge call | `"Invalid format: 'gif'"` |
| Android: unknown format string reaches native | Validator catches it | `E_INVALID_FORMAT` rejection |
| WebP encode fails (OOM, etc.) | Caught by outer `catch (e: Exception)` | `E_UNKNOWN` with message |

---

## Tech Decisions

| Decision | Choice | Rationale |
|---|---|---|
| `format` position in bridge | Last (position 10) | Least disruptive; appending keeps existing call sites forward-compatible in future |
| API 30 branch for WEBP_LOSSY | `Build.VERSION.SDK_INT >= R` | `WEBP_LOSSY`/`WEBP_LOSSLESS` added in API 30; `WEBP` (deprecated) still works and produces lossy output on older APIs — no behaviour difference for users |
| `keepMeta` check: `format == "jpeg"` not `ext == "jpg"` | format string | Removes implicit coupling between extension string and EXIF logic |
| Format resolution in TS, not native | TS layer | Keeps native layer simple (just a string mapper); JS unit-testable without emulator |
| `quality === 100 → png` fallback preserved | Yes (in TS resolution) | Backward compat — callers not passing `format` keep existing behaviour |
| P3 (`format:'jpeg'/'png'` explicit override) | Included in same milestone | Zero extra code — resolution logic already handles it; no reason to defer |
