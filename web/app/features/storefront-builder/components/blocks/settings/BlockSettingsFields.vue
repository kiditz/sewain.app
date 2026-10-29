<script setup lang="ts">
import { computed } from "vue";
import { allPaddingZero, type StorefrontBlock } from "~/types/storefront";
import EditorFieldRow from "../../EditorFieldRow.vue";
import PaddingField from "../../PaddingField.vue";
import LayoutField from "../../LayoutField.vue";
import TextField from "./TextField.vue";
import BlockLinkField from "./BlockLinkField.vue";
import { isBlockLinkField } from "../../../utils/block-link";
import MenuField from "./MenuField.vue";
import TextLayoutField from "./TextLayoutField.vue";
import ProductCardStyleField from "./ProductCardStyleField.vue";
import ImageField from "./ImageField.vue";
import LogoField from "./LogoField.vue";
import BorderRadiusField from "../../BorderRadiusField.vue";
import { useStorefront } from "../../../composables/useStorefront";

const props = defineProps<{
  block: StorefrontBlock;
  collections: Array<{ id: string; name: string }>;
  productImage?: boolean;
}>();
const { t } = useI18n();
const storefront = useStorefront();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const collectionItems = computed(() =>
  props.collections.map((collection) => ({
    title: collection.name,
    value: collection.id,
  })),
);

const aspectRatioItems = computed(() => [
  { title: t("ui.auto"), value: "auto" },
  { title: t("ui.portrait"), value: "portrait" },
  { title: t("ui.square"), value: "square" },
  { title: t("ui.landscape"), value: "landscape" },
]);

function selectedAspectRatio(value: unknown): string {
  const ratio = String(value ?? "auto");
  if (["auto", "portrait", "square", "landscape"].includes(ratio)) return ratio;
  if (ratio === "1/1") return "square";
  if (["3/4", "2/3"].includes(ratio)) return "portrait";
  return "landscape";
}

