<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import { menuPages, menuItemPage } from "~/features/storefront-builder/utils/storefront-menu";
import type { StorefrontMenu, StorefrontMenuItem } from "~/types/storefront";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const storefront = useStorefront();
await storefront.load();
const { saving, saveError, hasUnsavedChanges } = storefront.menuStore;
const menus = ref<StorefrontMenu[]>(JSON.parse(JSON.stringify(storefront.MenuItems.value)));
const availablePages = computed(() => menuPages(storefront.pages.value));
const pageOptions = computed(() => availablePages.value.map(page => ({
  title: `${page.title}${page.status === "DRAFT" ? " (Draft)" : ""}`,
  value: page.id,
})));
for (const menu of menus.value) {
  for (const item of menu.items) {
    item.pageId = menuItemPage(item, storefront.pages.value)?.id;
  }
}
const headerMenuId = ref(storefront.headerMenuId.value);
const selectedId = ref(menus.value[0]?.id);
const selected = computed(() => menus.value.find(menu => menu.id === selectedId.value));
const submitted = ref(false);
watch([menus, headerMenuId], () => { submitted.value = false; }, { deep: true });
const validationError = computed(() => {
  if (menus.value.some(menu => !menu.name.trim())) return "Setiap menu harus memiliki nama.";
  if (menus.value.some(menu => menu.items.some(item => !menuItemPage(item, storefront.pages.value)))) {
    return "Pilih halaman storefront untuk setiap item menu.";
  }
  return "";
});
function addMenu() {
  const menu = { id: crypto.randomUUID(), name: "New Menu", items: [] };
  menus.value.push(menu);
  selectedId.value = menu.id;
}
function removeMenu() {
  if (!selected.value) return;
  menus.value = menus.value.filter(menu => menu.id !== selectedId.value);
  if (headerMenuId.value === selectedId.value) headerMenuId.value = null;
  selectedId.value = menus.value[0]?.id;
}
function addItem() {
  selected.value?.items.push({ id: crypto.randomUUID() });
}
function selectPage(item: StorefrontMenuItem, pageId: string) {
  item.pageId = pageId;
}
function moveItem(index: number, offset: number) {
  const items = selected.value?.items;
  if (!items || index + offset < 0 || index + offset >= items.length) return;
  const [item] = items.splice(index, 1);
  if (item) items.splice(index + offset, 0, item);
}
async function save() {
  submitted.value = true;
  if (validationError.value) return;
  storefront.MenuItems.value = menus.value.map(menu => ({
    ...menu, name: menu.name.trim(),
    items: menu.items.map(item => {
      const page = menuItemPage(item, storefront.pages.value)!;
      return { id: item.id, pageId: page.id };
    }),
  }));
  storefront.headerMenuId.value = headerMenuId.value;
  try { await storefront.menuStore.saveMenus(); } catch { /* Error is displayed below. */ }
}
</script>

<template>
  <v-container class="py-8" style="max-width: 1080px">
    <header class="d-flex align-center flex-wrap ga-3 mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" to="/merchant/storefront" aria-label="Kembali ke storefront" />
      <div class="flex-grow-1">
        <div class="text-overline">Storefront</div>
        <h1 class="text-headline-medium">Menu</h1>
        <p class="text-body-medium text-medium-emphasis">Atur menu dan link navigasi toko.</p>
      </div>
      <v-btn color="primary" :loading="saving" @click="save">Simpan menu</v-btn>
    </header>
    <v-alert v-if="submitted && validationError" type="error" class="mb-4">{{ validationError }}</v-alert>
    <v-alert v-if="saveError" type="error" class="mb-4">Menu gagal disimpan. Klik Simpan menu untuk mencoba lagi.</v-alert>
    <v-alert v-else-if="submitted && !validationError && !saving && !hasUnsavedChanges" type="success" class="mb-4">Menu tersimpan.</v-alert>
    <v-row>
      <v-col cols="12" md="4">
        <v-card border flat rounded="lg" class="pa-4 mb-4">
          <h2 class="text-title-medium mb-3">Menu</h2>
          <v-list v-if="menus.length" aria-label="Daftar menu">
            <v-list-item v-for="menu in menus" :key="menu.id" :title="menu.name || 'Untitled Menu'" :active="selectedId === menu.id" @click="selectedId = menu.id" />
          </v-list>
          <p v-else class="text-body-small mb-3">Belum ada menu. Tambahkan menu pertama.</p>
          <v-btn variant="tonal" prepend-icon="mdi-plus" block @click="addMenu">Tambah menu</v-btn>
        </v-card>
        <v-card border flat rounded="lg" class="pa-4">
          <h2 class="text-title-medium mb-3">Header</h2>
          <v-select v-model="headerMenuId" :items="menus" item-title="name" item-value="id" label="Menu di header" clearable hide-details />
          <p class="text-body-small text-medium-emphasis mt-3">Pilih menu yang ditampilkan di header desktop dan mobile.</p>
        </v-card>
      </v-col>
      <v-col cols="12" md="8">
        <v-card v-if="selected" border flat rounded="lg" class="pa-5">
          <v-text-field v-model="selected.name" label="Nama menu" placeholder="Main Menu" />
          <div v-for="(item, index) in selected.items" :key="item.id" class="menu-item mb-4 pa-3">
            <div class="d-flex align-center justify-space-between mb-2">
              <span class="text-title-small">Link {{ index + 1 }}</span>
              <div>
                <v-btn icon="mdi-arrow-up" size="small" variant="text" :disabled="index === 0" aria-label="Pindahkan link ke atas" @click="moveItem(index, -1)" />
                <v-btn icon="mdi-arrow-down" size="small" variant="text" :disabled="index === selected.items.length - 1" aria-label="Pindahkan link ke bawah" @click="moveItem(index, 1)" />
                <v-btn icon="mdi-delete-outline" size="small" variant="text" color="error" aria-label="Hapus link" @click="selected.items.splice(index, 1)" />
              </div>
            </div>
            <v-select :model-value="item.pageId" :items="pageOptions" label="Halaman storefront" :error="submitted && !menuItemPage(item, storefront.pages.value)" @update:model-value="selectPage(item, $event)" hide-details class="mb-4" />
            <v-text-field :model-value="menuItemPage(item, storefront.pages.value)?.targetUrl ?? ''" label="URL" disabled hide-details />
            <p v-if="menuItemPage(item, storefront.pages.value)?.status === 'DRAFT'" class="text-body-small text-medium-emphasis mt-2">Link akan tampil setelah halaman dipublikasikan.</p>
          </div>
          <p v-if="!selected.items.length" class="text-body-medium mb-4">Menu ini belum memiliki link.</p>
          <div class="d-flex flex-wrap justify-space-between ga-3">
            <v-btn variant="tonal" prepend-icon="mdi-plus" @click="addItem">Tambah link</v-btn>
            <v-btn color="error" variant="text" @click="removeMenu">Hapus menu</v-btn>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.menu-item { border: 1px solid rgba(0, 0, 0, 0.12); border-radius: 8px; }
</style>
