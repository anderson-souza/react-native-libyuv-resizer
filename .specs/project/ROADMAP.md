# Roadmap

**Current Milestone:** M3 — Rotate API (Android done, iOS pending)  
**Status:** Android fully complete; iOS native implementation next

---

## M1 — Native Foundation ✅ COMPLETE

**Goal:** Working native build with libyuv wired up on Android  
**Delivered:** libyuv submodule added, CMake builds green, JNI call round-trip verified

### Features

**libyuv Android Integration** - DONE

- libyuv git submodule at `libyuv/` (android.googlesource.com)
- `android/CMakeLists.txt` with `add_subdirectory` + link `yuv` target
- `android/build.gradle` with `externalNativeBuild` + NDK flags
- Builds for all 4 ABIs (armeabi-v7a, x86, x86_64, arm64-v8a)

**iOS Accelerate Baseline** - PLANNED

- Podspec already references Accelerate; implementation stub in `LibyuvResizer.mm`
- Full implementation deferred; iOS CI job disabled (`if: false`)

---

## M2 — Resize API ✅ COMPLETE (Android)

**Goal:** Public `resize` API usable from JS — accepts URI, target dimensions, options, returns URI

### Features

**JS/TS API Contract** - DONE

- `resize(filePath, w, h, quality, options?)` in `resizer.native.tsx`
- `ResizeOptions`: `rotation`, `mode` (contain/cover/stretch), `filterMode` (none/linear/bilinear/box), `outputPath`
- Web fallback throws with clear message
- Exported from `index.tsx`

**Android Resize Implementation** - DONE

- Decode input JPEG/PNG with `BitmapFactory` (inSampleSize optimization)
- `DimensionCalculator.computeDstDims` → contain/cover/stretch output dimensions
- C++ JNI: `libyuv::ARGBScale` with configurable filter (0–3)
- Encode result as JPEG or PNG (quality=100 → PNG)
- Resolve promise with output absolute path

**iOS Resize Implementation** - PLANNED

**Unit Tests** - DONE

- JS: `NativeLibyuvResizer.test.ts` covers arch guard + legacy bridge fallback
- Android JVM: `ResizeValidatorTest`, `DimensionCalculatorTest`
- Android instrumented: full module integration tests (error, filter, outputPath, rotation)
- 85% Jest coverage threshold enforced

---

## M3 — Rotate API ✅ COMPLETE (Android)

**Goal:** Rotation integrated into `resize` call — single native roundtrip

### Features

**JS/TS API Contract** - DONE

- `rotation?: RotationAngle` in `ResizeOptions` (0 | 90 | 180 | 270 | negative equivalents)
- Negative angles normalised to positive in `resizer.native.tsx`

**Android Rotate Implementation** - DONE

- `nativeResizeAndRotate` JNI call — scale-first then `libyuv::ARGBRotate`
- Dimension swap handled by `DimensionCalculator` for 90°/270° rotations
- Single JNI call (not two)

**iOS Rotate Implementation** - PLANNED

---

## M3.5 — Legacy Bridge Support ✅ COMPLETE

**Goal:** Library works with both New Architecture and legacy bridge

### Features

**JS arch guard** - DONE

- `globalThis.__turboModuleProxy` runtime check in `NativeLibyuvResizer.ts`
- Falls back to `NativeModules.LibyuvResizer` with clear link error

**Android dual-arch** - DONE

- `TurboReactPackage` replaces `BaseReactPackage`
- `@ReactModule` annotation on `LibyuvResizerModule`
- `IS_NEW_ARCHITECTURE_ENABLED` buildConfigField from host app's Gradle property

**iOS dual-compat** - DONE

- `RCT_EXPORT_MODULE` + `RCT_EXPORT_METHOD` alongside JSI `getTurboModule:`

---

## M4 — iOS Native Implementation

**Goal:** Full `resize` + rotate on iOS via Accelerate / vImage

### Features

**iOS Resize + Rotate** - PLANNED

- Decode input URI to `vImage_Buffer`
- Call `vImageScale_ARGB8888`
- `vImageRotate90_ARGB8888` for 90°/270°; matrix rotation for 180°
- Encode to JPEG/PNG, write to temp file, resolve promise
- Re-enable `build-ios` CI job

---

## M4.5 — keepMeta / EXIF Copy

**Goal:** Opt-in EXIF metadata preservation from source JPEG to resized JPEG output  
**Status:** Spec complete — design + tasks pending

### Features

**keepMeta option (Android)** - PLANNED

- `keepMeta?: boolean` added to `ResizeOptions` (default `false`)
- Android: `ExifInterface` reads all tags from source → copies to output JPEG after `Bitmap.compress`
- `TAG_ORIENTATION` reset to `ORIENTATION_NORMAL` post-copy (image already decoded correctly)
- PNG output (`quality=100`) + `keepMeta: true` → silent no-op, Promise resolves
- iOS `keepMeta: true` → silent no-op (iOS still stub)
- `androidx.exifinterface:exifinterface` added as `implementation` dep

---

## M4.6 — WebP Output Format ✅ COMPLETE

**Goal:** Opt-in WebP lossy output via explicit `format` option in `ResizeOptions`

### Features

**WebP output (Android)** - DONE

- `format?: 'jpeg' | 'png' | 'webp'` added to `ResizeOptions`
- `OutputFormat` type exported from `resizer.native.tsx`
- Format resolution in TS: explicit `format` wins; fallback: `quality===100→'png'`, else `'jpeg'`
- Android: `formatToExtAndCompressFormat()` maps format → `Bitmap.CompressFormat`; API 30+ uses `WEBP_LOSSY`, older uses deprecated `WEBP`
- `keepMeta + format='webp'` → silent no-op (consistent with PNG)
- iOS: accepts `format` param, silently ignores it (stub behaviour unchanged)

---

## M5 — Quality & Release

**Goal:** Library publishable to npm with docs, tests, and CI all green  
**Target:** v0.1.0 on npm

### Features

**Documentation** - PARTIAL

- README updated with full API reference
- TSDoc on all public symbols

**Release Pipeline** - PLANNED

- Verify `release-it` + conventional changelog flow
- Publish v0.1.0 to npm

---

## Future Considerations

- Crop API (after rotate ships — shares decode/encode pipeline)
- Batch operations (resize + rotate in single native call)
- YUV↔RGB conversion as standalone API (camera frame use case)
- Expo plugin for managed workflow
- Performance benchmarks vs. alternative RN image libs