function asSettings(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

const editableSettings = computed(() => Object.fromEntries(Object.entries(props.block.settings).filter(([key]) => key !== "linkTargets")));

const blockSettings = computed(() => {
  if (["PRODUCT_TITLE", "PRODUCT_PRICE"].includes(props.block.type)) {
    return {
      ...editableSettings.value,
      padding: editableSettings.value.padding ?? allPaddingZero,
      layout: {
        width: "match",
        maxWidth: "normal",
        ...asSettings(editableSettings.value.layout),
      },
    };
  }

  if (!["TEXT", "HEADING", "EYEBROW"].includes(props.block.type))
    return editableSettings.value;

  const { color, backgroundColor, ...textSettings } = editableSettings.value;
  return {
    ...textSettings,
    borderRadius: { topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0, ...asSettings(textSettings.borderRadius) },
    style: { color: "", backgroundColor: "#00000000", ...asSettings(textSettings.style) },
    padding: editableSettings.value.padding ?? allPaddingZero,
    layout: {
      width: "match",
      maxWidth: "normal",
      ...asSettings(editableSettings.value.layout),
    },
  };
});

function selectedIds(value: unknown): string[] {
  return String(value ?? "")
    .split(",")
    .filter(Boolean);
}

function labelOf(key: string): string {
  const labels: Record<string, string> = {
    collectionIds: "ui.collections_to_show",
    level: "ui.heading_level",
    style: "ui.button_style",
    url: "ui.url",
    src: "ui.image_url",
    alt: "ui.alt_text",
    aspectRatio: "ui.aspect_ratio",
  };
  if (labels[key]) return t(labels[key]!);
  const label = key.replace(/([A-Z])/g, " $1");
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function update(key: string, value: unknown) {
  emit("update", key, value);
}
</script>

<template>
  <div class="settings-fields">
    <EditorFieldRow
      v-if="block.type === 'IMAGE' && productImage"
      :label="t('ui.image_display_type')"
    >
      <v-select
        :model-value="String(block.settings.displayType ?? 'single')"
        :items="[
          { title: t('ui.single_image'), value: 'single' },
          { title: t('productList.types.carousel'), value: 'carousel' },
        ]"
        variant="outlined"
        density="compact"
        hide-details
        @update:model-value="update('displayType', String($event ?? 'single'))"
      />
    </EditorFieldRow>

    <template v-for="(value, key) in blockSettings" :key="key">
      <template v-if="key === 'displayType' && block.type === 'IMAGE'" />

      <EditorFieldRow
        v-if="key === 'collectionIds'"
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
          @update:model-value="update(String(key), $event.join(','))"
        />
      </EditorFieldRow>

      <EditorFieldRow v-else-if="isBlockLinkField(String(key), value)" :label="labelOf(String(key)).replace(/url|href/gi, 'Link')">
        <BlockLinkField :settings="block.settings" :field-key="String(key)" :label="labelOf(String(key)).replace(/url|href/gi, 'Link')" @update="update" />
      </EditorFieldRow>

      <!-- <EditorFieldRow v-else-if="block.type === 'MENU' && key === 'menuId'" label="Menu">
        <v-select :model-value="value ?? null" :items="[
          ...storefront.MenuItems.value.map(menu => ({ title: menu.name, value: menu.id })),
        ]" variant="outlined" density="compact" hide-details aria-label="Navigation menu" @update:model-value="update('menuId', $event)" />
      </EditorFieldRow> -->
      <MenuField v-else-if="block.type === 'MENU' && key === 'menuId'" :model-value="typeof value === 'string' ? value : null" @update="update" />

      <EditorFieldRow v-else-if="key === 'aspectRatio'" :label="labelOf(String(key))">
        <v-select
          :model-value="selectedAspectRatio(value)"
          :items="aspectRatioItems"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event ?? 'auto'))"
        />
      </EditorFieldRow>

      <!-- <LogoField v-else-if="key === 'src' && block.type === 'LOGO'" :settings="block.settings" @update="update" /> -->

      <template v-else-if="key === 'src' && block.type === 'IMAGE'">
        <ImageField
          v-if="!productImage"
          :settings="block.settings"
          @update="update"
        />
      </template>

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

      <div class="d-flex flex-column" v-else-if="key === 'style' && ['EYEBROW', 'HEADING', 'TEXT'].includes(block.type)">
        <div class="text-title-medium font-weight-bold px-4 py-2">Style</div>
        <v-divider />
        <div class="d-flex flex-column py-2 ga-2">
        <EditorFieldRow v-for="styleKey in ['color', 'backgroundColor']" :key="styleKey" :label="styleKey === 'color' ? 'Warna teks' : t('ui.background_color')">
          <v-color-input
            :model-value="String(asSettings(value)[styleKey] || (styleKey === 'backgroundColor' ? '#00000000' : block.type === 'EYEBROW' ? storefront.theme.value.colors.primary : storefront.theme.value.colors.text))"
            color-format="hexa"
            variant="outlined"
            density="compact"
            hide-details
            color-pip
            :aria-label="styleKey === 'color' ? 'Warna teks' : t('ui.background_color')"
            @update:model-value="update('style', { ...asSettings(value), [styleKey]: $event ?? '' })"
          />
        </EditorFieldRow>
        </div>
      </div>

      <TextField v-else-if="key === 'text' || key === 'label'" :block="block" :field-key="key === 'label' ? 'label' : 'text'" @update="update" />

      <EditorFieldRow v-else-if="key === 'level'" :label="labelOf(String(key))">
        <v-select
          :model-value="String(value ?? '')"
          :items="['h1', 'h2', 'h3']"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event))"
        />
      </EditorFieldRow>

      <ProductCardStyleField
        v-else-if="key === 'style' && block.type === 'PRODUCT_CARD'"
        :value="value"
        @update="update('style', $event)"
      />
      <EditorFieldRow v-else-if="key === 'style'" :label="labelOf(String(key))">
        <v-select
          :model-value="String(value ?? '')"
          :items="['primary', 'secondary', 'outline']"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event))"
        />
      </EditorFieldRow>

      <PaddingField
        v-else-if="key === 'padding'"
        :settings="block.settings"
        @update="update"
      />
      <LayoutField
        v-else-if="key === 'layout' && (['HEADER', 'MENU'].includes(block.type) || block.type.startsWith('CHECKOUT_'))"
        :settings="block.settings"
        @update="update"
      />
      <TextLayoutField
        v-else-if="key === 'layout'"
        :settings="block.settings"
        @update="update"
      />
      <BorderRadiusField
        v-else-if="key === 'borderRadius' && ['IMAGE', 'PRODUCT_CARD', 'EYEBROW', 'HEADING', 'TEXT'].includes(block.type)"
        :settings="block.settings"
        :default-value="['EYEBROW', 'HEADING', 'TEXT'].includes(block.type) ? 0 : storefront.theme.value.layout.borderRadius"
        @update="update"
      />
      <EditorFieldRow v-else :label="labelOf(String(key))">
        <v-text-field
          :model-value="String(value ?? '')"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event ?? ''))"
        />
      </EditorFieldRow>
    </template>
  </div>
</template>

<style scoped>
.settings-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
