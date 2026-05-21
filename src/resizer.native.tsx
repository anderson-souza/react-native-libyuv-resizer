import LibyuvResizer, { type ResizeResult } from './NativeLibyuvResizer';

export type { ResizeResult };

/**
 * Valid rotation values in degrees.
 * Negative angles are normalised to their positive equivalents before being
 * sent to the native layer (`-90` → `270`, etc.).
 */
export type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;

/**
 * Controls how the source image is fitted into the target bounding box.
 *
 * - `'contain'` — scales uniformly so the entire image fits within the box,
 *   preserving aspect ratio. Empty space is left uncropped.
 * - `'cover'` — scales uniformly so the image fills the box, preserving
 *   aspect ratio. Excess pixels are cropped.
 * - `'stretch'` — scales to exactly `targetWidth × targetHeight`, ignoring
 *   aspect ratio.
 */
export type ResizeMode = 'contain' | 'cover' | 'stretch';

/**
 * Scaling filter applied during the resize operation.
 *
 * Higher quality filters are slower. Choose based on your latency vs quality
 * requirements.
 *
 * - `'none'` — nearest-neighbour; fastest, lowest quality.
 * - `'linear'` — linear interpolation.
 * - `'bilinear'` — bilinear interpolation.
 * - `'box'` — box filter; best quality for downscaling *(default)*.
 */
export type FilterMode = 'none' | 'linear' | 'bilinear' | 'box';

/**
 * Output image format.
 *
 * When omitted, format is derived from `quality`: `quality === 100` → `'png'`, else `'jpeg'`.
 * When specified, this value takes precedence over the quality-based heuristic.
 *
 * - `'jpeg'` — lossy JPEG.
 * - `'png'`  — lossless PNG. `quality` is ignored.
 * - `'webp'` — lossy WebP. `quality` controls compression level *(Android only; iOS produces JPEG)*.
 */
export type OutputFormat = 'jpeg' | 'png' | 'webp';

/** Options accepted by {@link resize}. */
export interface ResizeOptions {
  /**
   * Clockwise rotation applied to the image **before** resizing.
   * @default 0
   */
  rotation?: RotationAngle;

  /**
   * How the image is fitted into the target bounding box.
   * @default 'contain'
   */
  mode?: ResizeMode;

  /**
   * Scaling filter used during the resize operation.
   * @default 'box'
   */
  filterMode?: FilterMode;

  /**
   * Full absolute path for the output file, including directory, filename,
   * and extension (e.g. `'/sdcard/Pictures/thumb.jpg'`).
   * The parent directory must already exist.
   * When omitted the native layer generates a path in the app's cache
   * directory automatically.
   */
  outputPath?: string;

  /**
   * When `true`, copies all EXIF tags from the source image to the output
   * JPEG. Has no effect on PNG or WebP output, or on iOS (no-op).
   * @default false
   * @platform android
   */
  keepMeta?: boolean;

  /**
   * Output image format. When omitted, `quality === 100` produces PNG; otherwise JPEG.
   * Specifying `format` takes precedence over the quality-based heuristic.
   * @default derived from quality
   */
  format?: OutputFormat;
}

const VALID_MODES: ResizeMode[] = ['contain', 'cover', 'stretch'];
const VALID_FILTER_MODES: FilterMode[] = ['none', 'linear', 'bilinear', 'box'];
const VALID_FORMATS: OutputFormat[] = ['jpeg', 'png', 'webp'];

/** Normalises negative or out-of-range angles to 0 | 90 | 180 | 270. */
function toCanonicalAngle(angle: RotationAngle): 0 | 90 | 180 | 270 {
  return (((angle % 360) + 360) % 360) as 0 | 90 | 180 | 270;
}

/**
 * Resizes an image using the libyuv native backend (Android).
 *
 * The source image is read from `filePath`, resized to the requested
 * dimensions, and saved as a JPEG. The absolute path of the output file is
 * returned on success.
 *
 * @param filePath - Absolute path to the source image.
 * @param targetWidth - Output width in pixels (must be > 0).
 * @param targetHeight - Output height in pixels (must be > 0).
 * @param quality - JPEG encoding quality from `1` (lowest) to `100` (highest).
 *   Use `100` to produce PNG output instead of JPEG.
 * @param options - Optional resize behaviour overrides.
 * @returns A `Promise` that resolves to a {@link ResizeResult} with the output
 *   file path, URI, size, name, and dimensions.
 * @throws {TypeError} When `options.mode` or `options.filterMode` is not one
 *   of the accepted string literals.
 *
 * @example
 * ```ts
 * // Basic resize
 * const result = await resize('/path/to/photo.jpg', 1280, 720, 85);
 * console.log(result.path, result.width, result.height);
 *
 * // With options
 * const result = await resize('/path/to/photo.jpg', 800, 600, 80, {
 *   rotation: 90,
 *   mode: 'cover',
 *   filterMode: 'bilinear',
 *   outputPath: '/path/to/output/thumb.jpg',
 * });
 *
 * // Preserve EXIF (GPS, camera, date) — Android only
 * const result = await resize('/path/to/photo.jpg', 800, 600, 80, {
 *   keepMeta: true,
 * });
 * ```
 */
export function resize(
  filePath: string,
  targetWidth: number,
  targetHeight: number,
  quality: number,
  options?: ResizeOptions
): Promise<ResizeResult> {
  const rotation =
    options?.rotation != null ? toCanonicalAngle(options.rotation) : 0;
  const mode: ResizeMode = options?.mode ?? 'contain';
  if (!VALID_MODES.includes(mode)) {
    return Promise.reject(new TypeError(`Invalid resize mode: '${mode}'`));
  }
  const filterMode: FilterMode = options?.filterMode ?? 'box';
  if (!VALID_FILTER_MODES.includes(filterMode)) {
    return Promise.reject(
      new TypeError(`Invalid filter mode: '${filterMode}'`)
    );
  }
  const format: OutputFormat =
    options?.format ?? (quality === 100 ? 'png' : 'jpeg');
  if (!VALID_FORMATS.includes(format)) {
    return Promise.reject(new TypeError(`Invalid format: '${format}'`));
  }
  return LibyuvResizer.resize(
    filePath,
    targetWidth,
    targetHeight,
    quality,
    rotation,
    mode,
    options?.outputPath ?? '',
    filterMode,
    options?.keepMeta ?? false,
    format
  );
}
