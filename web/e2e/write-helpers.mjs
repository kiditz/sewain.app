import { readFileSync } from 'node:fs';
import { expect } from './fixtures.mjs';

const dictionaries = ['en', 'id'].map(locale => JSON.parse(readFileSync(new URL(`../i18n/locales/${locale}/${locale}.json`, import.meta.url), 'utf8')));
export function label(key) {
  const values = dictionaries.map(dictionary => key.split('.').reduce((value, part) => value?.[part], dictionary));
  if (values.some(value => typeof value !== 'string')) throw new Error(`Missing existing translation key: ${key}`);
  const special = new Set('.*+?^${}()|[]\\');
  const escaped = [...new Set(values)].map(value => [...value].map(char => special.has(char) ? '\\' + char : char).join(''));
  return new RegExp(`^(?:${escaped.join('|')})$`);
}
export async function ready(page) {
  await page.waitForFunction(() => document.querySelector('#__nuxt')?.__vue_app__?.config.globalProperties.$nuxt?.isHydrating === false);
}
// Existing forms mix associated Vuetify labels and separate v-label siblings.
export async function input(page, key) {
  const fieldLabel = page.locator('label:visible').filter({ hasText: label(key) }).first();
  await expect(fieldLabel).toBeVisible();
  const containerPath = 'xpath=ancestor-or-self::*[contains(concat(" ", normalize-space(@class), " "), " v-input ")][1]';
  // Built-in Vuetify labels live inside their input; external v-labels are siblings.
  let field = fieldLabel.locator(containerPath);
  if (await field.count() === 0) {
    const id = await fieldLabel.getAttribute('for');
    field = id
      ? page.locator(`[id=${JSON.stringify(id)}]`).locator(containerPath)
      : fieldLabel.locator('xpath=following-sibling::*[contains(concat(" ", normalize-space(@class), " "), " v-input ")][1]');
  }
  await expect(field).toBeVisible();
  // Vuetify selects expose a combobox plus a hidden input containing their value.
  // Click the accessible combobox rather than the hidden model-value input.
  const combobox = field.getByRole('combobox');
  const control = await combobox.count()
    ? combobox.first()
    : field.locator('input:not([type="hidden"]):visible, textarea:visible').first();
  await expect(control).toBeVisible();
  return control;
}
export async function expand(page, key) {
  const title = page.locator('.v-expansion-panel-title').filter({ has: page.getByText(label(key), { exact: true }) });
  await expect(title).toHaveCount(1);
  if (await title.getAttribute('aria-expanded') !== 'true') await title.click();
  return title.locator('..');
}
export async function pickVariant(page, productName) {
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByPlaceholder(label('product.variantDialog.searchPlaceholder')).fill(productName);
  const option = dialog.getByRole('checkbox', { name: new RegExp(productName) });
  await expect(option).toHaveCount(1);
  await option.click();
  await dialog.getByRole('button', { name: label('common.select') }).click();
  await expect(dialog).not.toBeVisible();
}
export async function submitAndCapture(page, path, action) {
  const pending = page.waitForResponse(response => new URL(response.url()).pathname === path && response.request().method() === 'POST');
  await action();
  const response = await pending;
  expect(response.ok(), `${path} returned HTTP ${response.status()}`).toBe(true);
  return { result: await response.json(), command: response.request().postDataJSON() };
}
export async function selectFirst(page, key) {
  await (await input(page, key)).click();
  const option = page.getByRole('option').first();
  await expect(option).toBeVisible();
  await option.click();
}
