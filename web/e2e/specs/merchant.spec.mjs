import { readFile } from 'node:fs/promises';
import { test, expect, expectNoPageOverflow } from '../fixtures.mjs';

const menus = [
  ['/merchant/products', /Produk|Products/], ['/merchant/orders', /Pesanan|Orders/],
  ['/merchant/customers', /Pelanggan|Customers/], ['/merchant/inventory', /Inventaris|Inventory/],
  ['/merchant/collections', /Koleksi|Collections/], ['/merchant/finance', /Keuangan|Finance/],
  ['/merchant/reports', /Laporan|Reports/], ['/merchant/calendar', /Kalender|Calendar/],
  ['/merchant/pages', /Halaman|Pages/], ['/merchant/integrations', /Integrasi|Integrations/],
  ['/merchant/settings', /Pengaturan|Settings/], ['/merchant/settings/regional', /Regional/],
  ['/merchant/settings/rental', /Rental/], ['/merchant/settings/rental-plans', /Paket|Plans/],
  ['/merchant/settings/payments', /Pembayaran|Payments/],
];
for (const [path, title] of menus) {
  test(`merchant navigation opens ${path} with a page title`, async ({ merchantPage: page }) => {
    const responsePromise = page.waitForURL(url => url.pathname === path);
    await page.locator(`#dashboard-navigation a[href="${path}"]`).click();
    await responsePromise;
    await expect(page).toHaveTitle(title);
    await expect(page.locator('main')).toBeVisible();
    await expectNoPageOverflow(page);
  });
}

test('product import template downloads and invalid local file is rejected without upload', async ({ merchantPage: page }) => {
  await page.goto('/merchant/products/import');
  await expect(page.getByRole('heading', { name: 'Import produk dari CSV' })).toBeVisible();
  const downloaded = page.waitForEvent('download');
  await page.getByRole('link', { name: 'Download template CSV' }).click();
  const download = await downloaded;
  expect(await download.failure()).toBeNull();
  const csv = await readFile(await download.path(), 'utf8');
  expect(csv.split(/\r?\n/)[0].split(',')).toEqual(expect.arrayContaining(['slug', 'variant_name', 'sku', 'rental_fee', 'image_urls']));
  let uploads = 0;
  page.on('request', request => { if (request.method() === 'POST' && new URL(request.url()).pathname === '/api/products/imports') uploads++; });
  await page.getByLabel('File CSV', { exact: true }).setInputFiles({ name: 'invalid.txt', mimeType: 'text/plain', buffer: Buffer.from('invalid') });
  await expect(page.getByText('Pilih file CSV maksimal 5 MB.', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Import produk', exact: true })).toBeDisabled();
  expect(uploads).toBe(0);
});

test('WhatsApp disconnect button reflects real session state without modifying it', async ({ merchantPage: page }) => {
  const received = page.waitForResponse(response => new URL(response.url()).pathname === '/api/integrations/whatsapp' && response.request().method() === 'GET');
  await page.goto('/merchant/integrations/whatsapp');
  const response = await received;
  expect(response.status()).toBe(200);
  const state = await response.json();
  await expect(page.getByRole('heading', { name: 'WhatsApp', exact: true })).toBeVisible();
  const disconnect = page.getByRole('button', { name: 'Putuskan koneksi', exact: true });
  const active = state.enabled && ['STARTING', 'QR', 'AUTHENTICATING', 'CONNECTED', 'UNAVAILABLE'].includes(state.state);
  if (active) await expect(disconnect).toBeVisible();
  else await expect(disconnect).toHaveCount(0);
  const send = page.getByRole('button', { name: 'Kirim', exact: true });
  await expect(send).toBeDisabled(); // Empty draft never sends, even when the phone is connected.
  await expectNoPageOverflow(page);
});

test('logout clears identity cookies and protects merchant pages after reload', async ({ merchantPage: page }) => {
  await page.locator('.profileBtn').click();
  await page.getByRole('button', { name: /^(Keluar|Logout|Log out)$/i }).click();
  await expect(page).toHaveURL(/\/login\/?$/);
  const cookies = await page.context().cookies();
  for (const name of ['access_token', 'merchant', 'user']) expect(cookies.some(cookie => cookie.name === name && cookie.value)).toBe(false);
  await page.goto('/merchant/products');
  await expect(page).toHaveURL(/\/login\/?$/);
});
