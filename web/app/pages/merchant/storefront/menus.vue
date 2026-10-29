<script setup lang="ts">
import { VueDraggable } from "vue-draggable-plus";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import MenuLinkPicker from "~/features/storefront-builder/components/MenuLinkPicker.vue";
import {
  menuItemTitle,
  hasMenuDestination,
  menuItemPage,
} from "~/features/storefront-builder/utils/storefront-menu";
import type { StorefrontMenu, StorefrontMenuItem } from "~/types/storefront";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });
useHead({ title: $t("ui.menu") });
const storefront = useStorefront();
await storefront.load();
const { saving } = storefront.menuStore;
const snackbar = useSnackbarStore();
const menus = ref<StorefrontMenu[]>(
  JSON.parse(JSON.stringify(storefront.MenuItems.value)),
);
for (const menu of menus.value) {
  for (const item of menu.items) {
    if (!item.target)
      item.pageId = menuItemPage(item, storefront.pages.value)?.id;
  }
}
const headerMenuId = ref(storefront.headerMenuId.value);
const selectedId = ref(menus.value[0]?.id);
const selected = computed(() =>
  menus.value.find((menu) => menu.id === selectedId.value),
);
const editorId = `menu-editor-${useId()}`;
const editingItemId = ref<string | null>(selected.value?.items[0]?.id ?? null);
const attemptedItemId = ref<string | null>(null);
const orderAnnouncement = ref("");
watch(selectedId, () => {
  editingItemId.value = selected.value?.items[0]?.id ?? null;
  attemptedItemId.value = null;
});
function itemLabel(item: StorefrontMenuItem) {
  return (
    item.label?.trim() ||
    menuItemTitle(item, storefront.pages.value) ||
    "Item menu baru"
  );
}
function finishEditing(item: StorefrontMenuItem) {
  attemptedItemId.value = item.id;
  if (!hasMenuDestination(item, storefront.pages.value)) return;
  editingItemId.value = null;
  attemptedItemId.value = null;
}
function removeItem(id: string) {
  if (!selected.value) return;
  selected.value.items = selected.value.items.filter((item) => item.id !== id);
  if (editingItemId.value === id) editingItemId.value = null;
}
function announceOrder() {
  orderAnnouncement.value = `Urutan menu diperbarui: ${selected.value?.items.map(itemLabel).join(", ")}.`;
}
const submitted = ref(false);
watch(
  [menus, headerMenuId],
  () => {
    submitted.value = false;
  },
  { deep: true },
);
const validationError = computed(() => {
  if (menus.value.some((menu) => !menu.name.trim()))
    return "Setiap menu harus memiliki nama.";
  if (
    menus.value.some((menu) =>
      menu.items.some(
        (item) => !hasMenuDestination(item, storefront.pages.value),
      ),
    )
  ) {
    return "Pilih link tujuan untuk setiap item menu.";
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
  menus.value = menus.value.filter((menu) => menu.id !== selectedId.value);
  if (headerMenuId.value === selectedId.value) headerMenuId.value = null;
  selectedId.value = menus.value[0]?.id;
}
function addItem() {
  if (!selected.value) return;
  const item = { id: crypto.randomUUID() };
  selected.value.items.push(item);
  editingItemId.value = item.id;
}
function selectDestination(
  item: StorefrontMenuItem,
  destination: { pageId?: string; target?: StorefrontMenuItem["target"] },
) {
  delete item.pageId;
  delete item.target;
  delete item.url;
  Object.assign(item, destination);
}
function moveItem(index: number, offset: number) {
  const items = selected.value?.items;
  if (!items || index + offset < 0 || index + offset >= items.length) return;
  const [item] = items.splice(index, 1);
  if (item) items.splice(index + offset, 0, item);
  announceOrder();
}
async function save() {
  if (saving.value) return;
  submitted.value = true;
  if (validationError.value) {
    snackbar.openSnackbar({ message: validationError.value, type: "error" });
    const invalidMenu = menus.value.find((menu) =>
      menu.items.some(
        (item) => !hasMenuDestination(item, storefront.pages.value),
      ),
    );
    if (invalidMenu) {
      selectedId.value = invalidMenu.id;
      await nextTick();
      editingItemId.value =
        invalidMenu.items.find(
          (item) => !hasMenuDestination(item, storefront.pages.value),
        )?.id ?? null;
    }
    return;
  }
  storefront.MenuItems.value = menus.value.map((menu) => ({
    ...menu,
    name: menu.name.trim(),
    items: menu.items.map((item) => {
      const destination = item.target
        ? { target: { ...item.target } }
        : { pageId: menuItemPage(item, storefront.pages.value)!.id };
      const label = item.label?.trim();
      return { id: item.id, ...destination, ...(label ? { label } : {}) };
    }),
  }));
  storefront.headerMenuId.value = headerMenuId.value;
  try {
    await storefront.menuStore.saveMenus();
    snackbar.openSnackbar({ message: "Menu tersimpan.", type: "success" });
  } catch {
    snackbar.openSnackbar({
      message: "Menu gagal disimpan. Klik Simpan menu untuk mencoba lagi.",
      type: "error",
    });
  }
}
</script>

<template>
  <v-container class="menu-editor">
    <header class="menu-editor__toolbar">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        to="/merchant/storefront"
        aria-label="Kembali ke storefront"
      />
      <h1>{{ selected?.name || "Menu" }}</h1>
      <v-btn color="primary" :loading="saving" @click="save">Simpan menu</v-btn>
    </header>

    <v-card
      class="mt-5"
      variant="outlined"
      density="compact"
      rounded="lg"
      aria-label="Menu navigation default"
    >
      <v-card-text class="d-flex justify-space-between align-center">
        <v-select
          v-if="menus.length"
          v-model="selectedId"
          :items="menus"
          item-title="name"
          item-value="id"
          label="Pilih menu"
          variant="outlined"
          density="compact"
          hide-details
          :disabled="saving"
        />
        <v-btn
          variant="text"
          prepend-icon="mdi-plus"
          :disabled="saving"
          @click="addMenu"
          >Tambah menu</v-btn
        >
      </v-card-text>
    </v-card>

    <template v-if="selected">
      <v-card variant="outlined" rounded="lg" class="mt-5">
        <v-card-text>
          <label :for="`${editorId}-name`" class="menu-editor__label"
            >Nama</label
          >
          <v-text-field
            :id="`${editorId}-name`"
            v-model="selected.name"
            placeholder="Main menu"
            aria-label="Nama menu"
            variant="outlined"
            density="compact"
            hide-details
            rounded="lg"
            :disabled="saving"
          />
          <p class="menu-editor__handle">Handle: {{ selected.id }}</p>
        </v-card-text>
      </v-card>

      <v-card variant="outlined" rounded="lg" class="mt-5">
        <v-card-text>
          <div class="menu-editor__section-heading">
            <h2 id="menu-items-title">Item menu</h2>
            <span>Tarik pegangan untuk mengubah urutan.</span>
          </div>
          <VueDraggable
            v-model="selected.items"
            item-key="id"
            handle=".menu-item__handle"
            ghost-class="menu-item--ghost"
            chosen-class="menu-item--chosen"
            :animation="180"
            :disabled="saving"
            :delay="120"
            :delay-on-touch-only="true"
            :force-fallback="true"
            class="menu-editor__list"
            @end="announceOrder"
          >
            <div
              v-for="(item, index) in selected.items"
              :key="item.id"
              class="menu-item"
              :class="{ 'menu-item--expanded': editingItemId === item.id }"
              :data-menu-item-id="item.id"
            >
              <button
                type="button"
                class="menu-item__handle"
                :disabled="saving"
                :aria-label="`Urutkan ${itemLabel(item)}. Gunakan panah atas atau bawah untuk memindahkan.`"
                title="Tarik untuk mengurutkan; atau gunakan tombol panah atas/bawah"
                @keydown.up.prevent="moveItem(index, -1)"
                @keydown.down.prevent="moveItem(index, 1)"
              >
                <v-icon icon="mdi-drag" size="18" aria-hidden="true" />
              </button>

              <template v-if="editingItemId === item.id">
                <div class="menu-item__fields">
                  <div class="menu-item__field">
                    <label
                      :for="`${editorId}-${item.id}-label`"
                      class="menu-editor__label"
                      >Label</label
                    >
                    <v-text-field
                      :id="`${editorId}-${item.id}-label`"
                      v-model="item.label"
                      :placeholder="
                        menuItemTitle(item, storefront.pages.value) ||
                        'Nama item menu'
                      "
                      :aria-label="`Nama item menu ${index + 1}`"
                      variant="outlined"
                      density="compact"
                      hide-details
                      rounded="lg"
                      :disabled="saving"
                    />
                  </div>
                  <div class="menu-item__field">
                    <label
                      :for="`${editorId}-${item.id}-page`"
                      class="menu-editor__label"
                      >Link</label
                    >
                    <MenuLinkPicker
                      :id="`${editorId}-${item.id}-page`"
                      :item="item"
                      :pages="storefront.pages.value"
                      :error="
                        (submitted || attemptedItemId === item.id) &&
                        !hasMenuDestination(item, storefront.pages.value)
                      "
                      :disabled="saving"
                      @select="selectDestination(item, $event)"
                    />
                  </div>
                  <p
                    v-if="
                      (submitted || attemptedItemId === item.id) &&
                      !hasMenuDestination(item, storefront.pages.value)
                    "
                    class="menu-item__note menu-item__note--error"
                  >
                    Pilih link tujuan terlebih dahulu.
                  </p>
                  <p
                    v-else-if="
                      menuItemPage(item, storefront.pages.value)?.status ===
                      'DRAFT'
                    "
                    class="menu-item__note"
                  >
                    Link akan tampil setelah halaman dipublikasikan.
                  </p>
                </div>
                <div class="menu-item__actions">
                  <v-btn
                    icon="mdi-check"
                    size="small"
                    variant="text"
                    aria-label="Selesai mengedit item menu"
                    :disabled="saving"
                    @click="finishEditing(item)"
                  />
                  <v-btn
                    icon="mdi-trash-can-outline"
                    size="small"
                    variant="text"
                    aria-label="Hapus item menu"
                    :disabled="saving"
                    @click="removeItem(item.id)"
                  />
                </div>
              </template>
              <button
                v-else
                type="button"
                class="menu-item__summary"
                :disabled="saving"
                :aria-label="`Edit ${itemLabel(item)}`"
                :aria-expanded="false"
                @click="editingItemId = item.id"
              >
                {{ itemLabel(item) }}
              </button>
            </div>
          </VueDraggable>
          <button
            type="button"
            class="menu-editor__add-item"
            :disabled="saving"
            @click="addItem"
          >
            <v-icon
              icon="mdi-plus-circle-outline"
              size="18"
              aria-hidden="true"
            />Tambah item menu
          </button>
          <p class="menu-editor__sr-only" role="status" aria-live="polite">
            {{ orderAnnouncement }}
          </p>
        </v-card-text>
      </v-card>

      <div class="menu-editor__footer">
        <v-btn
          color="error"
          variant="text"
          :disabled="saving"
          @click="removeMenu"
          >Hapus menu</v-btn
        >
      </div>
    </template>
    <div v-else class="menu-editor__card menu-editor__empty">
      <v-icon icon="mdi-menu" size="32" />
      <p>
        Belum ada menu. Tambahkan menu pertama untuk mulai mengatur navigasi
        toko.
      </p>
      <v-btn color="primary" @click="addMenu">Tambah menu</v-btn>
    </div>
  </v-container>
</template>

<style scoped>
.menu-editor {
  max-width: 960px;
  padding: 32px 24px 48px;
  color: #202124;
}
.menu-editor__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.menu-editor__toolbar h1 {
  flex: 1;
  min-width: 0;
  font-size: 28px;
  font-weight: 500;
  line-height: 1.3;
  overflow-wrap: anywhere;
}
.menu-editor__switcher {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.menu-editor__switcher :deep(.v-input) {
  max-width: 280px;
}
.menu-editor__card {
  background: #fff;
  border: 1px solid #e3e3e3;
  border-radius: 18px;
  box-shadow: 0 2px 12px #00000005;
  padding: 16px;
}
.menu-editor__label {
  display: block;
  font-size: 13px;
  line-height: 1.4;
  margin-bottom: 6px;
}
.menu-editor__handle {
  margin: 10px 0 0;
  color: #616161;
  font-size: 13px;
  overflow-wrap: anywhere;
}
.menu-editor__items-section {
  margin-top: 36px;
}
.menu-editor__section-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}
.menu-editor h2 {
  font-size: 16px;
  font-weight: 500;
}
.menu-editor__section-heading span {
  color: #757575;
  font-size: 12px;
}
.menu-editor__list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.menu-item__handle,
.menu-item__summary,
.menu-editor__add-item {
  appearance: none;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}
.menu-item__handle,
.menu-item__summary {
  border: 0;
}
.menu-editor__identity :deep(.v-field),
.menu-item__field :deep(.v-field) {
  border-radius: 12px;
}
.menu-item {
  display: flex;
  align-items: center;
  min-height: 46px;
  border: 1px solid #d8d8d8;
  border-radius: 8px;
  background: #fff;
}
.menu-item--expanded {
  padding: 14px 10px 12px 4px;
  align-items: flex-start;
}
.menu-item__handle {
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  flex: 0 0 32px;
  min-height: 44px;
  border-radius: 6px;
  color: #555;
  cursor: grab;
  touch-action: none;
}
.menu-item__handle:active {
  cursor: grabbing;
}
.menu-item__handle:focus-visible,
.menu-item__summary:focus-visible,
.menu-editor__add-item:focus-visible {
  outline: 2px solid #3f51b5;
  outline-offset: 2px;
}
.menu-item__summary {
  text-align: left;
  flex: 1;
  align-self: stretch;
  padding: 12px 12px 12px 4px;
  font-size: 14px;
  overflow-wrap: anywhere;
}
.menu-item__summary:hover {
  background: #fafafa;
}
.menu-item__fields {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px 8px;
}
.menu-item__field {
  min-width: 0;
}
.menu-item__actions {
  display: flex;
  align-items: center;
  margin: 22px 0 0 8px;
}
.menu-item__note {
  grid-column: 1 / -1;
  margin: 0;
  font-size: 12px;
  color: #757575;
}
.menu-item__note--error {
  color: #c62828;
}
.menu-item--ghost {
  opacity: 0.35;
  background: #eef2ff;
  border-style: dashed;
}
.menu-item--chosen {
  box-shadow: 0 4px 14px #00000010;
}
.menu-editor__add-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 46px;
  padding: 12px;
  margin-top: 5px;
  text-align: left;
  border: 1px solid #d8d8d8;
  border-radius: 8px;
  font-size: 14px;
}
.menu-editor__add-item .v-icon {
  color: #2463eb;
}
.menu-editor__add-item:hover {
  background: #fafafa;
}
.menu-editor__navigation {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 1fr minmax(200px, 280px);
  gap: 20px;
  align-items: center;
}
.menu-editor__navigation p {
  margin: 5px 0 0;
  color: #757575;
  font-size: 13px;
}
.menu-editor__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.menu-editor__empty {
  text-align: center;
  padding: 40px 24px;
}
.menu-editor__empty p {
  margin: 14px 0 20px;
  color: #616161;
}
.menu-editor__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}
@media (max-width: 600px) {
  .menu-editor {
    padding: 20px 12px 32px;
  }
  .menu-editor__toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }
  .menu-editor__toolbar h1 {
    font-size: 24px;
  }
  .menu-editor__section-heading {
    flex-direction: column;
    gap: 4px;
  }
  .menu-item--expanded {
    flex-wrap: wrap;
  }
  .menu-item__fields {
    grid-template-columns: minmax(0, 1fr);
  }
  .menu-item__actions {
    margin: 8px 0 0 32px;
  }
  .menu-editor__navigation {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}
</style>
