import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // Ensure state transitions stay clean
  workers: 1, // Deterministic sequential runs for transactional state checks
  retries: 0,
  timeout: 30000,
  expect: {
    timeout: 5000,
  },
  reporter: [
    ['list'],
    ['json', { outputFile: 'test-results.json' }],
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:18088',
    trace: 'off',
    video: 'off',
    screenshot: 'off',
  },
});
