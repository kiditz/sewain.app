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

test('menu labels can be customized while page URLs and visibility stay current', () => {
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
  assert.equal(result[0].label, 'Old label');
  assert.equal(result[1].label, 'About');
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

test('legacy pages receive exactly one navigation and hidden navigation stays hidden', async () => {
  const { ensureNavigationSection, navigationDefaults, resolveNavigationMenu } = await loadModule('utils/navigation-section.ts');
  const page = { id: 'home', sections: [{ id: 'content', type: 'HERO', enabled: true, settings: {}, blocks: [] }] };
  assert.equal(ensureNavigationSection(page), true);
  assert.equal(page.sections[0].type, 'NAVIGATION');
  assert.equal(page.sections[1].id, 'content');
  const id = page.sections[0].id;
  page.sections[0].enabled = false;
  assert.equal(ensureNavigationSection(page), false);
  assert.equal(page.sections[0].id, id);
  assert.equal(page.sections[0].enabled, false);
  assert.equal(page.sections.length, 2);
  const first = navigationDefaults();
  first.padding.top = 99;
  assert.equal(navigationDefaults().padding.top, 20);
  const menus = [{ id: 'main' }, { id: 'secondary' }];
  assert.equal(resolveNavigationMenu(menus, null, 'main').id, 'main');
  assert.equal(resolveNavigationMenu(menus, 'secondary', 'main').id, 'secondary');
  assert.equal(resolveNavigationMenu(menus, '__none__', 'main'), undefined);
  assert.equal(resolveNavigationMenu(menus, 'deleted', 'main'), undefined);
});

test('legacy header becomes navigation with independent blocks and retains hidden state and selected menu', async () => {
  const { ensureNavigationSection } = await loadModule('utils/navigation-section.ts');
  const padding = { top: 8, right: 16, bottom: 8, left: 16 };
  const page = { id: 'home', sections: [{ id: 'existing-header', type: 'HEADER', enabled: false, settings: { menuId: 'secondary', padding }, blocks: [] }] };
  assert.equal(ensureNavigationSection(page), true);
  const navigation = page.sections[0];
  assert.equal(navigation.type, 'NAVIGATION');
  assert.equal(navigation.id, 'existing-header');
  assert.equal(navigation.enabled, false);
  assert.deepEqual(navigation.settings.padding, padding);
  assert.equal('menuId' in navigation.settings, false);
  assert.deepEqual(navigation.blocks.map(block => block.type), ['LOGO', 'HEADING', 'MENU']);
  assert.equal(navigation.blocks[2].settings.menuId, 'secondary');
  navigation.blocks[0].enabled = false;
  navigation.blocks[1].enabled = false;
  navigation.blocks[2].enabled = false;
  const snapshot = JSON.stringify(page);
  assert.equal(ensureNavigationSection(page), false);
  assert.equal(JSON.stringify(page), snapshot, 'reloading does not recreate or reveal hidden blocks');
  navigation.blocks = [];
  assert.equal(ensureNavigationSection(page), false);
  assert.deepEqual(navigation.blocks, [], 'removing all navigation blocks remains intentional');
});

const { useStorefrontCatalog } = await loadModule('composables/useStorefrontCatalog.ts');

test('catalog loads only the requested page even for a huge total', async () => {
  const calls = [];
  context(async (path, options) => { calls.push([path, options.query]); return { items: [{ id: 'page-item' }], totalItems: 50000 }; });
  const catalog = useStorefrontCatalog();
  assert.equal(calls.length, 0, 'constructing the catalog must not fetch anything');
  const result = await catalog.loadProducts({ page: 2, size: 12 });
  assert.equal(calls.length, 1);
  assert.deepEqual(calls[0], ['/products', { status: 'ACTIVE', page: 2, size: 12 }]);
  assert.equal(result.totalItems, 50000);
  assert.equal(catalog.totalProducts.value, 50000);
  assert.equal(catalog.products.value.length, 1);
});

test('catalog deduplicates matching in-flight requests but keeps different pages separate', async () => {
  const calls = [];
  context(async (path, options) => { calls.push([path, options.query]); return { items: [], totalItems: 1000 }; });
  const first = useStorefrontCatalog();
  const second = useStorefrontCatalog();
  await Promise.all([first.fetchProducts({ page: 0, size: 3 }), second.fetchProducts({ size: 3, page: 0 }), first.fetchProducts({ page: 1, size: 3 })]);
  assert.equal(calls.length, 2);
  assert.deepEqual(calls.map(([, query]) => query.page), [0, 1]);
  assert.deepEqual(first.products.value, [], 'public section requests do not populate the builder product cache');
});

test('collections and product types load independently and metadata failures can retry', async () => {
  const calls = [];
  let fail = true;
  context(async path => {
    calls.push(path);
    if (path === '/collections' && fail) throw new Error('temporary');
    return path === '/collections' ? [{ id: 'collection' }] : [{ id: 'type', name: 'Type' }];
  });
  const catalog = useStorefrontCatalog();
  await assert.rejects(catalog.loadCollections());
  fail = false;
  await Promise.all([catalog.loadCollections(), useStorefrontCatalog().loadCollections()]);
  await catalog.loadCollections();
  assert.deepEqual(calls, ['/collections', '/collections']);
  await Promise.all([catalog.loadProductTypes(), useStorefrontCatalog().loadProductTypes()]);
  await catalog.loadProductTypes();
  assert.deepEqual(calls, ['/collections', '/collections', '/products/types']);
});

test('targeted collection queries retain pagination, IDs and active/availability filters', async () => {
  const calls = [];
  context(async (path, options) => { calls.push([path, options.query]); return { items: [], totalItems: 0 }; });
  const catalog = useStorefrontCatalog();
  await catalog.fetchCollectionProducts('selected-collection', { page: 3, size: 8, id: 'selected-product', available: true });
  assert.deepEqual(calls, [['/collections/selected-collection/products', { status: 'ACTIVE', page: 3, size: 8, id: 'selected-product', available: true }]]);
});


test('blank menu labels use the current page title and custom labels are trimmed', () => {
  const pages = [{ id: 'home', title: 'Home', targetUrl: '/', kind: 'HOME', status: 'PUBLISHED' }];
  const items = [{ id: 'default', pageId: 'home' }, { id: 'blank', pageId: 'home', label: '  ' }, { id: 'custom', pageId: 'home', label: '  Beranda  ' }];
  assert.deepEqual(menuHelpers.resolveMenuItems(items, pages).map(item => item.label), ['Home', 'Home', 'Beranda']);
  pages[0].title = 'Updated home';
  assert.deepEqual(menuHelpers.resolveMenuItems(items, pages).map(item => item.label), ['Updated home', 'Updated home', 'Beranda']);
});

test('reordered menu items and custom labels survive saving and reloading', async () => {
  let stored = { id: 'main', name: 'Main menu', items: [
    { id: 'home-link', pageId: 'home', label: 'Beranda' },
    { id: 'catalog-link', pageId: 'catalog' },
    { id: 'about-link', pageId: 'about', label: 'Tentang kami' },
  ] };
  const writes = [];
  context(async (url, options) => {
    if (!options) return { configuration: structuredClone(url.endsWith('/menus') ? [stored] : { headerMenuId: 'main' }) };
    writes.push(url);
    stored = structuredClone(options.body.configuration);
  });
  const store = useStorefrontMenus();
  await store.load();
  const items = store.MenuItems.value[0].items;
  items.unshift(items.splice(2, 1)[0]);
  await store.saveMenus();
  assert.deepEqual(writes, ['/storefront/menus/main']);
  store.initialized.value = false;
  await store.load();
  assert.deepEqual(store.MenuItems.value[0].items.map(item => item.id), ['about-link', 'home-link', 'catalog-link']);
  const pages = ['home', 'catalog', 'about'].map(id => ({ id, title: id, targetUrl: `/${id}`, status: 'PUBLISHED', kind: 'CUSTOM' }));
  assert.deepEqual(menuHelpers.resolveMenuItems(store.MenuItems.value[0].items, pages).map(item => item.label), ['Tentang kami', 'Beranda', 'catalog']);
});

test('catalog menu destinations resolve without exposing a URL editor or requiring a page record', () => {
  const items = [
    { id: 'product', target: { type: 'PRODUCT', id: 'p1', title: 'Camera', slug: 'camera' } },
    { id: 'collection', label: 'Pilihan', target: { type: 'COLLECTION', id: 'c1', title: 'Cameras', slug: 'cameras' } },
    { id: 'invalid', target: { type: 'PRODUCT', id: 'p2', title: 'Missing slug', slug: '' } },
  ];
  assert.deepEqual(menuHelpers.resolveMenuItems(items, []).map(({ label, url }) => ({ label, url })), [
    { label: 'Camera', url: '/products/camera' }, { label: 'Pilihan', url: '/collections/cameras' },
  ]);
  assert.equal(menuHelpers.hasMenuDestination(items[0], []), true);
  assert.equal(menuHelpers.hasMenuDestination(items[2], []), false);
  assert.equal(menuHelpers.menuItemTitle(items[1], []), 'Cameras');
});
