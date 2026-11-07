import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ImageCompressorUtil, CompressionResult } from './image-compressor.util';

describe('ImageCompressorUtil (WebP Compression & BT.601 Luminance)', () => {
  describe('calculateScaledDimensions', () => {
    it('TC-IMG-1: Dimensions within 1920px are not scaled up', () => {
      const { width, height } = ImageCompressorUtil.calculateScaledDimensions(800, 600, 1920);
      expect(width).toBe(800);
      expect(height).toBe(600);
    });

    it('TC-IMG-2: Wide landscape image scales width to 1920px preserving ratio', () => {
      const { width, height } = ImageCompressorUtil.calculateScaledDimensions(3840, 1920, 1920);
      expect(width).toBe(1920);
      expect(height).toBe(960);
    });

    it('TC-IMG-3: Tall portrait image scales height to 1920px preserving ratio', () => {
      const { width, height } = ImageCompressorUtil.calculateScaledDimensions(1080, 2160, 1920);
      expect(width).toBe(960);
      expect(height).toBe(1920);
    });
  });

  describe('computeLuminanceFromRgba', () => {
    it('TC-IMG-4: Pure dark pixels return average luminance < 40', () => {
      // 4 pixels: R=10, G=10, B=10, A=255
      const rgba = new Uint8ClampedArray([
        10, 10, 10, 255,
        15, 15, 15, 255,
        20, 20, 20, 255,
        5, 5, 5, 255
      ]);
      const luminance = ImageCompressorUtil.computeLuminanceFromRgba(rgba);
      expect(luminance).toBeLessThan(40);
      expect(luminance).toBe(13);
    });

    it('TC-IMG-5: Bright daylight pixels return average luminance >= 40', () => {
      // 2 pixels: R=180, G=200, B=150, A=255
      const rgba = new Uint8ClampedArray([
        180, 200, 150, 255,
        220, 210, 190, 255
      ]);
      const luminance = ImageCompressorUtil.computeLuminanceFromRgba(rgba);
      expect(luminance).toBeGreaterThanOrEqual(40);
      expect(luminance).toBeGreaterThan(150);
    });

    it('TC-IMG-6: ITU-R BT.601 formula weights Green highest', () => {
      // Pure Green: 0, 100, 0, 255 => 0.587 * 100 = 58.7 -> 59
      const pureGreen = new Uint8ClampedArray([0, 100, 0, 255]);
      // Pure Blue: 0, 0, 100, 255 => 0.114 * 100 = 11.4 -> 11
      const pureBlue = new Uint8ClampedArray([0, 0, 100, 255]);

      expect(ImageCompressorUtil.computeLuminanceFromRgba(pureGreen)).toBe(59);
      expect(ImageCompressorUtil.computeLuminanceFromRgba(pureBlue)).toBe(11);
    });
  });

  describe('compressImage', () => {
    beforeEach(() => {
      vi.restoreAllMocks();
    });

    it('TC-IMG-7: Compresses image file to WebP with target quality and metadata', async () => {
      const mockBlob = new Blob(['mock-binary-webp-data'], { type: 'image/webp' });

      // Mock canvas
      const mockCanvas: any = {
        width: 1920,
        height: 1080,
        getContext: vi.fn().mockReturnValue({
          drawImage: vi.fn(),
          getImageData: vi.fn().mockReturnValue({
            data: new Uint8ClampedArray([120, 130, 140, 255, 150, 160, 170, 255])
          })
        }),
        toBlob: vi.fn().mockImplementation((cb: (b: Blob) => void, type: string) => {
          cb(mockBlob);
        }),
        toDataURL: vi.fn().mockReturnValue('data:image/webp;base64,mock')
      };

      vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
        if (tag === 'canvas') return mockCanvas;
        return document.createElement(tag);
      });

      // Mock createImageBitmap
      (globalThis as any).createImageBitmap = vi.fn().mockResolvedValue({
        width: 3840,
        height: 2160,
        close: vi.fn()
      });

      const inputFile = new File(['fake-input'], 'foto-moto.jpg', { type: 'image/jpeg' });
      const result: CompressionResult = await ImageCompressorUtil.compressImage(inputFile);

      expect(result.width).toBe(1920);
      expect(result.height).toBe(1080);
      expect(result.blob).toBe(mockBlob);
      expect(result.dataUrl).toContain('data:image/webp;base64');
      expect(result.averageLuminance).toBeGreaterThan(40);
    });

    it('TC-IMG-8: Lowers quality to 0.70 if initial compressed blob exceeds 500KB', async () => {
      const largeBlob = new Blob([new Uint8Array(600 * 1024)], { type: 'image/webp' });
      const smallBlob = new Blob([new Uint8Array(400 * 1024)], { type: 'image/webp' });

      let callCount = 0;
      const mockCanvas: any = {
        width: 1920,
        height: 1080,
        getContext: vi.fn().mockReturnValue({
          drawImage: vi.fn(),
          getImageData: vi.fn().mockReturnValue({
            data: new Uint8ClampedArray([100, 100, 100, 255])
          })
        }),
        toBlob: vi.fn().mockImplementation((cb: (b: Blob) => void, type: string, quality: number) => {
          callCount++;
          if (callCount === 1) {
            expect(quality).toBe(0.82);
            cb(largeBlob);
          } else {
            expect(quality).toBe(0.70);
            cb(smallBlob);
          }
        }),
        toDataURL: vi.fn().mockReturnValue('data:image/webp;base64,compressed')
      };

      vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
        if (tag === 'canvas') return mockCanvas;
        return document.createElement(tag);
      });

      (globalThis as any).createImageBitmap = vi.fn().mockResolvedValue({
        width: 1920,
        height: 1080,
        close: vi.fn()
      });

      const inputFile = new File(['large-image'], 'photo.png', { type: 'image/png' });
      const result = await ImageCompressorUtil.compressImage(inputFile);

      expect(callCount).toBe(2);
      expect(result.sizeBytes).toBe(smallBlob.size);
    });
  });
});
