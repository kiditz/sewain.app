<script setup lang="ts">
import type { StorefrontSection } from "~/types/storefront";
import EditorFieldRow from "../../EditorFieldRow.vue";
import LayoutField from "../../LayoutField.vue";
import AppearanceField from "../../AppearanceField.vue";
import MediaField from "../../MediaField.vue";
import PaddingField from "../../PaddingField.vue";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront.ts";

const props = defineProps<{
  section: StorefrontSection;
}>();

const { t } = useI18n();
const storefront = useStorefront();
const allProductsSource = "__all_products__";
const productSourceItems = computed(() => [
  { title: t("ui.all_products"), value: allProductsSource },
  ...storefront.collections.value.map((collection) => ({
    title: collection.name,
    value: collection.id,
  })),
]);
const productItems = computed(() =>
  storefront.products.value
    .filter((product) => product.status === "ACTIVE")
    .map((product) => ({ title: product.name, value: product.id })),
);
const collectionItems = computed(() =>
  storefront.collections.value.map((collection) => ({
    title: collection.name,
    value: collection.id,
  })),
);
const featuredDisplayTypes = computed(() => [
  { title: t("productList.types.carousel"), value: "carousel" },
  { title: t("productList.types.grid"), value: "grid" },
  { title: t("productList.types.editorial"), value: "editorial" },
]);

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

function update(key: string, value: unknown) {
  emit("update", key, value);
}

function selectedProductSource(value: unknown): string {
  return value == null ||
    value === "" ||
    value === "all" ||
    value === "featured"
    ? allProductsSource
    : String(value);
}

function selectedIds(value: unknown): string[] {
  return String(value ?? "")
    .split(",")
    .filter(Boolean);
}

function labelOf(key: string) {
  const labels: Record<string, string> = {
    background: "ui.background",
    alignment: "ui.alignment",
  };
  if (props.section.type === "PRODUCT_GRID") {
    const productGridLabels: Record<string, string> = {
      columns: "productGrid.settings.columns",
      productsPerPage: "productGrid.settings.productsPerPage",
      showFilters: "productGrid.settings.showFilters",
      showSorting: "productGrid.settings.showSorting",
      showProductCount: "productGrid.settings.showProductCount",
    };
    if (productGridLabels[key]) return t(productGridLabels[key]!);
  }
  if (props.section.type === "FEATURED_PRODUCTS") {
    const featuredLabels: Record<string, string> = {
      source: "ui.product_source",
      displayType: "productList.settings.displayType",
      count: "productList.settings.count",
      columns: "productList.settings.columns",
      mobileColumns: "productList.settings.mobileColumns",
      productIds: "ui.featured_products",
    };
    if (featuredLabels[key]) return t(featuredLabels[key]!);
  }
  if (props.section.type === "COLLECTION_LIST") {
    const collectionLabels: Record<string, string> = {
      collectionIds: "ui.collections_to_show",
      displayType: "productList.settings.displayType",
      count: "productList.settings.count",
      columns: "productList.settings.columns",
      mobileColumns: "productList.settings.mobileColumns",
    };
    if (collectionLabels[key]) return t(collectionLabels[key]!);
  }
  const label = labels[key] ? t(labels[key]!) : key.replace(/([A-Z])/g, " $1");
  return label.charAt(0).toUpperCase() + label.slice(1);
}
</script>

