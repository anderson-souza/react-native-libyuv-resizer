# keepMeta / EXIF Copy — Tasks

**Design**: `.specs/features/keep-meta-exif/design.md`
**Status**: Done

---

## Execution Plan

### Phase 1: Foundation (Sequential)

Codegen spec first — all other layers depend on it.

```
T1 → T2 → T3
```

- **T1**: Add `keepMeta: boolean` to native spec (`NativeLibyuvResizer.ts`)
- **T2**: Add `keepMeta` dep to `android/build.gradle`
- **T3**: Add `keepMeta: Boolean` to `ResizeParams` data class

### Phase 2: Core Implementation (Parallel after T3)

```
        ┌→ T4 ──────────────────┐
T1+T3 ──┼→ T5 ──────────────────┼──→ T8 → T9
        ├→ T6 ──────────────────┤
        └→ T7 ──────────────────┘
T2 ─────────────────────────────┘
```

- **T4** [P]: Create `ExifCopier.kt`
- **T5** [P]: Wire `keepMeta` into `LibyuvResizerModule.resize()`
- **T6** [P]: Update `resizer.native.tsx` — `ResizeOptions` + bridge call
- **T7** [P]: Update `ios/LibyuvResizer.mm` — accept `keepMeta` param

### Phase 3: Tests + Validation (Sequential)

```
T4+T5+T6+T7 complete → T8 → T9
```

- **T8**: Unit test `ExifCopier.kt` (JVM)
- **T9**: Instrumented integration test `LibyuvResizerModuleExifTest.kt`

---

## Task Breakdown

### T1: Add `keepMeta: boolean` to native Turbo Module spec

**What**: Add 9th positional parameter `keepMeta: boolean` to `resize()` in `NativeLibyuvResizer.ts`
**Where**: `src/NativeLibyuvResizer.ts`
**Depends on**: None
**Reuses**: Existing `resize()` signature pattern

**Done when**:

- [ ] `resize()` in `Spec` interface has `keepMeta: boolean` as 9th param
- [ ] JSDoc updated with `@param keepMeta` description
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```
Expected: no TypeScript errors

---

### T2: Add `androidx.exifinterface` to Android build deps

**What**: Add `implementation("androidx.exifinterface:exifinterface:1.3.7")` to `android/build.gradle`
**Where**: `android/build.gradle`
**Depends on**: None
**Reuses**: Existing `dependencies {}` block pattern

**Done when**:

- [ ] Dependency present in `dependencies {}` block
- [ ] `./gradlew :rn-libyuv-resizer:dependencies` shows `exifinterface` resolved

**Verify**:
```bash
cd android && ./gradlew :rn-libyuv-resizer:dependencies --configuration releaseRuntimeClasspath | grep exifinterface
```
Expected: `androidx.exifinterface:exifinterface:1.3.7` in output

---

### T3: Add `keepMeta: Boolean` to `ResizeParams` data class

**What**: Add `val keepMeta: Boolean = false` as last field in `ResizeParams` data class; update `ResizeValidator.validate()` call site in `LibyuvResizerModule.kt` to pass the new field
**Where**: `android/src/main/java/com/libyuvresizer/ResizeValidator.kt` (data class) + `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt` (call site)
**Depends on**: None (parallel with T1/T2 but must finish before T4/T5)
**Reuses**: Existing `ResizeParams` fields pattern; default value keeps all existing `ResizeParams(...)` constructors valid

**Done when**:

- [ ] `ResizeParams` has `val keepMeta: Boolean = false` as last field
- [ ] `LibyuvResizerModule` constructs `ResizeParams(..., keepMeta = keepMeta)` (once T5 adds the param)
- [ ] Existing JVM unit tests pass: `yarn test --testPathPattern="Validator|Calculator"` or Android equivalent

**Verify**:
```bash
cd android && ./gradlew testDebugUnitTest
```
Expected: all existing unit tests green

---

### T4: Create `ExifCopier.kt`

**What**: New `internal object ExifCopier` with `fun copy(sourcePath: String, destPath: String)` — reads all EXIF tags from source, writes to dest, resets orientation, calls `saveAttributes()`
**Where**: `android/src/main/java/com/libyuvresizer/ExifCopier.kt` (new file)
**Depends on**: T2 (dep available), T3 (not strictly required — no dependency on ResizeParams)
**Reuses**: Design's explicit `TAGS` list from `design.md`

**Implementation**:

```kotlin
package com.libyuvresizer

