<script setup lang="ts">
import { useStorefront } from "../../../composables/useStorefront";
import type { StorefrontProductCardStyle } from "~/types/storefront";
import EditorFieldRow from "../../EditorFieldRow.vue";

const props = defineProps<{
  value: unknown;
}>();

const emit = defineEmits<{
  update: [value: StorefrontProductCardStyle];
}>();

const { t } = useI18n();
const storefront = useStorefront();

const style = computed<StorefrontProductCardStyle>(() => {
  const value = props.value;
  const settings =
    value !== null && typeof value === "object"
      ? (value as Partial<StorefrontProductCardStyle>)
      : {};

  return {
    variant: settings.variant ?? "elevated",
    color: ["store-primary", "store-secondary", "none", "white"].includes(
      settings.color ?? "",
    )
      ? settings.color!
      : "store-primary",
  };
});

const variantItems = ["elevated", "flat", "tonal", "outlined", "text", "plain"].map(
  (value) => ({
    title: value.charAt(0).toUpperCase() + value.slice(1),
    value,
  }),
);
const colorItems = computed(() => [
  { title: t("ui.store_primary"), value: "store-primary" },
  { title: t("ui.store_secondary"), value: "store-secondary" },
  { title: t("ui.none"), value: "none" },
  { title: t("ui.white"), value: "white" },
]);

function swatchStyle(value: unknown) {
  return {
    backgroundColor:
      value === "store-primary"
        ? storefront.theme.value.colors.primary
        : value === "store-secondary"
          ? storefront.theme.value.colors.secondary
          : value === "white"
            ? "#ffffff"
            : "transparent",
    backgroundImage:
      value === "none"
        ? "linear-gradient(45deg, #ccc 25%, transparent 25%, transparent 75%, #ccc 75%), linear-gradient(45deg, #ccc 25%, white 25%, white 75%, #ccc 75%)"
        : undefined,
    backgroundSize: value === "none" ? "8px 8px" : undefined,
    backgroundPosition: value === "none" ? "0 0, 4px 4px" : undefined,
  };
}

function update(key: keyof StorefrontProductCardStyle, value: string) {
  emit("update", { ...style.value, [key]: value } as StorefrontProductCardStyle);
}
</script>

<template>
  <div class="product-card-style">
    <div class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.product_card_style") }}
    </div>
    <v-divider />
    <div class="d-flex flex-column ga-2 py-2">
      <EditorFieldRow :label="$t('ui.variant')">
        <v-select
          :model-value="style.variant"
          :items="variantItems"
          variant="outlined"
          density="compact"
          hide-details
          @update:model-value="update('variant', String($event))"
        />
      </EditorFieldRow>
      <EditorFieldRow :label="$t('ui.color')">
        <v-select
          :model-value="style.color"
          :items="colorItems"
          variant="outlined"
          density="compact"
          hide-details
          @update:model-value="update('color', String($event))"
        >
          <template #item="{ props: itemProps, item }">
            <v-list-item v-bind="itemProps">
              <template #prepend>
                <span
                  class="product-card-style__swatch"
                  :style="swatchStyle(item.value)"
                />
              </template>
            </v-list-item>
          </template>
          <template #selection="{ item }">
            <span class="d-flex align-center ga-2">
              <span
                class="product-card-style__swatch"
                :style="swatchStyle(item.value)"
              />
              {{ item.title }}
            </span>
          </template>
        </v-select>
      </EditorFieldRow>
    </div>
  </div>
</template>

<style scoped>
.product-card-style__swatch {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

</style>
