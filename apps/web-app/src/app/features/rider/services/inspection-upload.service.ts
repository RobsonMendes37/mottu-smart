import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RiderService } from './rider.service';
import { ImageCompressorUtil, CompressionResult } from '../utils/image-compressor.util';

export interface PhotoSlotUploadResult {
  photoObjectKey: string;
  compression: CompressionResult;
}

@Injectable({ providedIn: 'root' })
export class InspectionUploadService {
  private riderService: RiderService;

  constructor(riderService?: RiderService) {
    try {
      this.riderService = riderService ?? inject(RiderService);
    } catch {
      this.riderService = riderService as RiderService;
    }
  }

  /**
   * Compresses an image to WebP and uploads it directly to the presigned storage URL.
   * Direct PUT is performed via fetch to bypass any Angular Bearer auth interceptors.
   */
  public async uploadPhotoSlot(
    file: File | Blob,
    slot: string = 'damage'
  ): Promise<PhotoSlotUploadResult> {
    // 1. Client-side canvas compression to WebP (< 500KB, quality 0.82)
    const compression = await ImageCompressorUtil.compressImage(file);

    // 2. Request presigned upload URL from Core API
    const presigned = await firstValueFrom(
      this.riderService.getUploadUrl({
        contentType: 'image/webp',
        fileSizeBytes: compression.sizeBytes,
        slot,
      })
    );

    // 3. Resolve upload URL to match current page's protocol and host (prevents Mixed Content & CORS blocks)
    let uploadUrl = presigned.uploadUrl;
    if (typeof window !== 'undefined' && window.location) {
      try {
        const parsedUrl = new URL(uploadUrl);
        parsedUrl.protocol = window.location.protocol;
        if (window.location.hostname && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
          parsedUrl.host = window.location.host;
        }
        uploadUrl = parsedUrl.toString();
      } catch {
        if (window.location.protocol === 'https:' && uploadUrl.startsWith('http://')) {
          uploadUrl = uploadUrl.replace('http://', 'https://');
        }
      }
    }

    // 4. Direct binary PUT to storage URL (WITHOUT Authorization header)
    const storageResponse = await fetch(uploadUrl, {
      method: 'PUT',
      headers: {
        'Content-Type': 'image/webp',
      },
      body: compression.blob,
    });

    if (!storageResponse.ok) {
      throw new Error(`Falha no upload para o storage S3 (HTTP ${storageResponse.status})`);
    }

    return {
      photoObjectKey: presigned.photoObjectKey,
      compression,
    };
  }
}
