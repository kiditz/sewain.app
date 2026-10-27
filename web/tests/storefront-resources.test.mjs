import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import { computed, ref } from 'vue';

async function loadModule(file, replacements = []) {
  let source = stripTypeScriptTypes(readFileSync(new URL(`../app/features/storefront-builder/${file}`, import.meta.url), 'utf8'));
  source = source.replace('import { useStorefrontState as useState } from \"./useStorefrontState\";', '');
  for (const [from, to] of replacements) source = source.replaceAll(from, to);
  return import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
}
let activeApp;
let request;
globalThis.computed = computed;
globalThis.useNuxtApp = () => activeApp;
globalThis.useRequestURL = () => new URL('http://store.sewain.app');
globalThis.useMerchantStore = () => ({ current: { slug: 'store' } });
globalThis.useApi = () => request;
globalThis.useState = (key, initialize) => {
  if (!activeApp.state.has(key)) activeApp.state.set(key, ref(initialize()));
  return activeApp.state.get(key);
};
function context(handler) {
  request = handler;
  activeApp = {
    state: new Map(),
    runWithContext(fn) {
      const previous = activeApp;
      activeApp = this;
      try { return Promise.resolve(fn()); } finally { activeApp = previous; }
    },
  };
  return activeApp;
}
const resourceModule = await loadModule('composables/useStorefrontResource.ts', [['import.meta.client', 'true']]);
Object.assign(globalThis, resourceModule);
const menuHelpers = await loadModule('utils/storefront-menu.ts');
globalThis.defaultMenus = menuHelpers.defaultMenus;
const { useStorefrontMenus } = await loadModule('composables/useStorefrontMenus.ts', [
  ['import { defaultMenus } from "../utils/storefront-menu";', ''],
  ['import { useStorefrontResource, useStorefrontResourceFactory } from "./useStorefrontResource";', ''],
]);
const flush = () => new Promise(resolve => setImmediate(resolve));

test('debounces each resource independently and snapshots payloads', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const calls = [];
  context(async (url, options) => { calls.push([url, options.body.configuration]); });
  const factory = resourceModule.useStorefrontResourceFactory('pages');
  const home = factory('home', '/storefront/pages/home');
  const about = factory('about', '/storefront/pages/about');
  const page = { id: 'home', title: 'First' };
  home.schedule(page);
  page.title = 'Latest';
  home.schedule(page);
  page.title = 'Unscheduled';
  about.schedule({ id: 'about' });
  assert.equal(home.hasUnsavedChanges.value, true);
  t.mock.timers.tick(600);
  await flush();
  assert.deepEqual(calls, [
    ['/storefront/pages/home', { id: 'home', title: 'Latest' }],
    ['/storefront/pages/about', { id: 'about' }],
  ]);
  assert.equal(home.hasUnsavedChanges.value, false);
});

test('a blocked theme save does not block page saves; failures can retry', async () => {
  let unblock;
  context((url) => url.endsWith('/theme') ? new Promise(resolve => { unblock = resolve; }) : Promise.resolve());
  const theme = resourceModule.useStorefrontResource('theme', 'theme', '/storefront/theme');
  const page = resourceModule.useStorefrontResource('pages', 'home', '/storefront/pages/home');
  const themeSave = theme.save({ name: 'Theme' });
  await page.save({ id: 'home' });
  assert.equal(page.hasUnsavedChanges.value, false);
  assert.equal(theme.saving.value, true);
  unblock();
  await themeSave;
  let fail = true;
  context(async () => { if (fail) throw new Error('offline'); });
  const retry = resourceModule.useStorefrontResource('theme', 'theme', '/storefront/theme');
  await assert.rejects(retry.save({ name: 'Theme' }));
  assert.equal(retry.saveError.value, true);
  assert.equal(retry.hasUnsavedChanges.value, true);
  fail = false;
  await retry.save({ name: 'Theme' });
  assert.equal(retry.saveError.value, false);
  assert.equal(retry.hasUnsavedChanges.value, false);
});

test('deleting cancels pending autosave; queues do not cross Nuxt apps', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const calls = [];
  context(async (_, options) => { calls.push(options.method); });
  const resource = resourceModule.useStorefrontResource('pages', 'about', '/storefront/pages/about');
  resource.schedule({ id: 'about' });
  await resource.remove();
  t.mock.timers.tick(600);
  await flush();
  assert.deepEqual(calls, ['DELETE']);
  context(async () => undefined);
  const anotherApp = resourceModule.useStorefrontResource('pages', 'about', '/storefront/pages/about');
  assert.notEqual(resource.status, anotherApp.status);
});

