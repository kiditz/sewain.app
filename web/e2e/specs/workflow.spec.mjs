import { randomUUID } from 'node:crypto';
import { test, expect } from '../fixtures.mjs';
import { label, ready, input, expand, pickVariant, submitAndCapture, selectFirst } from '../write-helpers.mjs';

// Real writes, explicit opt-in, sequential dependencies. No mocked APIs or API-created fixtures.
test.describe.configure({ mode: 'serial' });
test.beforeEach(() => {
  test.skip(process.env.E2E_ALLOW_WRITES !== 'true', 'Set E2E_ALLOW_WRITES=true only for a dedicated writable test tenant.');
});
const suffix = randomUUID().slice(0, 8);
const productName = `E2E Tent ${suffix}`;
const collectionName = `E2E Collection ${suffix}`;
const sku = `E2E-${suffix}`;
let product;
let collection;

async function record(testInfo, kind, result) {
  await testInfo.attach(`created-${kind}`, { body: Buffer.from(JSON.stringify({ id: result.id, name: result.name, orderNumber: result.orderNumber, suffix }, null, 2)), contentType: 'application/json' });
}

test('create an active product with daily rental price and physical inventory, then reload it', async ({ merchantPage: page }, testInfo) => {
  await page.goto('/merchant/products/create');
  await ready(page);
  await page.getByPlaceholder(label('product.namePlaceholder')).fill(productName);
  await (await input(page, 'product.slug')).fill(`e2e-tent-${suffix}`);
  await (await input(page, 'product.variant.sku.label')).fill(sku);
  await (await input(page, 'product.depositAmount')).fill('0');
  await (await input(page, 'product.status')).click();
  await page.getByRole('option', { name: label('product.statusOptions.ACTIVE'), exact: true }).click();
  const plans = await expand(page, 'product.form.rentalPlanTitle');
  // The empty state also has an Add button; target the rental-plan toolbar.
  const addPlan = plans.locator('.d-flex.justify-end.flex-wrap.ga-2.mb-4')
    .getByRole('button', { name: label('common.add'), exact: true });
  await expect(addPlan).toHaveCount(1);
  await addPlan.click();
  await expect(plans.locator('.rental-plan-card')).toHaveCount(1);
  await (await input(page, 'rentalplan.durationValue')).fill('1');
  await (await input(page, 'rentalplan.rentalFee')).fill('100000');
  const stock = await expand(page, 'product.form.inventoryUnits');
  await (await input(page, 'product.form.inventoryQuantity')).fill('1');
  await stock.getByRole('button', { name: label('product.form.generateUnits') }).click();
  const created = await submitAndCapture(page, '/api/products', () => page.getByRole('button', { name: label('common.saveChange'), exact: true }).click());
  product = created.result;
  await record(testInfo, 'product', product);
  expect(product.id).toBeTruthy();
  expect(product.name).toBe(productName);
  expect(product.status).toBe('ACTIVE');
  expect(product.variants).toHaveLength(1);
  expect(product.variants[0].sku).toBe(sku);
  expect(product.variants[0].inventoryUnits).toHaveLength(1);
  expect(product.variants[0].rentalPlans).toEqual(expect.arrayContaining([expect.objectContaining({ durationValue: 1, durationUnit: 'DAY', active: true })]));
  expect(Number(product.variants[0].rentalPlans[0].rentalFee)).toBe(100000);
  // Reopen through a fresh route: confirm persisted data, not just local form state.
  await page.goto(`/merchant/products/${product.id}`);
  await ready(page);
  await expect(page.getByPlaceholder(label('product.namePlaceholder'))).toHaveValue(productName);
  await expect(await input(page, 'product.variant.sku.label')).toHaveValue(sku);
});

