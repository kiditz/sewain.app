<script setup lang="ts">
import { imageBannerDefaults } from "../../../utils/image-banner";
import { productInformationSettings } from "../../../utils/product-information-layout";
import { navigationDefaults } from "../../../utils/navigation-section";
import type { StorefrontSection } from "~/types/storefront";
import ProductSelector from "../../ProductSelector.vue";
import EditorFieldRow from "../../EditorFieldRow.vue";
import LayoutField from "../../LayoutField.vue";
import AppearanceField from "../../AppearanceField.vue";
import MediaField from "../../MediaField.vue";
import ImageField from "../../blocks/settings/ImageField.vue";
import PaddingField from "../../PaddingField.vue";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront.ts";

const props = defineProps<{
  section: StorefrontSection;
}>();

const sectionSettings = computed(() => {
  if (props.section.type === "NAVIGATION") {
    const defaults = navigationDefaults();
    return { ...defaults, ...props.section.settings,
      layout: { ...defaults.layout, ...bannerImage(props.section.settings.layout) },
      padding: { ...defaults.padding, ...bannerImage(props.section.settings.padding) },
    };
  }
  if (props.section.type === "PRODUCT_INFORMATION") return productInformationSettings(props.section.settings);
  if (props.section.type !== "IMAGE_BANNER") return props.section.settings;
  const defaults = imageBannerDefaults();
  return { ...defaults, ...props.section.settings,
    layout: { ...defaults.layout, ...bannerImage(props.section.settings.layout) },
    padding: { ...defaults.padding, ...bannerImage(props.section.settings.padding) },
  };
});
function bannerImage(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}
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
  if (props.section.type === "NAVIGATION" && key === "sticky") return t("storefrontNavigation.sticky");
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
    <template v-for="(value, key) in sectionSettings" :key="key">
      <div v-if="section.type === 'IMAGE_BANNER' && key === 'image'">
        <ImageField :settings="bannerImage(value)" @update="(field, selected) => update('image', { ...bannerImage(value), [field]: selected })" />
        <EditorFieldRow :label="t('ui.alt_text')">
          <v-text-field :model-value="String(bannerImage(value).alt || '')" variant="outlined" density="compact" hide-details :aria-label="t('ui.alt_text')" @update:model-value="update('image', { ...bannerImage(value), alt: $event })" />
        </EditorFieldRow>
      </div>
      <EditorFieldRow v-else-if="section.type === 'PRODUCT_INFORMATION' && key === 'mediaPosition'" :label="t('ui.position')">
        <v-select :model-value="value" :items="[{ title: t('ui.left'), value: 'left' }, { title: t('ui.right'), value: 'right' }]" variant="outlined" density="compact" hide-details @update:model-value="update('mediaPosition', $event)" />
      </EditorFieldRow>
      <EditorFieldRow v-else-if="section.type === 'CART' && key === 'summaryPosition'" label="Posisi ringkasan">
        <v-select :model-value="value" :items="[{ title: 'Kiri', value: 'left' }, { title: 'Kanan', value: 'right' }]" variant="outlined" density="compact" hide-details @update:model-value="update('summaryPosition', $event)" />
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
        <ProductSelector
          :model-value="selectedIds(value)"
          :label="labelOf(String(key))"
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
        :settings="sectionSettings"
        @update="update"
      />
      <AppearanceField
        v-else-if="key === 'appearance'"
        :settings="sectionSettings"
        @update="update"
      />
      <PaddingField
        v-else-if="key === 'padding'"
        :settings="sectionSettings"
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
