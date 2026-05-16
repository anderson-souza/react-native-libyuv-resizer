# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Coding Behavior Contract (12 Rules)

1. Think before coding. State your assumptions. Surface tradeoffs. Ask before guessing. Push back when a simpler approach exists.
2. Simplicity first. Minimum code that solves the problem. No speculative features. No abstractions for single-use code.
3. Surgical changes. Touch only what is asked. Do not "improve" adjacent code, comments, or formatting. Match existing style.
4. Goal-driven execution. Define success criteria. Loop until verified. Do not narrate steps; tell me what success looks like.
5. Do not make the model do non-language work. Retry policies, routing, escalation thresholds belong in deterministic code.
6. Hard token budgets, no exceptions. Stop and ask if a task is trending past its budget.
7. Surface conflicts, do not average them. If two parts of the codebase disagree, flag the disagreement and ask which to follow.
8. Read before you write. Understand adjacent code (the file and nearby siblings) before adding new code.
9. Tests are required but are not the goal. A passing test that tests nothing useful is a failure. Tests must check behavior.
10. Long-running operations require checkpoints. After every significant step, summarize what was done and confirm before proceeding.
11. Convention beats novelty. In an established codebase, match the existing pattern even if a "better" one exists.
12. Fail visibly, not silently. Surface every skipped record, every rolled-back transaction, every constraint violation. Never report success when something was bypassed.

## Environment

- **Shell:** Use PowerShell tool for all commands — Bash tool fails (no `/mnt/e/` mount on Windows).
- **File search:** Glob tool times out on this repo; use `Get-ChildItem` via PowerShell instead. Never run `Get-ChildItem` or `find` from repo root — `node_modules` causes 1.4MB output/timeout. Always target a specific subpath (e.g. `android/src`, `src/`).

## Commands

```bash
# Install dependencies
yarn install

# Build the JS/TS library output (lib/)
yarn prepare

# Type check
yarn typecheck

# Lint
yarn lint

# Tests
yarn test
yarn test --testPathPattern="src/__tests__/index"  # single file

# Build example app (Android)
yarn turbo run build:android

# Build example app (iOS)
yarn turbo run build:ios

# Clean all build artifacts
yarn clean
```

## Architecture

This is a **React Native Turbo Module** library scaffolded with `react-native-builder-bob`. It wraps libyuv (Google's YUV conversion/scaling C++ library) for use in React Native apps.

### JS/TS Layer (`src/`)

- `NativeLibyuvResizer.ts` — Turbo Module spec; defines the native bridge interface via `TurboModuleRegistry`
- `multiply.native.tsx` — native implementation entry (calls `NativeLibyuvResizer`)
- `multiply.tsx` — JS/web fallback
- `index.tsx` — public exports

The codegen config in `package.json` (`codegenConfig`) generates the native spec glue from `NativeLibyuvResizer.ts`. Module name is `LibyuvResizer`, codegen name is `LibyuvResizerSpec`.

### Android (`android/`)

- `LibyuvResizerModule.kt` — extends `NativeLibyuvResizerSpec` (codegen-generated), implements native methods
- `LibyuvResizerPackage.kt` — registers the module as a Turbo Module via `BaseReactPackage`
- `build.gradle` — no C++ build configured yet; add `externalNativeBuild` + CMake when adding libyuv

**libyuv integration (planned):** libyuv is a git submodule at `libyuv/` sourced from `https://android.googlesource.com/platform/external/libyuv/`. Android CMake uses `add_subdirectory(../libyuv libyuv)` and links against the `yuv` static target. iOS does NOT use libyuv — use Apple's Accelerate framework (`vImage`) instead.

### iOS (`ios/`)

- `LibyuvResizer.mm` — Objective-C++ implementation; extends the codegen-generated `NativeLibyuvResizerSpecJSI`
- `LibyuvResizer.h` — header

### Example App (`example/`)

Separate yarn workspace (`react-native-libyuv-resizer-example`). Run example commands via `yarn example <cmd>` from root or directly inside `example/`.

## Android Build

`gradlew` lives in `example/android/` — the library has no standalone gradlew. Gradle project name is `:react-native-libyuv-resizer`.

```bash
# Compile library Kotlin
cd example/android && ./gradlew :react-native-libyuv-resizer:compileDebugKotlin

# Compile instrumented tests
./gradlew :react-native-libyuv-resizer:compileDebugAndroidTestKotlin

# Run instrumented tests (requires connected emulator/device)
./gradlew :react-native-libyuv-resizer:connectedDebugAndroidTest

# Run specific test class
./gradlew :react-native-libyuv-resizer:connectedDebugAndroidTest --tests "com.libyuvresizer.ClassName"
```

## Gotchas

- **RN codegen + Promise payload:** Inline object literal (`Promise<{ path: string }>`) breaks codegen. Always declare a named exported type (`export type Foo = { ... }`) and reference it.
- **Android tests:** `Arguments.createMap()` available in `androidTest/` (instrumented, has Android runtime) but not in `test/` (JVM unit tests). Don't use WritableMap/ReadableMap in JVM tests.
- **Bridge param changes break all TS tests:** Adding a param to `NativeLibyuvResizer.ts` breaks every `toHaveBeenCalledWith` in `index.test.tsx`. Update all expected arg lists when the bridge signature changes.

## Key Details

- **Package manager:** Yarn 4 (Berry) with workspaces — always use `yarn`, never `npm`
- **Build output:** `lib/` — generated by `react-native-builder-bob` targeting ESM module + TypeScript declarations
- **Commits:** conventional commits enforced via commitlint + lefthook. Always use one of: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`, `perf:`, `ci:`. Breaking changes use `!` suffix (`feat!:`). Subject line must be imperative, lowercase, ≤72 chars, no period. Body optional — use only when the _why_ is non-obvious. Never multi-paragraph commit messages.
- **New Arch only:** module uses Turbo Modules (`isTurboModule = true`) — no legacy bridge support