test('create a manual collection containing the new product and reopen it', async ({ merchantPage: page }, testInfo) => {
  expect(product?.id, 'Product creation must succeed first').toBeTruthy();
  await page.goto('/merchant/collections/create');
  await ready(page);
  await page.getByPlaceholder(label('collections.placeholders.title')).fill(collectionName);
  await (await input(page, 'collections.fields.slug')).fill(`e2e-collection-${suffix}`);
  await page.getByRole('button', { name: label('collections.actions.addProduct'), exact: true }).first().click();
  await pickVariant(page, productName);
  const created = await submitAndCapture(page, '/api/collections', () => page.getByRole('button', { name: label('collections.actions.create'), exact: true }).click());
  collection = created.result;
  await record(testInfo, 'collection', collection);
  expect(collection.id).toBeTruthy();
  expect(collection.name).toBe(collectionName);
  expect(created.command.type).toBe('MANUAL');
  expect(created.command.productIds).toContain(product.id);
  await page.goto(`/merchant/collections/create?id=${collection.id}`);
  await ready(page);
  await expect(page.getByPlaceholder(label('collections.placeholders.title'))).toHaveValue(collectionName);
  await expect(page.getByText(productName, { exact: true })).toBeVisible();
});

test('create a pickup order using the new product and verify persisted order detail', async ({ merchantPage: page }, testInfo) => {
  expect(product?.id, 'Product creation must succeed first').toBeTruthy();
  const lead = Number(process.env.E2E_RENTAL_LEAD_DAYS || '30');
  expect(Number.isInteger(lead) && lead >= 1 && lead <= 365).toBe(true);
  const start = new Date(); start.setUTCDate(start.getUTCDate() + lead);
  const end = new Date(start); end.setUTCDate(end.getUTCDate() + 3);
  const startDate = start.toISOString().slice(0, 10);
  const endDate = end.toISOString().slice(0, 10);
  const customer = `E2E Customer ${suffix}`;
  await page.goto('/merchant/orders/create');
  await ready(page);
  await (await input(page, 'orders.create.customerName')).fill(customer);
  await (await input(page, 'orders.create.customerEmail')).fill(`e2e-${suffix}@example.invalid`);
  await (await input(page, 'orders.create.startAt')).fill(startDate);
  await (await input(page, 'orders.create.endAt')).fill(endDate);
  await page.getByRole('button', { name: label('orders.create.addProduct'), exact: true }).click();
  await pickVariant(page, productName);
  await (await input(page, 'orders.create.pickupAt')).fill(`${startDate}T09:00`);
  await selectFirst(page, 'orders.create.initialPayment');
  const balance = page.locator('label').filter({ hasText: label('orders.create.balanceDueAt') });
  if (await balance.count()) await selectFirst(page, 'orders.create.balanceDueAt');
  await selectFirst(page, 'orders.create.paymentMethod');
  const created = await submitAndCapture(page, '/api/merchant/orders', () => page.getByRole('button', { name: label('orders.create.submit'), exact: true }).click());
  const order = created.result;
  await record(testInfo, 'order', order);
  expect(order.id).toBeTruthy();
  expect(order.orderNumber).toBeTruthy();
  expect(order.customerName).toBe(customer);
  expect(created.command.fulfillmentType).toBe('PICKUP');
  expect(created.command.items).toHaveLength(1);
  expect(created.command.items[0]).toMatchObject({ productId: product.id, variantId: product.variants[0].id, quantity: 1 });
  expect(Number(order.subtotal)).toBe(300000);
  expect(Number(order.totalAmount)).toBeGreaterThan(0);
  await expect(page).toHaveURL(new RegExp(`/merchant/orders/${order.id}$`));
  // Client navigation makes the real detail GET observable (SSR would run it on the server).
  await page.goto('/merchant/orders');
  const loaded = page.waitForResponse(response => new URL(response.url()).pathname === `/api/merchant/orders/${order.id}` && response.request().method() === 'GET');
  await page.getByText(order.orderNumber, { exact: true }).click();
  const detail = await loaded;
  expect(detail.ok()).toBe(true);
  const persisted = await detail.json();
  // POST returns an order summary; the detail GET loads persisted line items.
  expect(persisted.items).toHaveLength(1);
  expect(persisted.items[0]).toMatchObject({ productId: product.id, variantId: product.variants[0].id, quantity: 1 });
  expect(persisted.customerName).toBe(customer);
  expect(Number(persisted.totalAmount)).toBe(Number(order.totalAmount));
  await expect(page.getByText(order.orderNumber, { exact: true }).first()).toBeVisible();
});
