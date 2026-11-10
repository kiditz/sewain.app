import { defineConfig } from '@playwright/test';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

if (process.env.E2E_REQUIRE_AUTH === 'true' && (!process.env.E2E_EMAIL || !process.env.E2E_PASSWORD || !process.env.E2E_MERCHANT_NAME)) {
  throw new Error('Set E2E_EMAIL, E2E_PASSWORD and E2E_MERCHANT_NAME for authenticated E2E.');
}
export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: !!process.env.CI,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  outputDir: process.env.E2E_OUTPUT_DIR || join(tmpdir(), 'sewain-e2e-results'),
  reporter: [['list'], ['html', { open: 'never', outputFolder: join(tmpdir(), 'sewain-e2e-report') }]],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://sewain.app.localhost:3000',
    browserName: 'chromium',
    locale: 'id-ID',
    trace: 'off', video: 'off', screenshot: 'only-on-failure',
    actionTimeout: 15_000, navigationTimeout: 30_000,
    serviceWorkers: 'block',
  },
  projects: [
    { name: 'desktop', testMatch: ['public.spec.mjs', 'merchant.spec.mjs'], use: { viewport: { width: 1440, height: 1000 } } },
    { name: 'mobile', testMatch: ['public.spec.mjs', 'responsive.spec.mjs'], use: { viewport: { width: 375, height: 812 }, isMobile: true, hasTouch: true } },
    { name: 'tablet', testMatch: 'responsive.spec.mjs', use: { viewport: { width: 768, height: 1024 }, hasTouch: true } },
  ],
  // Intentionally no webServer: run against the user's existing Docker stack.
});
