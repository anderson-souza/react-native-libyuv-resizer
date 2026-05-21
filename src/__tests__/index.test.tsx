import { describe, it, expect, jest, beforeEach } from '@jest/globals';
import type { ResizeResult } from '../NativeLibyuvResizer';

const MOCK_RESULT: ResizeResult = {
  path: '/out.jpg',
  uri: 'file:///out.jpg',
  size: 1024,
  name: 'out.jpg',
  width: 400,
  height: 225,
};

const mockResize = jest
  .fn<(...args: unknown[]) => Promise<ResizeResult>>()
  .mockResolvedValue(MOCK_RESULT);

jest.mock('../NativeLibyuvResizer', () => ({
  __esModule: true,
  default: { resize: mockResize },
}));

// Import the native implementation directly — importing from '../index' would resolve
// to '../resizer.tsx' (web fallback) because Jest does not resolve .native extensions
// by default. The public API surface (index.tsx re-exports) is covered by typecheck.
const { resize } = require('../resizer.native');

beforeEach(() => {
  mockResize.mockClear();
});

describe('resize mode', () => {
  describe('contain (default)', () => {
    it('landscape image in square target → width=targetW, height reduced', async () => {
      // 1920×1080 into 400×400 → scale=min(400/1920, 400/1080)=min(0.208,0.370)=0.208 → 400×225
      await resize('/img.jpg', 400, 400, 80);
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'contain',
        '',
        'box',
        false,
        'jpeg'
      );
    });

    it('omitting mode defaults to contain', async () => {
      await resize('/img.jpg', 400, 400, 80, {});
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'contain',
        '',
        'box',
        false,
        'jpeg'
      );
    });

    it('explicit contain forwards correctly', async () => {
      await resize('/img.jpg', 400, 400, 80, { mode: 'contain' });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'contain',
        '',
        'box',
        false,
        'jpeg'
      );
    });
  });

  describe('cover', () => {
    it('forwards cover to native', async () => {
      await resize('/img.jpg', 400, 400, 80, { mode: 'cover' });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'cover',
        '',
        'box',
        false,
        'jpeg'
      );
    });
  });

  describe('stretch', () => {
    it('forwards stretch to native', async () => {
      await resize('/img.jpg', 400, 400, 80, { mode: 'stretch' });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'stretch',
        '',
        'box',
        false,
        'jpeg'
      );
    });
  });

  describe('invalid mode', () => {
    it('throws TypeError and does not call native', async () => {
      await expect(
        resize('/img.jpg', 400, 400, 80, { mode: 'fill' as any })
      ).rejects.toThrow(TypeError);
      expect(mockResize).not.toHaveBeenCalled();
    });

    it('error message includes the invalid mode value', async () => {
      await expect(
        resize('/img.jpg', 400, 400, 80, { mode: 'fill' as any })
      ).rejects.toThrow("Invalid resize mode: 'fill'");
    });
  });

  describe('rotation passthrough', () => {
    it('normalises negative rotation and forwards with mode', async () => {
      await resize('/img.jpg', 400, 400, 80, {
        rotation: -90,
        mode: 'stretch',
      });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        270,
        'stretch',
        '',
        'box',
        false,
        'jpeg'
      );
    });
  });
});

describe('outputPath', () => {
  it('omitting outputPath passes empty string to native', async () => {
    await resize('/img.jpg', 400, 400, 80);
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('provided outputPath is forwarded to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { outputPath: '/tmp/out.jpg' });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '/tmp/out.jpg',
      'box',
      false,
      'jpeg'
    );
  });

  it('empty string outputPath passes empty string to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { outputPath: '' });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('outputPath combined with mode and rotation', async () => {
    await resize('/img.jpg', 800, 600, 90, {
      mode: 'cover',
      rotation: 90,
      outputPath: '/sdcard/Pictures/resized.jpg',
    });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      800,
      600,
      90,
      90,
      'cover',
      '/sdcard/Pictures/resized.jpg',
      'box',
      false,
      'jpeg'
    );
  });
});

describe('keepMeta', () => {
  it('omitting keepMeta passes false to native', async () => {
    await resize('/img.jpg', 400, 400, 80);
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('keepMeta: true is forwarded to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { keepMeta: true });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      true,
      'jpeg'
    );
  });

  it('keepMeta: false is forwarded to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { keepMeta: false });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('keepMeta combined with outputPath and mode', async () => {
    await resize('/img.jpg', 800, 600, 85, {
      keepMeta: true,
      mode: 'cover',
      outputPath: '/sdcard/out.jpg',
    });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      800,
      600,
      85,
      0,
      'cover',
      '/sdcard/out.jpg',
      'box',
      true,
      'jpeg'
    );
  });
});

describe('format', () => {
  it('omitting format with quality<100 defaults to jpeg', async () => {
    await resize('/img.jpg', 400, 400, 80);
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('omitting format with quality=100 defaults to png', async () => {
    await resize('/img.jpg', 400, 400, 100);
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      100,
      0,
      'contain',
      '',
      'box',
      false,
      'png'
    );
  });

  it('format webp is forwarded to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { format: 'webp' });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'webp'
    );
  });

  it('format jpeg explicit is forwarded to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { format: 'jpeg' });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('format png explicit is forwarded to native', async () => {
    await resize('/img.jpg', 400, 400, 80, { format: 'png' });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      80,
      0,
      'contain',
      '',
      'box',
      false,
      'png'
    );
  });

  it('format jpeg wins over quality=100 heuristic', async () => {
    await resize('/img.jpg', 400, 400, 100, { format: 'jpeg' });
    expect(mockResize).toHaveBeenCalledWith(
      '/img.jpg',
      400,
      400,
      100,
      0,
      'contain',
      '',
      'box',
      false,
      'jpeg'
    );
  });

  it('invalid format throws TypeError and does not call native', async () => {
    await expect(
      resize('/img.jpg', 400, 400, 80, { format: 'gif' as any })
    ).rejects.toThrow(TypeError);
    expect(mockResize).not.toHaveBeenCalled();
  });

  it('invalid format error message includes the bad value', async () => {
    await expect(
      resize('/img.jpg', 400, 400, 80, { format: 'gif' as any })
    ).rejects.toThrow("Invalid format: 'gif'");
  });
});

describe('return value shape', () => {
  it('resolves ResizeResult with all 6 fields', async () => {
    const result = await resize('/img.jpg', 400, 400, 80);
    expect(result).toEqual({
      path: MOCK_RESULT.path,
      uri: MOCK_RESULT.uri,
      size: MOCK_RESULT.size,
      name: MOCK_RESULT.name,
      width: MOCK_RESULT.width,
      height: MOCK_RESULT.height,
    });
  });
});
