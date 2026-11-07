import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InspectionUploadService } from './inspection-upload.service';
import { RiderService } from './rider.service';
import { ImageCompressorUtil } from '../utils/image-compressor.util';
import { of } from 'rxjs';

describe('InspectionUploadService (WebP compression + Presigned direct PUT)', () => {
  let uploadService: InspectionUploadService;
  let mockRiderService: any;

  beforeEach(() => {
    mockRiderService = {
      getUploadUrl: vi.fn(),
    };
    uploadService = new InspectionUploadService(mockRiderService);
  });

  it('TC-UPL-1: Coordinates compression, presigned URL request and direct binary PUT without Bearer header', async () => {
    const fakeBlob = new Blob(['mock-webp-payload'], { type: 'image/webp' });
    const mockCompression = {
      blob: fakeBlob,
      dataUrl: 'data:image/webp;base64,mock',
      sizeBytes: 1024,
      width: 1920,
      height: 1080,
      averageLuminance: 65,
    };

    vi.spyOn(ImageCompressorUtil, 'compressImage').mockResolvedValue(mockCompression);

    mockRiderService.getUploadUrl.mockReturnValue(
      of({
        uploadUrl: 'http://localhost:9000/inspections/raw-photo.webp?X-Amz-Signature=xyz',
        photoObjectKey: 'inspections/temp/raw-photo.webp',
        expiresInSeconds: 300,
      })
    );

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
    });
    vi.stubGlobal('fetch', mockFetch);

    const inputFile = new File(['mock-camera-photo'], 'foto.jpg', { type: 'image/jpeg' });
    const result = await uploadService.uploadPhotoSlot(inputFile, 'damage');

    expect(ImageCompressorUtil.compressImage).toHaveBeenCalledWith(inputFile);
    expect(mockRiderService.getUploadUrl).toHaveBeenCalledWith({
      contentType: 'image/webp',
      fileSizeBytes: 1024,
      slot: 'damage',
    });

    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:9000/inspections/raw-photo.webp?X-Amz-Signature=xyz',
      expect.objectContaining({
        method: 'PUT',
        headers: {
          'Content-Type': 'image/webp',
        },
        body: fakeBlob,
      })
    );

    // Verify Bearer token was NOT sent in fetch headers
    const fetchHeaders = mockFetch.mock.calls[0][1].headers;
    expect(fetchHeaders['Authorization']).toBeUndefined();

    expect(result.photoObjectKey).toBe('inspections/temp/raw-photo.webp');
    expect(result.compression).toEqual(mockCompression);

    vi.unstubAllGlobals();
  });

  it('TC-UPL-2: Throws descriptive error if storage PUT fails', async () => {
    vi.spyOn(ImageCompressorUtil, 'compressImage').mockResolvedValue({
      blob: new Blob(['data']),
      dataUrl: 'data:...',
      sizeBytes: 100,
      width: 800,
      height: 600,
      averageLuminance: 50,
    });

    mockRiderService.getUploadUrl.mockReturnValue(
      of({
        uploadUrl: 'http://localhost:9000/fail',
        photoObjectKey: 'key',
        expiresInSeconds: 300,
      })
    );

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
      })
    );

    const inputFile = new File(['x'], 'x.jpg');
    await expect(uploadService.uploadPhotoSlot(inputFile, 'odometer')).rejects.toThrow(
      'Falha no upload para o storage S3 (HTTP 403)'
    );

    vi.unstubAllGlobals();
  });
});
