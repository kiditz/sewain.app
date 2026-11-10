import { test, expect, expectNoPageOverflow } from '../fixtures.mjs';

test('login renders and blocks empty or malformed credentials without calling auth API', async ({ page }) => {
  let requests = 0;
  page.on('request', request => { if (new URL(request.url()).pathname === '/api/auth/login') requests++; });
  const response = await page.goto('/login');
  expect(response?.status()).toBe(200);
  const form = page.locator('form');
  const submit = form.getByRole('button', { name: /^(Masuk|Sign in)$/i });
  await expect(submit).toBeDisabled();
  await form.locator('input:not([type="password"]):not([type="hidden"])').fill('invalid-email');
  await form.locator('input[type="password"]').fill('short');
  await expect(submit).toBeDisabled();
  expect(requests).toBe(0);
  await expectNoPageOverflow(page);
});

for (const path of ['/merchant/products', '/merchant/orders', '/merchant/integrations/whatsapp', '/merchant/products/import']) {
  test(`anonymous visitor is redirected from ${path} to login`, async ({ page }) => {
    await page.goto(path);
    await expect(page).toHaveURL(/\/login\/?$/);
    await expect(page.locator('form input[type="password"]')).toBeVisible();
    expect((await page.context().cookies()).some(cookie => cookie.name === 'access_token')).toBe(false);
  });
}

test('registration page can be reached from login without creating an account', async ({ page }) => {
  await page.goto('/login');
  await page.locator('a[href="/register"]').click();
  await expect(page).toHaveURL(/\/register\/?$/);
  await expect(page.locator('form input[type="password"]')).toBeVisible();
  await expectNoPageOverflow(page);
});