import androidx.exifinterface.media.ExifInterface
import java.io.IOException

internal object ExifCopier {
    fun copy(sourcePath: String, destPath: String) {
        val src = try { ExifInterface(sourcePath) } catch (_: IOException) { return }
        val dst = ExifInterface(destPath)
        for (tag in TAGS) {
            src.getAttribute(tag)?.let { dst.setAttribute(tag, it) }
        }
        dst.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        dst.saveAttributes()
    }

    private val TAGS = listOf(
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_BITS_PER_SAMPLE,
        ExifInterface.TAG_BRIGHTNESS_VALUE,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_COLOR_SPACE,
        ExifInterface.TAG_COMPONENTS_CONFIGURATION,
        ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
        ExifInterface.TAG_COMPRESSION,
        ExifInterface.TAG_CONTRAST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_CUSTOM_RENDERED,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
        ExifInterface.TAG_DNG_VERSION,
        ExifInterface.TAG_EXIF_VERSION,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_EXPOSURE_INDEX,
        ExifInterface.TAG_EXPOSURE_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_FILE_SOURCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FLASH_ENERGY,
        ExifInterface.TAG_FLASHPIX_VERSION,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
        ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
        ExifInterface.TAG_GAIN_CONTROL,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_INTEROPERABILITY_INDEX,
        ExifInterface.TAG_ISO_SPEED,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SPECIFICATION,
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MAKER_NOTE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_OECF,
        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
        ExifInterface.TAG_PIXEL_X_DIMENSION,
        ExifInterface.TAG_PIXEL_Y_DIMENSION,
        ExifInterface.TAG_RELATED_SOUND_FILE,
        ExifInterface.TAG_RESOLUTION_UNIT,
        ExifInterface.TAG_SATURATION,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
        ExifInterface.TAG_SCENE_TYPE,
        ExifInterface.TAG_SENSING_METHOD,
        ExifInterface.TAG_SHARPNESS,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
        ExifInterface.TAG_SUBJECT_AREA,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
        ExifInterface.TAG_SUBJECT_LOCATION,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_TRANSFER_FUNCTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_WHITE_POINT,
        ExifInterface.TAG_X_RESOLUTION,
        ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
        ExifInterface.TAG_Y_CB_CR_POSITIONING,
        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
        ExifInterface.TAG_Y_RESOLUTION,
        // TAG_ORIENTATION excluded — reset to NORMAL after loop
    )
}
```

**Done when**:

- [ ] File compiles: `./gradlew compileDebugKotlin`
- [ ] `ExifCopier.copy` reads source EXIF silently on `IOException`
- [ ] `ExifCopier.copy` propagates `IOException` from `ExifInterface(destPath)` or `saveAttributes()`
- [ ] `TAG_ORIENTATION` always set to `ORIENTATION_NORMAL` string

**Verify**:
```bash
cd android && ./gradlew compileDebugKotlin 2>&1 | grep -i error
```
Expected: no errors

---

### T5: Wire `keepMeta` into `LibyuvResizerModule.resize()`

**What**: Add `keepMeta: Boolean` parameter to `override fun resize(...)`, include in `ResizeParams`, insert `ExifCopier.copy` call after compress block
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T3 (`ResizeParams` updated), T4 (`ExifCopier` exists)
**Reuses**: Existing try/catch pattern around compress block; existing `ext` local variable

**Key insertion** (after `FileOutputStream(outFile).use { ... }`):

```kotlin
if (params.keepMeta && ext == "jpg") {
    try {
        ExifCopier.copy(filePath, outFile.absolutePath)
    } catch (e: IOException) {
        promise.reject("E_EXIF_WRITE_FAILED", e.message ?: "Failed to write EXIF metadata")
        return
    }
}
```

**Done when**:

- [ ] `override fun resize(...)` signature includes `keepMeta: Boolean` (codegen will enforce match)
- [ ] `ResizeParams` constructed with `keepMeta = keepMeta`
- [ ] `ExifCopier.copy` called only when `params.keepMeta && ext == "jpg"`
- [ ] `promise.reject("E_EXIF_WRITE_FAILED", ...)` on `IOException` from `ExifCopier`
- [ ] `./gradlew compileDebugKotlin` passes

**Verify**:
```bash
cd android && ./gradlew compileDebugKotlin 2>&1 | grep -i error
```

---

### T6: Update `resizer.native.tsx` — `ResizeOptions` + bridge call [P]

**What**: Add `keepMeta?: boolean` to `ResizeOptions`; pass `options?.keepMeta ?? false` as 9th arg to `LibyuvResizer.resize()`
**Where**: `src/resizer.native.tsx`
**Depends on**: T1 (`NativeLibyuvResizer.ts` spec updated)
**Reuses**: Existing optional field pattern (`outputPath?: string`)

**Done when**:

- [ ] `ResizeOptions` has `keepMeta?: boolean` with JSDoc (`@default false; Android-only; no-op on iOS and PNG output`)
- [ ] `LibyuvResizer.resize(...)` call passes `options?.keepMeta ?? false` as 9th argument
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T7: Update `ios/LibyuvResizer.mm` — accept `keepMeta` param [P]

**What**: Add `keepMeta:(BOOL)keepMeta` parameter to `RCT_EXPORT_METHOD`; ignore it (stub unchanged)
**Where**: `ios/LibyuvResizer.mm`
**Depends on**: T1 (spec updated — codegen regenerates iOS spec)
**Reuses**: Existing `RCT_EXPORT_METHOD` signature pattern

**Done when**:

- [ ] `RCT_EXPORT_METHOD(resize:...) ` includes `keepMeta:(BOOL)keepMeta` before `resolve:` and `reject:`
- [ ] Body unchanged (`reject(@"E_NOT_IMPLEMENTED", ...)`)
- [ ] iOS builds without warnings (parameter unused — `(void)keepMeta;` if needed)

**Verify**:
```bash
yarn turbo run build:ios 2>&1 | grep -i error
```
Expected: build succeeds (CI is currently disabled for iOS — manual verify via Xcode if available)

---

### T8: Unit test `ExifCopier.kt` (JVM)

**What**: New `ExifCopierTest.kt` with JVM unit tests covering: copy GPS tags, silent read failure, IOException on write propagates, orientation always reset
**Where**: `android/src/test/java/com/libyuvresizer/ExifCopierTest.kt` (new file)
**Depends on**: T4 (`ExifCopier.kt` exists)
**Reuses**: `TestFixtures` pattern from instrumented tests; write temp JPEG with known EXIF using `ExifInterface`

**Test cases**:

```
1. copy_GPS_tags_preserved — write JPEG with GPS EXIF via ExifInterface, call copy(), read output EXIF, assert lat/lng match
2. copy_orientation_reset_to_normal — source has ORIENTATION_ROTATE_90, after copy output has ORIENTATION_NORMAL
3. copy_make_model_datetime_preserved — camera metadata copied correctly
4. copy_source_no_exif_succeeds — source JPEG with no EXIF → copy() returns without error, output is valid
5. copy_source_unreadable_silent — source path does not exist → copy() returns without throwing
```

**Note**: `ExifInterface(filePath)` works in JVM tests (pure Java I/O). Use `java.io.File.createTempFile()` for output files. Use real JPEG bytes for source (copy from `androidTest/assets/` or generate minimal JPEG).

**Done when**:

- [ ] All 5 test cases implemented and named with backtick descriptors
- [ ] `./gradlew testDebugUnitTest --tests "com.libyuvresizer.ExifCopierTest"` passes

**Verify**:
```bash
cd android && ./gradlew testDebugUnitTest --tests "com.libyuvresizer.ExifCopierTest" --info
```
Expected: `5 tests completed, 0 failed`

---

### T9: Instrumented integration test `LibyuvResizerModuleExifTest.kt`

**What**: New instrumented test class verifying full pipeline: `keepMeta=true` preserves GPS, `keepMeta=false` produces no EXIF, PNG+keepMeta resolves without error
**Where**: `android/src/androidTest/java/com/libyuvresizer/LibyuvResizerModuleExifTest.kt` (new file)
**Depends on**: T5 (module wired), T8 (unit tests green)
**Reuses**: `FakePromise`, `FakeReactContext`, `TestFixtures` from existing instrumented tests; add JPEG-with-GPS-EXIF test asset to `androidTest/assets/`

**Test cases**:

```
1. resize_with_keepMeta_true_preserves_GPS — source JPEG has GPS tags → resize with keepMeta=true → output EXIF has matching GPS lat/lng
2. resize_with_keepMeta_true_resets_orientation — source JPEG has ORIENTATION_ROTATE_90 → output has ORIENTATION_NORMAL
3. resize_with_keepMeta_false_produces_no_exif — keepMeta=false → output JPEG has no GPS or make/model tags
4. resize_with_keepMeta_omitted_produces_no_exif — keepMeta not set (default) → same as above
5. resize_PNG_with_keepMeta_true_resolves — quality=100 (PNG) + keepMeta=true → promise resolves, output is valid PNG
```

**Test asset needed**: Add `gps_photo.jpg` (small JPEG with GPS EXIF) to `android/src/androidTest/assets/`. Use any Creative Commons photo with GPS EXIF or generate via `ExifInterface.setAttribute` in a `@BeforeClass` setup.

**Done when**:

- [ ] All 5 test cases implemented
- [ ] Test asset `gps_photo.jpg` present in `androidTest/assets/`
- [ ] Tests pass on emulator: `./gradlew connectedDebugAndroidTest --tests "com.libyuvresizer.LibyuvResizerModuleExifTest"`

**Verify**:
```bash
cd android && ./gradlew connectedDebugAndroidTest --tests "com.libyuvresizer.LibyuvResizerModuleExifTest"
```
Expected: `5 tests completed, 0 failed`

---

## Parallel Execution Map

```
Phase 1 (can parallelize T1+T2+T3):
  T1 (NativeLibyuvResizer.ts)  ─────────────────────────────────┐
  T2 (build.gradle dep)        ─────────────────────────────────┤
  T3 (ResizeParams field)      ─────────────────────────────────┘
                                                                 ↓