<template>
  <div class="settings-fields">
    <template v-for="(value, key) in section.settings" :key="key">
      <EditorFieldRow v-if="section.type === 'HEADER' && key === 'menuId'" label="Menu">
        <v-select
          :model-value="value ?? null"
          :items="[
            { title: 'Default header menu', value: null },
            { title: 'No menu', value: '__none__' },
            ...storefront.MenuItems.value.map(menu => ({ title: menu.name, value: menu.id })),
          ]"
          variant="outlined"
          density="compact"
          hide-details
          aria-label="Header menu"
          @update:model-value="update('menuId', $event)"
        />
      </EditorFieldRow>
      <EditorFieldRow
        v-else-if="section.type === 'COLLECTION_LIST' && key === 'collectionIds'"
        :label="labelOf(String(key))"
      >
        <v-autocomplete
          :model-value="selectedIds(value)"
          :items="collectionItems"
          variant="outlined"
          density="compact"
          chips
          closable-chips
          multiple
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update('collectionIds', $event.join(','))"
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="section.type === 'FEATURED_PRODUCTS' && key === 'source'"
        :label="labelOf(String(key))"
      >
        <v-select
          :model-value="selectedProductSource(value)"
          :items="productSourceItems"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="
            update(
              'source',
              $event === allProductsSource ? null : String($event),
            )
          "
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="
          ['FEATURED_PRODUCTS', 'COLLECTION_LIST'].includes(section.type) && key === 'displayType'
        "
        :label="labelOf(String(key))"
      >
        <v-select
          :model-value="String(value ?? 'grid')"
          :items="featuredDisplayTypes"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update('displayType', String($event ?? 'grid'))"
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="section.type === 'FEATURED_PRODUCTS' && key === 'productIds'"
        :label="labelOf(String(key))"
      >
        <v-autocomplete
          :model-value="selectedIds(value)"
          :items="productItems"
          variant="outlined"
          density="compact"
          chips
          closable-chips
          multiple
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update('productIds', $event.join(','))"
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="['FEATURED_PRODUCTS', 'COLLECTION_LIST'].includes(section.type) && key === 'columns'"
        :label="labelOf(String(key))"
      >
        <div class="columns-control">
          <v-slider
            :model-value="Math.max(1, Math.min(8, Number(value) || 3))"
            min="1"
            max="8"
            step="1"
            hide-details
            thumb-label
            @update:model-value="update('columns', Number($event))"
          />
          <v-text-field
            :model-value="Math.max(1, Math.min(8, Number(value) || 3))"
            type="number"
            min="1"
            max="8"
            variant="outlined"
            density="compact"
            hide-details
            :aria-label="labelOf(String(key))"
            @update:model-value="
              update('columns', Math.max(1, Math.min(8, Number($event) || 1)))
            "
          />
        </div>
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="
          ['FEATURED_PRODUCTS', 'COLLECTION_LIST'].includes(section.type) && key === 'mobileColumns'
        "
        :label="labelOf(String(key))"
      >
        <v-btn-toggle
          :model-value="Number(value) === 2 ? 2 : 1"
          color="primary"
          density="compact"
          divided
          mandatory
          @update:model-value="update('mobileColumns', Number($event))"
        >
          <v-btn :value="1">1</v-btn>
          <v-btn :value="2">2</v-btn>
        </v-btn-toggle>
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="typeof value === 'boolean'"
        :label="labelOf(String(key))"
      >
        <v-switch
          :model-value="value"
          color="primary"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), $event)"
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="
          ['body', 'description', 'subtitle'].some((part) =>
            String(key).toLowerCase().includes(part),
          )
        "
        :label="labelOf(String(key))"
      >
        <v-textarea
          :model-value="String(value ?? '')"
          variant="outlined"
          density="compact"
          rows="3"
          auto-grow
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), $event)"
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="typeof value === 'number'"
        :label="labelOf(String(key))"
      >
        <v-number-input
          :model-value="value"
          variant="outlined"
          density="compact"
          control-variant="stacked"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), Number($event))"
        />
      </EditorFieldRow>

      
      <EditorFieldRow
        v-else-if="typeof value === 'string'"
        :label="labelOf(String(key))"
      >
        <v-text-field
          :model-value="String(value ?? '')"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event ?? ''))"
        />
      </EditorFieldRow>
      <MediaField
        v-else-if="key === 'medias'"
        :settings="{ medias: value }"
        @update="update"
      />
      <LayoutField
        v-else-if="key === 'layout' && value != null"
        :settings="section.settings"
        @update="update"
      />
      <AppearanceField
        v-else-if="key === 'appearance'"
        :settings="section.settings"
        @update="update"
      />
      <PaddingField
        v-else-if="key === 'padding'"
        :settings="section.settings"
        @update="update"
      />
    </template>
  </div>
</template>

<style scoped>
.settings-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.columns-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 76px;
  align-items: center;
  gap: 8px;
}
</style>
