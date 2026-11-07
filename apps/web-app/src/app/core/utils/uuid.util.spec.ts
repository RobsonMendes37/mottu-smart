import { afterEach, describe, expect, it, vi } from 'vitest';
import { generateUuid } from './uuid.util';

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('generateUuid', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('uses native crypto.randomUUID when available (secure context)', () => {
    const native = vi.fn(() => '11111111-1111-4111-8111-111111111111');
    vi.stubGlobal('crypto', { randomUUID: native, getRandomValues: crypto.getRandomValues.bind(crypto) });

    expect(generateUuid()).toBe('11111111-1111-4111-8111-111111111111');
    expect(native).toHaveBeenCalledOnce();
  });

  it('falls back to getRandomValues on plain HTTP where randomUUID is undefined', () => {
    const realGetRandomValues = crypto.getRandomValues.bind(crypto);
    vi.stubGlobal('crypto', { getRandomValues: realGetRandomValues });

    const id = generateUuid();

    expect(id).toMatch(UUID_V4);
  });

  it('generates unique values in fallback mode', () => {
    vi.stubGlobal('crypto', { getRandomValues: crypto.getRandomValues.bind(crypto) });

    const ids = new Set(Array.from({ length: 500 }, () => generateUuid()));

    expect(ids.size).toBe(500);
  });
});
