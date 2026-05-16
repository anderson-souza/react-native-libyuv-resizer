# react-native-libyuv-resizer

High-performance image resizer for React Native using libyuv (Android).

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
| `quality`            | `number`        | —           | JPEG quality `1–100`. Use `100` to output PNG instead of JPEG.                   |
| `options.rotation`   | `RotationAngle` | `0`         | Clockwise rotation before resize: `0 \| 90 \| 180 \| 270 \| -90 \| -180 \| -270` |
| `options.mode`       | `ResizeMode`    | `'contain'` | How the image fits the target box: `'contain' \| 'cover' \| 'stretch'`           |
| `options.filterMode` | `FilterMode`    | `'box'`     | Scaling filter: `'none' \| 'linear' \| 'bilinear' \| 'box'`                     |
| `options.outputPath` | `string`        | auto        | Absolute directory path for the output file. Auto-generated in cache if omitted. |
| `options.keepMeta`   | `boolean`       | `false`     | Copy EXIF metadata from source to output JPEG. Android only; no-op on iOS and PNG output. |

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

interface ResizeOptions {
  rotation?:   RotationAngle;
  mode?:       ResizeMode;
  filterMode?: FilterMode;
  outputPath?: string;
  keepMeta?:   boolean;
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

// PNG output (quality = 100)
const result = await resize('/path/to/photo.jpg', 800, 600, 100);
console.log(result.path); // ends with .png
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

## keepMeta — EXIF preservation

When `keepMeta: true`, all EXIF tags present in the source JPEG are copied to the output JPEG after encoding. This includes GPS coordinates, camera make/model, capture date, and any other tags supported by `androidx.exifinterface`.

**Behavior by scenario:**

| Scenario | Result |
| --- | --- |
| `keepMeta: true` + JPEG output | EXIF tags copied; `Orientation` reset to normal |
| `keepMeta: true` + PNG output (`quality=100`) | No-op — PNG has no standard EXIF |
| `keepMeta: true` + iOS | No-op — iOS implementation pending |
| `keepMeta: false` (default) | No EXIF copy; identical to previous behavior |
| Source has no EXIF | Succeeds silently — output has no EXIF |

> **Note:** `Orientation` is always reset to `1` (normal) in the output, because the bitmap was decoded and re-encoded with the correct orientation already applied.

## Platform notes

| Platform    | Backend                      | keepMeta |
| ----------- | ---------------------------- | -------- |
| Android     | libyuv (`ARGBScale`) via NDK | ✅ |
| iOS         | Not yet implemented          | no-op |
| Web / other | Throws — native only         | — |

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
