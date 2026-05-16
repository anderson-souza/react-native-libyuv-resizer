# WebP Output Format Tasks

**Design**: `.specs/features/webp-output-format/design.md`
**Status**: Draft

---

## Execution Plan

```
Phase 1 — Bridge contract (must be first; everything depends on param order)
  T1 → T2

Phase 2 — Platform implementations (parallel after T2)
  T2 complete, then:
    ├── T3 [P]  Android: ResizeParams + ResizeValidator
    ├── T4 [P]  Android: LibyuvResizerModule encode path
    └── T5 [P]  iOS: LibyuvResizer.mm new param

Phase 3 — Tests (parallel after T3+T4)
  T3+T4 complete, then:
    ├── T6 [P]  TS unit tests update
    ├── T7 [P]  Android JVM unit tests (ResizeValidator)
    └── T8 [P]  Android instrumented tests

Phase 4 — Docs (after all pass)
  T9
```

---

## Task Breakdown

### T1: Add `OutputFormat` type and `format` option to TS public API

**What**: Add `OutputFormat = 'jpeg' | 'png' | 'webp'` type, `format?: OutputFormat` to `ResizeOptions`, format resolution logic, and `VALID_FORMATS` validation inside `resize()`.
**Where**: `src/resizer.native.tsx`
**Depends on**: None
**Reuses**: Same pattern as `ResizeMode` / `FilterMode` / `VALID_MODES`

**Done when**:
- [ ] `OutputFormat` type exported alongside `ResizeMode` and `FilterMode`
- [ ] `format?: OutputFormat` added to `ResizeOptions` with JSDoc
- [ ] `VALID_FORMATS` constant defined (`['jpeg', 'png', 'webp']`)
- [ ] Resolution logic: `options?.format ?? (quality === 100 ? 'png' : 'jpeg')`
- [ ] `TypeError` rejected on unknown format string (mirrors mode/filterMode pattern)
- [ ] `resolvedFormat` passed as last arg to `LibyuvResizer.resize()`
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T2: Add `format: string` to `NativeLibyuvResizer.ts` bridge spec

**What**: Add `format: string` as 10th positional parameter to `Spec.resize()`. Update JSDoc on the bridge method.
**Where**: `src/NativeLibyuvResizer.ts`
**Depends on**: T1 (must know param position is 10)
**Reuses**: Existing `Spec` interface — append param

**Done when**:
- [ ] `format: string` added after `keepMeta: boolean` in `Spec.resize()` signature
- [ ] JSDoc on bridge param updated to document accepted values (`'jpeg' | 'png' | 'webp'`)
- [ ] `yarn typecheck` passes
- [ ] No codegen errors: `yarn prepare` runs clean

**Verify**:
```bash
yarn typecheck && yarn prepare
```

---

### T3: Add `format` field to `ResizeParams` and `ResizeValidator` [P]

**What**: Add `format: String = "jpeg"` to `ResizeParams` data class; add `VALID_FORMATS` set and format validation rule to `ResizeValidator.validate()`.
**Where**: `android/src/main/java/com/libyuvresizer/ResizeValidator.kt`
**Depends on**: T2
**Reuses**: Existing `VALID_MODES` / `VALID_FILTER_MODES` pattern in same file

**Done when**:
- [ ] `ResizeParams` has `val format: String = "jpeg"` (last field, default preserves backward compat)
- [ ] `VALID_FORMATS = setOf("jpeg", "png", "webp")` in `ResizeValidator` companion
- [ ] Validation rule added: `E_INVALID_FORMAT` with message `"format must be jpeg, png, or webp, got: <value>"`
- [ ] Kotlin compiles: `./gradlew :react-native-libyuv-resizer:compileDebugKotlin` from `example/android/`

**Verify**:
```bash
cd example/android && ./gradlew :react-native-libyuv-resizer:compileDebugKotlin
```

---

### T4: Replace quality-based encode branch with format-driven encode in `LibyuvResizerModule.kt` [P]

**What**: Add `formatToExtAndCompressFormat()` private helper; replace the `if (q == 100) PNG else JPEG` block with format-driven logic; update `keepMeta` guard; wire `format` param from bridge into `ResizeParams`.
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T3 (needs updated `ResizeParams`)
**Reuses**: Existing `resolveOutputFile(filePath, outputPath, ext)` — already accepts ext string

**Done when**:
- [ ] `override fun resize()` signature has new `format: String` param after `keepMeta`
- [ ] `ResizeParams(...)` construction includes `format = format`
- [ ] `formatToExtAndCompressFormat(format, q)` helper exists in companion object, handles `"jpeg"` / `"png"` / `"webp"` (API 30 branch for `WEBP_LOSSY`, legacy `WEBP` fallback with `@Suppress("DEPRECATION")`)
- [ ] `keepMeta` guard uses `params.format == "jpeg"` (not `ext == "jpg"`)
- [ ] Auto-generated `outputPath` uses correct extension (`.webp` for WebP)
- [ ] `./gradlew :react-native-libyuv-resizer:compileDebugKotlin` green

**Verify**:
```bash
cd example/android && ./gradlew :react-native-libyuv-resizer:compileDebugKotlin
```

---

### T5: Accept `format` param in `LibyuvResizer.mm` (iOS no-op) [P]

**What**: Add `format:(NSString *)format` to both Turbo Module and `RCT_EXPORT_METHOD` signatures in the iOS implementation. Body unchanged — still encodes JPEG.
**Where**: `ios/LibyuvResizer.mm` (and `ios/LibyuvResizer.h` if signature is declared there)
**Depends on**: T2
**Reuses**: Existing pattern — other params like `keepMeta` accepted and ignored

