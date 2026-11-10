import { test, expect, expectNoPageOverflow } from '../fixtures.mjs';

test('mobile/tablet drawer opens and closes after navigation and reload', async ({ merchantPage: page }) => {
  const toggle = page.locator('button[aria-controls="dashboard-navigation"]');
  await expect(toggle).toBeVisible();
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await toggle.click();
  await expect(toggle).toHaveAttribute('aria-expanded', 'true');
  const products = page.locator('#dashboard-navigation a[href="/merchant/products"]');
  await expect(products).toBeVisible();
  await products.click();
  await expect(page).toHaveURL(/\/merchant\/products\/?$/);
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await expectNoPageOverflow(page);
  await page.reload();
  await expect(toggle).toBeVisible();
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await expect(page.locator('.dashboard-store-name')).toContainText(process.env.E2E_MERCHANT_NAME);
});

test('import page fits viewport and file picker remains usable', async ({ merchantPage: page }) => {
  await page.goto('/merchant/products/import');
  const input = page.getByLabel('File CSV', { exact: true });
  await expect(input).toBeVisible();
  await expectNoPageOverflow(page);
  const bounds = await input.boundingBox();
  expect(bounds).not.toBeNull();
  expect(bounds.x).toBeGreaterThanOrEqual(0);
  expect(bounds.x + bounds.width).toBeLessThanOrEqual(page.viewportSize().width + 1);
});
