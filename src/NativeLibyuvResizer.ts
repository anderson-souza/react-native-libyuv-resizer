import {
  NativeModules,
  TurboModuleRegistry,
  type TurboModule,
} from 'react-native';

/**
 * Metadata returned by the native `resize()` bridge call.
 *
 * All numeric fields reflect the **output** file / bitmap — not the source.
 */
export type ResizeResult = {
  /** Absolute path of the resized image file. */
  path: string;
  /** `file://` URI of the resized image file. */
  uri: string;
  /** File size in bytes. */
  size: number;
  /** File name (e.g. `"a3f2…1c.jpg"`). */
  name: string;
  /** Output bitmap width in pixels. */
  width: number;
  /** Output bitmap height in pixels. */
  height: number;
};

/**
 * Turbo Module contract for the native `LibyuvResizer` implementation.
 *
 * **Do not call this interface directly.** Use the public {@link resize}
 * function from `./resizer` (or the package root) instead — it validates
 * arguments and applies sensible defaults before forwarding to this bridge.
 *
 * This interface is consumed by React Native codegen to auto-generate the
 * JSI glue code (`LibyuvResizerSpec`).
 */
export interface Spec extends TurboModule {
  /**
   * Low-level bridge method. Parameter order mirrors the public `resize()`
   * signature with defaults already resolved.
   *
   * @param filePath    - Absolute path to the source image.
   * @param targetWidth - Output width in pixels.
   * @param targetHeight - Output height in pixels.
   * @param quality     - JPEG/WebP quality `0–100`.
   * @param rotation    - Canonical rotation in degrees (`0 | 90 | 180 | 270`).
   * @param mode        - Resize mode string (`'contain' | 'cover' | 'stretch'`).
   * @param outputPath  - Absolute output path, or empty string for auto.
   * @param filterMode  - Scaling filter (`'none' | 'linear' | 'bilinear' | 'box'`).
   * @param keepMeta    - Copy EXIF tags from source to output (JPEG only, Android only).
   * @param format      - Output format (`'jpeg' | 'png' | 'webp'`).
   * @returns Metadata about the resized image.
   */
  resize(
    filePath: string,
    targetWidth: number,
    targetHeight: number,
    quality: number,
    rotation: number,
    mode: string,
    outputPath: string,
    filterMode: string,
    keepMeta: boolean,
    format: string
  ): Promise<ResizeResult>;
}

const isTurboModuleEnabled =
  (globalThis as Record<string, unknown>).__turboModuleProxy != null;

const LibyuvResizerModule: Spec = isTurboModuleEnabled
  ? TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer')
  : (NativeModules.LibyuvResizer as Spec);

if (!LibyuvResizerModule) {
  throw new Error(
    'react-native-libyuv-resizer: native module not found. Did you forget to link the library?'
  );
}

export default LibyuvResizerModule;
