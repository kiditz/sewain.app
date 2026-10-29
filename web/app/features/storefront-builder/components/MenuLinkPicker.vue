<script setup lang="ts">
import type { StorefrontMenuItem, StorefrontPage } from "~/types/storefront";
import type { Product } from "~/types/product";
import imagePlaceholder from "~/assets/images/placeholder.svg";
import { menuItemTitle } from "../utils/storefront-menu";
import { useStorefrontCatalog } from "../composables/useStorefrontCatalog";
const props = defineProps<{
  item: StorefrontMenuItem;
  pages: StorefrontPage[];
  disabled?: boolean;
  error?: boolean;
  id: string;
  label?: string;
  placeholder?: string;
  includeCart?: boolean;
}>();
const emit = defineEmits<{
  select: [
    destination: { pageId?: string; target?: StorefrontMenuItem["target"] },
  ];
}>();
const catalog = useStorefrontCatalog();
const open = ref(false);
type PickerGroup = "Products" | "Collections" | "Pages";
type MenuEntry =
  | { title: PickerGroup; icon: string; kind?: never }
  | { title: "Home" | "Orders" | "Cart"; icon: string; kind: "HOME" | "CHECKOUT" | "CART" };
const group = ref<PickerGroup | null>(null);
const search = ref("");
const products = ref<Product[]>([]);
const page = ref(0);
const total = ref(0);
const loading = ref(false);
const failed = ref(false);
let sequence = 0;
let timer: ReturnType<typeof setTimeout> | undefined;
const baseGroups: MenuEntry[] = [
  { title: "Home", icon: "mdi-home-outline", kind: "HOME" },
  { title: "Products", icon: "mdi-package-variant-closed" },
  { title: "Collections", icon: "mdi-folder-outline" },
  { title: "Pages", icon: "mdi-file-document-outline" },
  { title: "Orders", icon: "mdi-receipt-text-outline", kind: "CHECKOUT" },
];
const groups = computed<MenuEntry[]>(() => props.includeCart ? [...baseGroups, { title: "Cart", icon: "mdi-cart-outline", kind: "CART" }] : baseGroups);
const icon = computed(() =>
  props.item.target?.type === "PRODUCT"
    ? "mdi-package-variant-closed"
    : props.item.target?.type === "COLLECTION"
      ? "mdi-folder-outline"
      : "mdi-link-variant",
);
const pageChoices = computed(() =>
  props.pages.filter(
    (p) =>
      p.kind === "CUSTOM" &&
      p.title.toLowerCase().includes(search.value.toLowerCase()),
  ),
);
const collectionChoices = computed(() =>
  catalog.collections.value.filter((c) =>
    c.name.toLowerCase().includes(search.value.toLowerCase()),
  ),
);
function choosePage(p: StorefrontPage) {
  emit("select", { pageId: p.id });
  open.value = false;
}
function chooseEntity(
  type: "PRODUCT" | "COLLECTION",
  entity: { id: string; name: string; slug: string },
) {
  emit("select", {
    target: { type, id: entity.id, title: entity.name, slug: entity.slug },
  });
  open.value = false;
}
async function fetchProducts(nextPage = 0) {
  const request = ++sequence;
  loading.value = true;
  failed.value = false;
  try {
    const result = await catalog.fetchProducts({
      page: nextPage,
      size: 20,
      keyword: search.value.trim() || undefined,
    });
    if (request !== sequence) return;
    products.value = nextPage
      ? [...products.value, ...result.items]
      : result.items;
    page.value = nextPage;
    total.value = result.totalItems;
  } catch {
    if (request === sequence) failed.value = true;
  } finally {
    if (request === sequence) loading.value = false;
  }
}
async function fetchCollections() {
  const request = ++sequence;
  loading.value = true;
  failed.value = false;
  try {
    await catalog.loadCollections();
  } catch {
    if (request === sequence) failed.value = true;
  } finally {
    if (request === sequence) loading.value = false;
  }
}
function resetPending() {
  ++sequence;
  if (timer) clearTimeout(timer);
  loading.value = false;
  failed.value = false;
}
function back() {
  resetPending();
  group.value = null;
  search.value = "";
}
function enter(entry: MenuEntry) {
  if (entry.kind) {
    const destination = props.pages.find((p) => p.kind === entry.kind);
    if (destination) choosePage(destination);
    return;
  }
  resetPending();
  group.value = null;
  search.value = "";
  group.value = entry.title;
  if (group.value === "Products") {
    products.value = [];
    total.value = 0;
    void fetchProducts();
  }
  if (group.value === "Collections") void fetchCollections();
}
watch(
  search,
  () => {
    if (group.value !== "Products") return;
    resetPending();
    products.value = [];
    total.value = 0;
    loading.value = true;
    timer = setTimeout(() => {
      void fetchProducts();
    }, 250);
  },
  { flush: "sync" },
);
watch(open, (value) => {
  if (!value) back();
});
onBeforeUnmount(resetPending);
</script>
<template>
  <v-menu
    v-model="open"
    :close-on-content-click="false"
    location="bottom start"
    :max-width="420"
  >
    <template #activator="{ props: activator }">
      <v-btn
        :id="id"
        v-bind="activator"
        class="menu-link-picker__trigger"
        :class="{ 'menu-link-picker__trigger--error': error }"
        variant="outlined"
        :prepend-icon="icon"
        append-icon="mdi-chevron-down"
        :disabled="disabled"
        :aria-label="label || 'Pilih link menu'"
        >{{ menuItemTitle(item, pages) || placeholder || "Pilih link" }}</v-btn
      >
    </template>
    <v-card class="menu-link-picker" rounded="lg" min-width="280">
      <v-list v-if="!group" density="compact" aria-label="Jenis link menu">
        <v-list-item
          v-for="entry in groups"
          :key="entry.title"
          :title="entry.title"
          :prepend-icon="entry.icon"
          :append-icon="entry.kind ? undefined : 'mdi-chevron-right'"
          :disabled="!!entry.kind && !pages.some((p) => p.kind === entry.kind)"
          @click="enter(entry)"
        />
      </v-list>
      <template v-else>
        <v-btn
          variant="text"
          prepend-icon="mdi-arrow-left"
          class="ma-2"
          @click="back"
          >{{ group }}</v-btn
        >
        <v-text-field
          v-model="search"
          :label="`Cari ${group.toLowerCase()}`"
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          density="compact"
          hide-details
          class="mx-3 mb-2"
        />
        <v-progress-linear v-if="loading" indeterminate color="primary" />
        <v-list
          class="menu-link-picker__results"
          density="compact"
          :aria-label="`List of ${group}`"
        >
          <template v-if="group === 'Products'">
            <v-list-item
              v-if="!search && pages.some((p) => p.kind === 'PRODUCT_GRID')"
              title="All products"
              prepend-icon="mdi-view-grid-outline"
              @click="choosePage(pages.find((p) => p.kind === 'PRODUCT_GRID')!)"
            />
            <v-list-item
              v-for="product in products"
              :key="product.id"
              :title="product.name"
              @click="chooseEntity('PRODUCT', product)"
            >
              <template #prepend>
                <v-avatar size="40" rounded="lg" class="mr-3">
                  <v-img
                    :src="
                      product.media ||
                      product.variants?.find((variant) => variant.active)
                        ?.mediaList?.[0] ||
                      imagePlaceholder
                    "
                    alt=""
                    cover
                  >
                    <template #error
                      ><v-img :src="imagePlaceholder" alt="" cover
                    /></template>
                  </v-img>
                </v-avatar>
              </template>
            </v-list-item>
            <v-list-item v-if="products.length < total"
              ><v-btn
                variant="text"
                :loading="loading"
                :disabled="loading"
                @click="fetchProducts(page + 1)"
                >Muat lebih banyak</v-btn
              ></v-list-item
            >
            <v-list-item
              v-if="!loading && !failed && !products.length"
              title="Produk tidak ditemukan."
            />
          </template>
          <template v-else-if="group === 'Collections'">
            <v-list-item
              v-if="!search && pages.some((p) => p.kind === 'COLLECTIONS')"
              title="All collections"
              prepend-icon="mdi-folder-multiple-outline"
              @click="choosePage(pages.find((p) => p.kind === 'COLLECTIONS')!)"
            />
            <v-list-item
              v-for="collection in collectionChoices"
              :key="collection.id"
              :title="collection.name"
              @click="chooseEntity('COLLECTION', collection)"
            >
              <template #prepend>
                <v-avatar size="40" rounded="lg" class="mr-3">
                  <v-img
                    :src="collection.imageUrl || imagePlaceholder"
                    alt=""
                    cover
                  >
                    <template #error
                      ><v-img :src="imagePlaceholder" alt="" cover
                    /></template>
                  </v-img>
                </v-avatar>
              </template>
            </v-list-item>
            <v-list-item
              v-if="!loading && !failed && !collectionChoices.length"
              title="Koleksi tidak ditemukan."
            />
          </template>
          <template v-else>
            <v-list-item
              v-for="p in pageChoices"
              :key="p.id"
              :title="p.title"
              :subtitle="p.status === 'DRAFT' ? 'Draft' : undefined"
              prepend-icon="mdi-file-document-outline"
              @click="choosePage(p)"
            />
            <v-list-item
              v-if="!pageChoices.length"
              title="Halaman tidak ditemukan."
            />
          </template>
          <v-list-item v-if="failed" title="Daftar gagal dimuat."
            ><v-btn
              variant="text"
              @click="
                group === 'Products' ? fetchProducts() : fetchCollections()
              "
              >Coba lagi</v-btn
            ></v-list-item
          >
        </v-list>
      </template>
    </v-card>
  </v-menu>
</template>
<style scoped>
.menu-link-picker__trigger {
  width: 100%;
  justify-content: flex-start;
  text-transform: none;
  letter-spacing: normal;
  height: 40px;
  border-color: #bdbdbd;
}
.menu-link-picker__trigger :deep(.v-btn__content) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
  flex: 1;
}
.menu-link-picker__trigger--error {
  border-color: #c62828;
}
.menu-link-picker {
  max-width: calc(100vw - 32px);
}
.menu-link-picker__results {
  max-height: 320px;
  overflow-y: auto;
}
</style>
