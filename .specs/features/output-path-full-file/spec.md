# Feature: outputPath as Full File Path

## Status: specified

## Problem

`outputPath` currently accepts a **directory** path. The native layer appends the
source file's name to produce the output path:

```kotlin
// Current: File("/sdcard/Pictures", "photo.jpg") → /sdcard/Pictures/photo.jpg
return File(outputPath, File(inputFilePath).name)
```

This makes it impossible to control the output filename or extension from JS.

## Goal

Accept a **full output file path** (directory + filename + extension):

```ts
resize('/path/to/source.jpg', 800, 600, 80, {
  outputPath: '/sdcard/Pictures/resized-thumb.jpg',
});
```

The native layer uses the path as-is, without appending anything.

## Scope

- Android native (Kotlin)
- TypeScript public API + JSDoc
- TS unit tests

iOS is not in scope (method is not implemented; it rejects all calls).

## Acceptance Criteria

1. `outputPath: '/some/dir/output.jpg'` writes the file to exactly that path.
2. `outputPath: '/some/dir/output.png'` writes a PNG to that path.
3. When `outputPath` is omitted or empty, auto-generation still works unchanged.
4. If the **parent directory** of `outputPath` does not exist, the call rejects with
   `E_INVALID_OUTPUT_PATH`.
5. If `outputPath` points to an existing **file** (not a dir), it is overwritten
   (no error — same behaviour as creating a new file at that path).
6. All existing tests pass; new tests cover the full-path cases.

## Out of Scope

- Creating parent directories automatically.
- Changing the bridge signature (same `outputPath: string` param).
- iOS implementation.