test('menu and navigation saves only write changed resources; empty menus remain empty', async () => {
  const menu = { id: 'main', name: 'Main Menu', items: [{ id: 'home', pageId: 'home' }] };
  const calls = [];
  let menus = [menu];
  let nav = { headerMenuId: 'main' };
  context(async (url, options) => {
    if (!options) return { configuration: structuredClone(url.endsWith('/menus') ? menus : nav) };
    calls.push([url, options.method]);
  });
  const store = useStorefrontMenus();
  await store.load();
  store.MenuItems.value[0].name = 'Renamed';
  await store.saveMenus();
  assert.deepEqual(calls.splice(0), [['/storefront/menus/main', 'PUT']]);
  store.headerMenuId.value = null;
  await store.saveMenus();
  assert.deepEqual(calls.splice(0), [['/storefront/navigation', 'PUT']]);
  store.MenuItems.value = [];
  await store.saveMenus();
  assert.deepEqual(calls.splice(0), [['/storefront/menus/main', 'DELETE']]);
  menus = [];
  nav = { headerMenuId: null };
  store.initialized.value = false;
  await store.load();
  assert.deepEqual(store.MenuItems.value, []);
  assert.equal(store.headerMenuId.value, null);
});

test('page references follow current titles/URLs and hide drafts, deleted pages and product templates', () => {
  const pages = [
    { id: 'home', title: 'New Home', targetUrl: '/', kind: 'HOME', status: 'PUBLISHED' },
    { id: 'about', title: 'About', targetUrl: '/pages/new-about', kind: 'CUSTOM', status: 'PUBLISHED' },
    { id: 'draft', title: 'Draft', targetUrl: '/pages/draft', kind: 'CUSTOM', status: 'DRAFT' },
    { id: 'product', title: 'Product', targetUrl: '/products/:slug', kind: 'PRODUCT', status: 'PUBLISHED' },
  ];
  const result = menuHelpers.resolveMenuItems([
    { id: 'one', pageId: 'home', label: 'Old label' },
    { id: 'two', pageId: 'about', url: '/old-url' },
    { id: 'three', pageId: 'draft' },
    { id: 'four', pageId: 'deleted' },
    { id: 'five', pageId: 'product' },
  ], pages);
  assert.equal(result.length, 2);
  assert.equal(result[0].label, 'New Home');
  assert.equal(result[1].url, '/pages/new-about');
});

test('initial menu defaults are persisted once, after pages exist', async () => {
  const calls = [];
  context(async (url, options) => {
    if (!options) return { configuration: url.endsWith('/menus') ? [] : {} };
    calls.push([url, options.method]);
  });
  const store = useStorefrontMenus();
  await store.load();
  await store.initializeDefaults();
  assert.deepEqual(calls, [['/storefront/menus/main-menu', 'PUT'], ['/storefront/navigation', 'PUT']]);
  await store.initializeDefaults();
  assert.equal(calls.length, 2);
});

test('editor state and initialization flags are isolated per merchant', async () => {
  context(async () => undefined);
  const original = globalThis.useMerchantStore;
  let slug = 'first-store';
  globalThis.useMerchantStore = () => ({ current: { slug } });
  try {
    const { useStorefrontState } = await loadModule('composables/useStorefrontState.ts');
    const first = useStorefrontState('initialized', () => false);
    first.value = true;
    slug = 'second-store';
    assert.equal(useStorefrontState('initialized', () => false).value, false);
    slug = 'first-store';
    assert.equal(useStorefrontState('initialized', () => false).value, true);
  } finally { globalThis.useMerchantStore = original; }
});

test('SSR save after await uses captured state without async runWithContext', async () => {
  const calls = [];
  const app = context(async (url, options) => { calls.push([url, options.method]); });
  assert.ok(app.runWithContext(() => 42) instanceof Promise);
  const factory = resourceModule.useStorefrontResourceFactory('pages');
  await Promise.resolve();
  // This is the async load/migration path: resource creation runs after setup.
  activeApp = undefined;
  const resource = factory('home', '/storefront/pages/home');
  await resource.save({ id: 'home' });
  assert.equal(resource.status.value.dirty, false);
  assert.equal(resource.status.value.saving, false);
  assert.deepEqual(calls, [['/storefront/pages/home', 'PUT']]);
});

test('legacy pages receive exactly one header and hidden headers stay hidden', async () => {
  const { ensureHeaderSection, headerDefaults, resolveHeaderMenu } = await loadModule('utils/header-section.ts');
  const page = { id: 'home', sections: [{ id: 'content', type: 'HERO', enabled: true, settings: {}, blocks: [] }] };
  assert.equal(ensureHeaderSection(page), true);
  assert.equal(page.sections[0].type, 'HEADER');
  assert.equal(page.sections[1].id, 'content');
  const id = page.sections[0].id;
  page.sections[0].enabled = false;
  assert.equal(ensureHeaderSection(page), false);
  assert.equal(page.sections[0].id, id);
  assert.equal(page.sections[0].enabled, false);
  assert.equal(page.sections.length, 2);
  const first = headerDefaults();
  first.padding.top = 99;
  assert.equal(headerDefaults().padding.top, 20);
  const menus = [{ id: 'main' }, { id: 'secondary' }];
  assert.equal(resolveHeaderMenu(menus, null, 'main').id, 'main');
  assert.equal(resolveHeaderMenu(menus, 'secondary', 'main').id, 'secondary');
  assert.equal(resolveHeaderMenu(menus, '__none__', 'main'), undefined);
  assert.equal(resolveHeaderMenu(menus, 'deleted', 'main'), undefined);
});
