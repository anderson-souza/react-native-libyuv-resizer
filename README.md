# react-native-libyuv-resizer

High-performance image resizer for React Native using libyuv (Android).

## Requirements

- React Native >= 0.72.0
- New Architecture (Turbo Modules) enabled or disabled — both supported
- Android only (iOS stubbed)

## Installation

```sh
yarn add react-native-libyuv-resizer
```

## API

### `resize(filePath, targetWidth, targetHeight, quality, options?): Promise<ResizeResult>`

| Parameter            | Type            | Default     | Description                                                                       |
| -------------------- | --------------- | ----------- | --------------------------------------------------------------------------------- |
| `filePath`           | `string`        | —           | Absolute path to source image                                                     |
| `targetWidth`        | `number`        | —           | Output width in pixels                                                            |
| `targetHeight`       | `number`        | —           | Output height in pixels                                                           |
| `quality`            | `number`        | —           | Compression quality `1–100`. Controls JPEG and WebP lossy level. Ignored for PNG. When `format` is omitted: `100` → PNG, else → JPEG. |
| `options.rotation`   | `RotationAngle` | `0`         | Clockwise rotation before resize: `0 \| 90 \| 180 \| 270 \| -90 \| -180 \| -270` |
| `options.mode`       | `ResizeMode`    | `'contain'` | How the image fits the target box: `'contain' \| 'cover' \| 'stretch'`           |
| `options.filterMode` | `FilterMode`    | `'box'`     | Scaling filter: `'none' \| 'linear' \| 'bilinear' \| 'box'`                     |
| `options.outputPath` | `string`        | auto        | Absolute directory path for the output file. Auto-generated in cache if omitted. |
| `options.keepMeta`   | `boolean`       | `false`     | Copy EXIF metadata from source to output JPEG. Android only; no-op on iOS, PNG and WebP output. |
| `options.format`     | `OutputFormat`  | derived     | Output format: `'jpeg' \| 'png' \| 'webp'`. When specified, takes precedence over the `quality`-based heuristic. WebP is Android only; iOS produces JPEG. |

### Return value

```ts
interface ResizeResult {
  path: string;   // absolute path to the resized file
  uri: string;    // file:// URI
  size: number;   // file size in bytes
  name: string;   // file name
  width: number;  // output width in pixels
  height: number; // output height in pixels
}
```

### Types

```ts
type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;
type ResizeMode    = 'contain' | 'cover' | 'stretch';
type FilterMode    = 'none' | 'linear' | 'bilinear' | 'box';
type OutputFormat  = 'jpeg' | 'png' | 'webp';

interface ResizeOptions {
  rotation?:   RotationAngle;
  mode?:       ResizeMode;
  filterMode?: FilterMode;
  outputPath?: string;
  keepMeta?:   boolean;
  format?:     OutputFormat;
}
```

### Resize modes

| Mode      | Behavior                                                       |
| --------- | -------------------------------------------------------------- |
| `contain` | Fits entirely within the target box, preserving aspect ratio   |
| `cover`   | Fills the target box, preserving aspect ratio (may crop)       |
| `stretch` | Stretches to exact target dimensions, ignoring aspect ratio    |

### Filter modes

| Mode       | Quality / Speed trade-off                              |
| ---------- | ------------------------------------------------------ |
| `none`     | Nearest-neighbor — fastest, lowest quality             |
| `linear`   | Linear interpolation                                   |
| `bilinear` | Bilinear interpolation                                 |
| `box`      | Box filter — best quality for downscaling *(default)* |

## Usage

```ts
import { resize } from 'react-native-libyuv-resizer';

// Basic resize
const result = await resize('/path/to/photo.jpg', 1280, 720, 85);
console.log(result.path, result.width, result.height);

// Resize with options
const result = await resize('/path/to/photo.jpg', 1280, 720, 85, {
  rotation: 90,
  mode: 'cover',
  filterMode: 'bilinear',
});

// Preserve EXIF metadata (GPS, camera, date) — Android only
const result = await resize('/path/to/photo.jpg', 800, 600, 80, {
  keepMeta: true,
});

// Custom output path
const result = await resize('/path/to/photo.jpg', 800, 600, 80, {
  outputPath: '/path/to/output-dir',
});

// PNG output — explicit format
const result = await resize('/path/to/photo.jpg', 800, 600, 80, {
  format: 'png',
});
console.log(result.path); // ends with .png

// WebP output — smaller files than JPEG at equivalent quality (Android only)
const result = await resize('/path/to/photo.jpg', 1280, 720, 85, {
  format: 'webp',
});
console.log(result.path); // ends with .webp
```

### With react-native-image-picker

```ts
import { launchImageLibrary } from 'react-native-image-picker';
import { resize } from 'react-native-libyuv-resizer';

const picked = await launchImageLibrary({ mediaType: 'photo' });
const asset = picked.assets?.[0];

if (asset?.uri) {
  const result = await resize(asset.uri, 800, 600, 80, {
    mode: 'cover',
    keepMeta: true, // preserve GPS and camera tags
  });
  console.log('Resized image at:', result.path);
}
```

## format — output format

The `format` option explicitly controls the output container. When omitted, the library falls back to the legacy quality-based heuristic (`quality === 100` → PNG, otherwise JPEG).

| Value    | Output | `quality` effect | Platform |
| -------- | ------ | ---------------- | -------- |
| `'jpeg'` | JPEG | Lossy compression level | Android, iOS |
| `'png'`  | PNG  | Ignored (lossless) | Android, iOS |
| `'webp'` | WebP lossy | Compression level | Android only; iOS produces JPEG |

**Performance note:** WebP encoding is significantly slower than JPEG (~5–15× on typical Android devices) because it runs in software with no dedicated hardware accelerator. Use `format: 'webp'` when file size matters more than encoding speed.

**Format vs quality precedence:**

```ts
// format wins — output is JPEG even though quality=100
await resize(src, 800, 600, 100, { format: 'jpeg' });

// no format — quality=100 heuristic applies → PNG (backward compat)
await resize(src, 800, 600, 100);
```

## keepMeta — EXIF preservation

When `keepMeta: true`, all EXIF tags present in the source JPEG are copied to the output JPEG after encoding. This includes GPS coordinates, camera make/model, capture date, and any other tags supported by `androidx.exifinterface`.

**Behavior by scenario:**

| Scenario | Result |
| --- | --- |
| `keepMeta: true` + JPEG output | EXIF tags copied; `Orientation` reset to normal |
| `keepMeta: true` + PNG output | No-op — PNG has no standard EXIF |
| `keepMeta: true` + WebP output | No-op — consistent with PNG behaviour |
| `keepMeta: true` + iOS | No-op — iOS implementation pending |
| `keepMeta: false` (default) | No EXIF copy; identical to previous behavior |
| Source has no EXIF | Succeeds silently — output has no EXIF |

> **Note:** `Orientation` is always reset to `1` (normal) in the output, because the bitmap was decoded and re-encoded with the correct orientation already applied.

## Platform notes

| Platform    | Backend                      | keepMeta | WebP output |
| ----------- | ---------------------------- | -------- | ----------- |
| Android     | libyuv (`ARGBScale`) via NDK | ✅       | ✅ (`WEBP_LOSSY` API 30+, `WEBP` fallback) |
| iOS         | Not yet implemented          | no-op    | no-op (produces JPEG) |
| Web / other | Throws — native only         | —        | — |

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
