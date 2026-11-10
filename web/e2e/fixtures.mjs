import { test as base, expect } from '@playwright/test';

export const test = base.extend({
  runtimeErrors: [async ({ page }, use) => {
    const errors = [];
    const collect = error => errors.push(error.message);
    page.on('pageerror', collect);
    await use();
    page.off('pageerror', collect);
    expect(errors, 'Uncaught frontend runtime errors').toEqual([]);
  }, { auto: true }],
  merchantPage: async ({ page }, use) => {
    test.skip(!process.env.E2E_EMAIL || !process.env.E2E_PASSWORD || !process.env.E2E_MERCHANT_NAME,
      'Set E2E_EMAIL, E2E_PASSWORD and E2E_MERCHANT_NAME; a READY test merchant is required.');
    await page.goto('/login');
    // Login's existing v-labels are not associated with the inputs; scope to its form.
    const form = page.locator('form');
    await form.locator('input:not([type="password"]):not([type="hidden"])').fill(process.env.E2E_EMAIL);
    await form.locator('input[type="password"]').fill(process.env.E2E_PASSWORD);
    await form.getByRole('button', { name: /^(Masuk|Sign in)$/i }).click();
    await expect(page).toHaveURL(/\/merchant\/?$/);
    await expect(page.locator('.dashboard-store-name').getByRole('button')).toHaveText(process.env.E2E_MERCHANT_NAME);
    await expect(page.locator('.profileBtn')).toBeVisible();
    await use(page);
  },
});
export { expect };
export async function expectNoPageOverflow(page) {
  const dimensions = await page.evaluate(() => ({ width: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
  expect(dimensions.scroll, 'The document should not scroll horizontally (tables may scroll internally)').toBeLessThanOrEqual(dimensions.width + 1);
}