**Done when**:
- [ ] `format` param added to Turbo signature (`resizeWithFilePath:...format:...`)
- [ ] `format` param added to `RCT_EXPORT_METHOD` macro
- [ ] No iOS build errors (validate with `yarn turbo run build:ios` or pod install check)
- [ ] Existing JPEG encode logic unchanged

**Verify**:
```bash
yarn turbo run build:ios
# or: cd example/ios && pod install --repo-update
```

---

### T6: Update TS Jest tests for new bridge param [P]

**What**: Update every `toHaveBeenCalledWith` in `src/__tests__/index.test.tsx` (and any other TS test files) to include the new `format` string as 10th arg. Add new test cases: `format:'webp'` output, invalid format rejection, `format:'jpeg'` with `quality:100` (format wins).
**Where**: `src/__tests__/index.test.tsx`
**Depends on**: T1, T2
**Reuses**: Existing test structure — follow same `it(...)` patterns

**Done when**:
- [ ] All existing `toHaveBeenCalledWith` calls updated with correct 10th arg (`'jpeg'` or `'png'` depending on test case)
- [ ] New test: `resize(..., { format: 'webp' })` → bridge called with `'webp'` as last arg
- [ ] New test: `resize(..., { format: 'gif' })` → `Promise.reject(TypeError)` 
- [ ] New test: `resize(..., 100, { format: 'jpeg' })` → bridge called with `'jpeg'` (not `'png'`)
- [ ] `yarn test` passes with 85%+ coverage

**Verify**:
```bash
yarn test
```

---

### T7: Add `ResizeValidatorTest` cases for `format` [P]

**What**: Add test cases to `ResizeValidatorTest.kt` covering valid formats (`jpeg`, `png`, `webp`) and invalid format rejection.
**Where**: `android/src/test/java/com/libyuvresizer/ResizeValidatorTest.kt`
**Depends on**: T3
**Reuses**: Existing test helper `validParams()` — add `format` to it

**Done when**:
- [ ] `validParams()` helper updated with `format = "jpeg"` default
- [ ] Test: `format = "webp"` → `ValidationResult.Valid`
- [ ] Test: `format = "gif"` → `ValidationResult.Invalid` with code `E_INVALID_FORMAT`
- [ ] Test: `format = ""` → `ValidationResult.Invalid`
- [ ] `./gradlew :react-native-libyuv-resizer:testDebugUnitTest` green

**Verify**:
```bash
cd example/android && ./gradlew :react-native-libyuv-resizer:testDebugUnitTest
```

---

### T8: Add instrumented test for WebP output [P]

**What**: Add test case(s) to the existing instrumented test class verifying that `format:'webp'` produces a `.webp` file that is decodable by `BitmapFactory`.
**Where**: `android/src/androidTest/java/com/libyuvresizer/LibyuvResizerModuleTest.kt` (or the instrumented test file that exercises `resize()`)
**Depends on**: T4
**Reuses**: Existing instrumented test fixture pattern (copy input JPEG to cache, call resize, assert)

**Done when**:
- [ ] Test calls `resize()` with `format = "webp"`, asserts `result.path` ends in `.webp`
- [ ] Test asserts `BitmapFactory.decodeFile(result.path)` returns non-null bitmap
- [ ] Test asserts `result.size > 0`
- [ ] Test: `format = "webp"` + `keepMeta = true` → resolves without error (no-op)
- [ ] `./gradlew :react-native-libyuv-resizer:connectedDebugAndroidTest` green (requires emulator/device)

**Verify**:
```bash
cd example/android && ./gradlew :react-native-libyuv-resizer:connectedDebugAndroidTest
```

---

### T9: Update ROADMAP.md and STATE.md

**What**: Add WebP Output Format milestone to ROADMAP.md; update STATE.md active work section.
**Where**: `.specs/project/ROADMAP.md`, `.specs/project/STATE.md`
**Depends on**: T6, T7, T8 (all green — feature complete)
**Reuses**: Existing milestone format in ROADMAP.md

**Done when**:
- [ ] ROADMAP.md has new milestone `M4.6 — WebP Output Format` with feature list and status
- [ ] STATE.md active work updated: keepMeta marked done (already is), WebP entry added and checked

**Verify**: Files updated, no broken markdown.

---

## Parallel Execution Map

```
Phase 1 (Sequential — bridge contract):
  T1 ──→ T2

Phase 2 (Parallel — platform impls):
  T2 complete, then simultaneously:
    ├── T3 [P]
    ├── T4 [P]  (T4 also waits for T3 to compile)
    └── T5 [P]

Phase 3 (Parallel — tests):
  T3+T4 complete, then simultaneously:
    ├── T6 [P]  (depends T1+T2)
    ├── T7 [P]  (depends T3)
    └── T8 [P]  (depends T4)

Phase 4 (Sequential — docs):
  All green → T9
```

## Granularity Check

| Task | Scope | Status |
|---|---|---|
| T1: OutputFormat type + ResizeOptions + validation | 1 file, cohesive | ✅ |
| T2: Bridge spec param | 1 param in 1 file | ✅ |
| T3: ResizeParams + ResizeValidator | 1 file, cohesive data+validation | ✅ |
| T4: LibyuvResizerModule encode path | 1 file, 1 concern | ✅ |
| T5: iOS param accept | 1 file | ✅ |
| T6: TS test updates + new cases | 1 test file | ✅ |
| T7: Validator JVM tests | 1 test file | ✅ |
| T8: Instrumented WebP test | 1 test file | ✅ |
| T9: Docs update | 2 spec files | ✅ |
