# keepMeta / EXIF Copy Specification

## Problem Statement

When an app resizes a photo, `BitmapFactory.decodeFile` + `Bitmap.compress` strips all EXIF metadata from the output file. GPS coordinates, camera model, capture date, and orientation tags are lost. Apps that display photo details, upload to services that need location, or rely on EXIF orientation for display end up with broken or incomplete data after resizing.

## Goals

- [ ] Caller can opt-in to EXIF preservation via `keepMeta: true` in `ResizeOptions`
- [ ] All tags supported by Android's `ExifInterface` are copied from source → output JPEG
- [ ] Zero performance impact when `keepMeta` is omitted or `false`
- [ ] No-op on PNG output (quality=100) — no error thrown
- [ ] No-op on iOS — documented as Android-only for now

## Out of Scope

- Writing new/synthetic EXIF tags (e.g., resized dimensions into `PixelXDimension`)
- PNG EXIF / tEXt chunk support
- iOS native EXIF copy (deferred to M4 iOS implementation)
- EXIF editing / partial tag selection

---

## User Stories

### P1: Copy EXIF from source to resized JPEG ⭐ MVP

**User Story**: As a mobile developer, I want to pass `keepMeta: true` to `resize()` so that the output JPEG retains all EXIF metadata from the original photo.

**Why P1**: Core value of the feature. Without this, the option has no effect.

**Acceptance Criteria**:

1. WHEN `resize()` is called with `keepMeta: true` and a JPEG source THEN the output JPEG SHALL contain all EXIF tags that were present in the source and are supported by `ExifInterface`
2. WHEN `resize()` is called with `keepMeta: true` THEN GPS tags (`TAG_GPS_LATITUDE`, `TAG_GPS_LONGITUDE`, `TAG_GPS_LATITUDE_REF`, `TAG_GPS_LONGITUDE_REF`, `TAG_GPS_ALTITUDE`, `TAG_GPS_ALTITUDE_REF`) SHALL be preserved if present in source
3. WHEN `resize()` is called with `keepMeta: true` THEN camera tags (`TAG_MAKE`, `TAG_MODEL`, `TAG_DATETIME`, `TAG_DATETIME_ORIGINAL`, `TAG_DATETIME_DIGITIZED`) SHALL be preserved if present in source
4. WHEN `resize()` is called with `keepMeta: true` THEN orientation tag (`TAG_ORIENTATION`) SHALL be set to `1` (normal) in the output, since the image was already decoded and re-encoded correctly
5. WHEN the source file has no EXIF data THEN `resize()` with `keepMeta: true` SHALL succeed without error and produce a valid JPEG

**Independent Test**: Call `resize('/path/to/gps-photo.jpg', 800, 600, 85, { keepMeta: true })`, then read EXIF from output with `ExifInterface` — GPS tags must match source.

---

### P2: keepMeta defaults to false — no behavior change for existing callers

**User Story**: As a developer already using `resize()`, I want existing calls without `keepMeta` to be unaffected so that the update is non-breaking.

**Why P2**: Any change to the default behavior breaks existing integrations.

**Acceptance Criteria**:

1. WHEN `keepMeta` is omitted from `ResizeOptions` THEN system SHALL behave identically to current behavior (no EXIF copy, no overhead)
2. WHEN `keepMeta: false` is passed explicitly THEN system SHALL behave identically to omitting it

**Independent Test**: All existing instrumented tests pass without modification.

---

### P3: PNG + keepMeta is a silent no-op

**User Story**: As a developer passing `keepMeta: true` with `quality: 100`, I want the call to succeed without error so that I don't need to guard my code against format combinations.

**Why P3**: PNG has no standard EXIF support. Silently skipping avoids breaking callers who always pass `keepMeta: true`.

**Acceptance Criteria**:

1. WHEN `resize()` is called with `keepMeta: true` AND `quality === 100` (PNG output) THEN the Promise SHALL resolve successfully
2. WHEN output is PNG with `keepMeta: true` THEN no EXIF copy operation SHALL be attempted

**Independent Test**: `resize('/path/to/photo.jpg', 800, 600, 100, { keepMeta: true })` resolves — output is valid PNG, no error.

---

### P4: iOS keepMeta is a silent no-op

**User Story**: As a cross-platform developer, I want `keepMeta: true` to be accepted on iOS without throwing so that I can share code between platforms.

**Why P4**: iOS is currently a stub. Rejecting `keepMeta` on iOS would force platform-specific guards at every call site.

**Acceptance Criteria**:

1. WHEN `resize()` is called with `keepMeta: true` on iOS THEN the Promise SHALL resolve successfully (iOS stub behavior unchanged)
2. API docs SHALL note that EXIF copy is Android-only until iOS is fully implemented

**Independent Test**: iOS stub returns `E_NOT_IMPLEMENTED` for resize itself — keepMeta does not add a new error.

---

## Edge Cases

- WHEN source JPEG exists but has no EXIF segment THEN `keepMeta: true` SHALL succeed — output is valid JPEG with no EXIF
- WHEN `ExifInterface` throws reading source EXIF THEN system SHALL proceed with resize without EXIF copy (log warning; do not reject Promise)
- WHEN `ExifInterface` throws writing EXIF to output THEN system SHALL reject Promise with `E_EXIF_WRITE_FAILED`
- WHEN source is a PNG file (no EXIF by definition) AND `keepMeta: true` THEN system SHALL produce valid JPEG output with no EXIF (no-op, no error)
- WHEN output JPEG file is deleted between resize and EXIF write THEN system SHALL reject Promise with `E_EXIF_WRITE_FAILED`

---

## Success Criteria

- [ ] `keepMeta: true` round-trip: GPS lat/lng preserved in resized JPEG (verified by instrumented test)
- [ ] `keepMeta` absent: zero change in output file or behavior vs current baseline
- [ ] PNG + `keepMeta: true`: Promise resolves, no crash, no error
- [ ] All existing instrumented tests pass without changes
- [ ] TypeScript: `keepMeta?: boolean` visible in `ResizeOptions` with JSDoc
- [ ] Native bridge: `keepMeta` passed as boolean through codegen spec

---

## Implementation Notes

- **Android library**: `androidx.exifinterface:exifinterface` — already available in most RN apps; add as `implementation` dep in `android/build.gradle`
- **EXIF copy strategy**: Read all `ExifInterface.TAG_*` string constants via reflection or explicit list → copy attribute-by-attribute from source `ExifInterface` to output `ExifInterface`; call `saveAttributes()` on output
- **Orientation reset**: After EXIF copy, explicitly set `TAG_ORIENTATION` to `ExifInterface.ORIENTATION_NORMAL` (value `"1"`) — the bitmap was decoded with correct orientation already
- **Turbo Module spec**: Add `keepMeta: boolean` as new parameter to `resize()` in `NativeLibyuvResizer.ts`; default to `false` in `resizer.native.tsx`
- **Codegen impact**: Adding a parameter to `resize()` is a breaking change to the codegen spec — Kotlin `override fun resize(...)` must add `keepMeta: Boolean`
