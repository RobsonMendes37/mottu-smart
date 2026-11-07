export interface CompressionResult {
  blob: Blob;
  dataUrl: string;
  sizeBytes: number;
  width: number;
  height: number;
  averageLuminance: number;
}

export class ImageCompressorUtil {
  public static readonly MAX_DIMENSION = 1920;
  public static readonly TARGET_QUALITY = 0.82;
  public static readonly REDUCED_QUALITY = 0.70;
  public static readonly MAX_PAYLOAD_BYTES = 500 * 1024; // 500 KB limit
  public static readonly MIN_ACCEPTABLE_LUMINANCE = 40;

  /**
   * Resizes and compresses an image to WebP with canvas.
   * If initial WebP payload > 500KB, attempts recompression at quality 0.70.
   */
  public static async compressImage(file: File | Blob): Promise<CompressionResult> {
    let sourceWidth = 0;
    let sourceHeight = 0;
    let imageSource: ImageBitmap | HTMLImageElement;

    if (typeof createImageBitmap === 'function') {
      const bitmap = await createImageBitmap(file);
      sourceWidth = bitmap.width;
      sourceHeight = bitmap.height;
      imageSource = bitmap;
    } else {
      // Fallback for environments where createImageBitmap is unavailable
      const img = await this.loadImageElement(file);
      sourceWidth = img.naturalWidth || img.width;
      sourceHeight = img.naturalHeight || img.height;
      imageSource = img;
    }

    const { width, height } = this.calculateScaledDimensions(
      sourceWidth,
      sourceHeight,
      this.MAX_DIMENSION
    );

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Canvas 2D context not available');
    }

    ctx.drawImage(imageSource, 0, 0, width, height);

    // Compute luminance from sampled grid
    const sampleWidth = Math.min(width, 100);
    const sampleHeight = Math.min(height, 100);
    const imgData = ctx.getImageData(0, 0, sampleWidth, sampleHeight);
    const averageLuminance = this.computeLuminanceFromRgba(imgData.data);

    // Initial compression
    let quality = this.TARGET_QUALITY;
    let blob = await this.toBlobWithFallback(canvas, quality);

    // If payload exceeds 500 KB, recompress with reduced quality
    if (blob.size > this.MAX_PAYLOAD_BYTES) {
      quality = this.REDUCED_QUALITY;
      blob = await this.toBlobWithFallback(canvas, quality);
    }

    let dataUrl = '';
    try {
      dataUrl = canvas.toDataURL('image/webp', quality);
    } catch {
      dataUrl = canvas.toDataURL('image/jpeg', quality);
    }

    // Clean up ImageBitmap if applicable
    if ('close' in imageSource && typeof (imageSource as ImageBitmap).close === 'function') {
      (imageSource as ImageBitmap).close();
    }

    return {
      blob,
      dataUrl,
      sizeBytes: blob.size,
      width,
      height,
      averageLuminance,
    };
  }

  /**
   * Calculates new dimensions preserving aspect ratio with maximum boundary.
   */
  public static calculateScaledDimensions(
    width: number,
    height: number,
    maxDimension: number = 1920
  ): { width: number; height: number } {
    if (width <= maxDimension && height <= maxDimension) {
      return { width, height };
    }

    if (width > height) {
      const scaledHeight = Math.round((height * maxDimension) / width);
      return { width: maxDimension, height: scaledHeight };
    } else {
      const scaledWidth = Math.round((width * maxDimension) / height);
      return { width: scaledWidth, height: maxDimension };
    }
  }

  /**
   * Computes average luminance using ITU-R BT.601 standard:
   * Y = 0.299*R + 0.587*G + 0.114*B
   */
  public static computeLuminanceFromRgba(data: Uint8ClampedArray): number {
    if (data.length === 0) return 0;
    let total = 0;
    const pixelCount = data.length / 4;

    for (let i = 0; i < data.length; i += 4) {
      const r = data[i];
      const g = data[i + 1];
      const b = data[i + 2];
      total += 0.299 * r + 0.587 * g + 0.114 * b;
    }

    return Math.round(total / pixelCount);
  }

  private static toBlobWithFallback(canvas: HTMLCanvasElement, quality: number): Promise<Blob> {
    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob);
          } else {
            // Fallback to image/jpeg if webp conversion fails
            canvas.toBlob(
              (jpegBlob) => {
                if (jpegBlob) resolve(jpegBlob);
                else reject(new Error('Canvas toBlob failed for both WebP and JPEG'));
              },
              'image/jpeg',
              quality
            );
          }
        },
        'image/webp',
        quality
      );
    });
  }

  private static loadImageElement(file: File | Blob): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(file);
      img.onload = () => {
        URL.revokeObjectURL(url);
        resolve(img);
      };
      img.onerror = (e) => {
        URL.revokeObjectURL(url);
        reject(new Error('Failed to load image file into Image element'));
      };
      img.src = url;
    });
  }
}
