import crypto from 'node:crypto';

// Minimal valid 1x1 transparent WebP image
export const VALID_WEBP_BASE64 =
  'UklGRkAAAABXRUJQVlA4IDQAAADwAQCdASoBAAEAAQAcJaACdLoAAP7/2QAA';

export const VALID_JPEG_BASE64 =
  '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=';

export const VALID_PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';

export function createMockImageBuffer(seed: string): { buffer: Buffer; sha256: string; dHash: string } {
  const hash = crypto.createHash('sha256').update(seed).digest('hex');
  const dHash = crypto.createHash('md5').update(seed).digest('hex').substring(0, 16);
  // create dummy 1KB buffer with seed
  const buffer = Buffer.alloc(1024, seed);
  return { buffer, sha256: hash, dHash };
}

// Precomputed known hashes
export const KNOWN_SUSPICIOUS_SHA256 =
  'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'; // hash of empty string / reused
export const KNOWN_DUPLICATE_SHA256 =
  'a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e';
