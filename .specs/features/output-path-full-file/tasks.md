# Tasks: outputPath as Full File Path

## Task 1 — Update ResizeValidator (Android)

**File:** `android/src/main/java/com/libyuvresizer/ResizeValidator.kt`  
**Lines:** 56–68

Replace the current check (requires `outputPath` to be an existing directory) with a
check that the **parent directory** of `outputPath` exists.

**Before:**
```kotlin
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
```

**After:**
```kotlin
if (params.outputPath.isNotEmpty()) {
  val parentDir = File(params.outputPath).parentFile
  if (parentDir == null || !parentDir.exists())
    return ValidationResult.Invalid(
      "E_INVALID_OUTPUT_PATH",
      "Output directory does not exist: ${File(params.outputPath).parent}"
    )
}
```

**Verify:** `ResizeValidatorTest` passes; new test cases added (see Task 5).

---

## Task 2 — Update resolveOutputFile (Android)

**File:** `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`  
**Lines:** 171–176

Use the full path directly instead of treating it as a directory.

**Before:**
```kotlin
private fun resolveOutputFile(inputFilePath: String, outputPath: String, ext: String): File {
    if (outputPath.isEmpty()) {
        return File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
    }
    return File(outputPath, File(inputFilePath).name)
}
```

**After:**
```kotlin
private fun resolveOutputFile(inputFilePath: String, outputPath: String, ext: String): File {
    if (outputPath.isEmpty()) {
        return File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
    }
    return File(outputPath)
}
```

The `inputFilePath` parameter is now unused; remove it from the signature and update
the two call sites inside `resize()` (line 137):

```kotlin
// Before:
val outFile = resolveOutputFile(filePath, outputPath, ext)
// After:
val outFile = resolveOutputFile(outputPath, ext)
```

**Verify:** Compiles; instrumented test writes to the given full path.

---

## Task 3 — Update TypeScript JSDoc

**File:** `src/resizer.native.tsx`  
**Lines:** 70–74 (the `outputPath` field in `ResizeOptions`) and the `@example` block
(lines 126–137).

Update `outputPath` doc:

```ts
/**
 * Full absolute path for the output file, including directory, filename, and
 * extension (e.g. `'/sdcard/Pictures/thumb.jpg'`).
 * The parent directory must already exist.
 * When omitted the native layer generates a path in the app's cache
 * directory automatically.
 */
outputPath?: string;
```

Update the `@example` block to use a full file path:

```ts
// With options
const result = await resize('/path/to/photo.jpg', 800, 600, 80, {
  rotation: 90,
  mode: 'cover',
  filterMode: 'bilinear',
  outputPath: '/path/to/output/thumb.jpg',
});
```

**Verify:** `yarn typecheck` passes.

---

## Task 4 — Update TS unit tests

**File:** `src/__tests__/index.test.tsx`

Three existing tests use directory-like `outputPath` values. Update them to use full
file paths.

| Line | Old value | New value |
|------|-----------|-----------|
| 175  | `'/tmp/out'` | `'/tmp/out.jpg'` |
| 210  | `'/sdcard/Pictures'` | `'/sdcard/Pictures/resized.jpg'` |
| 280  | `'/sdcard/out'` | `'/sdcard/out.jpg'` |

The `expect(mockResize).toHaveBeenCalledWith(...)` args at lines 178, 217, 287 must
match the new values.

**Verify:** `yarn test` → all tests green.

---

## Task 5 — Add Android unit tests for validator

**File:** `android/src/test/java/com/libyuvresizer/ResizeValidatorTest.kt`  
(create if it does not exist; check `android/src/test/` first)

Add test cases for the new validator logic:

```kotlin
@Test
fun `valid outputPath with existing parent dir passes`() { ... }

@Test
fun `outputPath whose parent dir does not exist fails with E_INVALID_OUTPUT_PATH`() { ... }

@Test
fun `empty outputPath skips parent dir check`() { ... }
```

**Verify:** `./gradlew :react-native-libyuv-resizer:test` passes.

---

## Execution Order

```
Task 1 → Task 2  (both Android, safe to do sequentially in one commit)
Task 3            (TS only, independent)
Task 4            (TS tests, depends on Task 3 for correct expected values)
Task 5            (Android tests, depends on Task 1)
```

Suggested commits:
- `fix: treat outputPath as full file path on Android`  (Tasks 1 + 2)
- `test: add validator tests for full outputPath`       (Task 5)
- `docs: update outputPath JSDoc to reflect full path`  (Task 3)
- `test: update outputPath test fixtures to full paths` (Task 4)