Phase 2 (T1+T3 done → parallel):                        T4 ─┐
  T1 done → T6 [P] (resizer.native.tsx)                 T5 ─┤
  T1 done → T7 [P] (LibyuvResizer.mm)                   T6 ─┼──→ Phase 3
  T2+T3 done → T4 [P] (ExifCopier.kt)                   T7 ─┘
  T3+T4 done → T5 (LibyuvResizerModule.kt)               ↑
                                                         T5 (needs T3+T4)

Phase 3 (all T4-T7 done):
  T8 (ExifCopierTest) → T9 (instrumented)
```

---

## Granularity Check

| Task | Scope | Status |
|---|---|---|
| T1: NativeLibyuvResizer.ts param | 1 file, 1 param | ✅ |
| T2: build.gradle dep | 1 file, 1 line | ✅ |
| T3: ResizeParams field | 1 data class field + 1 call site | ✅ |
| T4: ExifCopier.kt | 1 new file, 1 object | ✅ |
| T5: LibyuvResizerModule wiring | 1 file, 1 insertion point | ✅ |
| T6: resizer.native.tsx | 1 file, 2 changes (type + call) | ✅ |
| T7: LibyuvResizer.mm param | 1 file, 1 param | ✅ |
| T8: ExifCopierTest | 1 test file, 5 cases | ✅ |
| T9: LibyuvResizerModuleExifTest | 1 test file, 5 cases + asset | ✅ |
