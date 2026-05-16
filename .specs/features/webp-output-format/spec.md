# WebP Output Format Specification

## Problem Statement

The library currently outputs only JPEG or PNG, selected implicitly via `quality === 100 → PNG`. WebP offers significantly better compression than JPEG at equivalent visual quality (Google reports ~30% smaller files), making it attractive for apps that need to reduce bandwidth or storage while maintaining image fidelity. There is no way to produce WebP output today.

## Goals

- [ ] Callers can request WebP lossy output via an explicit `format` option in `ResizeOptions`
- [ ] WebP output quality is controlled by the existing `quality` parameter (0–100)
- [ ] Backward compatibility: existing calls without `format` behave identically to today
- [ ] Output file name / auto-path use `.webp` extension when `format='webp'`

## Out of Scope

- WebP lossless output (quality-based lossy only)
- iOS native implementation (iOS remains a silent no-op for `format='webp'`)
- WebP input decoding (source files are still JPEG/PNG; WebP as input not required)
- Animated WebP
- `keepMeta` support for WebP (no-op, consistent with PNG behavior)

---

## User Stories

### P1: Resize image to WebP output ⭐ MVP

**User Story**: As a React Native developer, I want to pass `format: 'webp'` in `ResizeOptions` so that the resized image is saved as a WebP file, reducing output size vs JPEG.

**Why P1**: Core feature — no other story is meaningful without this.

**Acceptance Criteria**:

1. WHEN `resize(filePath, w, h, quality, { format: 'webp' })` is called THEN system SHALL return a `ResizeResult` whose `path` ends with `.webp`
2. WHEN `format: 'webp'` is passed THEN the output file SHALL be a valid WebP (lossy) image readable by `BitmapFactory.decodeFile` on Android
3. WHEN `quality` is between 1 and 99 inclusive and `format: 'webp'` THEN output SHALL be lossy WebP compressed at that quality level
4. WHEN `format: 'webp'` and `quality === 100` THEN output SHALL be WebP lossy at quality 100 (NOT PNG — `format` takes precedence)
5. WHEN `format` is omitted THEN system SHALL behave exactly as today (`quality === 100 → PNG`, else `JPEG`)

**Independent Test**: Call `resize` with `format: 'webp'`, check `result.path` ends in `.webp`, verify file is non-empty and decodable.

---

### P2: Quality controls WebP compression level

**User Story**: As a developer, I want `quality` to control the WebP compression level so I can trade off file size vs image quality just like with JPEG.

**Why P2**: Without this story the `quality` parameter is silently ignored for WebP, which is confusing and undocumented.

**Acceptance Criteria**:

1. WHEN `format: 'webp'` and `quality=20` THEN output file SHALL be smaller than same call with `quality=80`
2. WHEN `format: 'webp'` and `quality` is out of range (< 1 or > 100) THEN system SHALL reject with a descriptive error (consistent with existing JPEG validation)

**Independent Test**: Two identical resize calls differing only in quality; assert `size` ordering.

---

### P3: format field accepts 'jpeg' and 'png' explicitly

**User Story**: As a developer, I want to write `format: 'jpeg'` or `format: 'png'` explicitly instead of relying on the `quality === 100` magic so that code intent is clear.

**Why P3**: Nice-to-have ergonomic improvement; does not block WebP shipping.

**Acceptance Criteria**:

1. WHEN `format: 'jpeg'` THEN output SHALL be JPEG regardless of `quality` value (including `quality=100`)
2. WHEN `format: 'png'` THEN output SHALL be PNG regardless of `quality` value
3. WHEN both `format` and an incompatible quality convention would disagree (e.g. `format:'jpeg', quality:100`) THEN `format` SHALL win

**Independent Test**: Call with `format:'jpeg', quality:100`; assert output file is JPEG, not PNG.

---

## Edge Cases

- WHEN `format: 'webp'` and `keepMeta: true` THEN system SHALL silently skip EXIF copy and resolve normally (no-op, consistent with PNG)
- WHEN `format: 'webp'` on iOS THEN system SHALL silently produce a JPEG fallback or no-op and resolve (iOS is stub; must not crash)
- WHEN `format` is an unrecognised string THEN system SHALL reject with `TypeError: Invalid format: '<value>'`
- WHEN `outputPath` is provided with a non-`.webp` extension and `format: 'webp'` THEN system SHALL use the caller-supplied path as-is (caller owns the path)
- WHEN `outputPath` is empty (auto) and `format: 'webp'` THEN system SHALL generate a path with `.webp` extension

---

## Success Criteria

- [ ] `format: 'webp'` produces a readable WebP file on Android (verified by instrumented test)
- [ ] Auto-generated output path ends in `.webp` when `format: 'webp'`
- [ ] `ResizeResult.name` ends in `.webp` when `format: 'webp'`
- [ ] Existing tests pass unchanged (backward compat — no `format` field)
- [ ] Invalid `format` value produces a `TypeError` (not a crash or silent wrong output)
- [ ] `keepMeta: true` + `format: 'webp'` does not error
