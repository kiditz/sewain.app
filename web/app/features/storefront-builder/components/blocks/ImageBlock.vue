<script setup lang="ts">
import {
  getBlockSettings,
  getBorderRadiusStyle,
  getPaddingStyle,
} from "../../utils/block-settings";
import type {
  StorefrontBlock,
  StorefrontBorderRadius,
  StorefrontPadding,
} from "~/types/storefront";
import type { Product } from "~/types/product";
import { useStorefront } from "../../composables/useStorefront";

const props = defineProps<{
  block: StorefrontBlock;
  productContext?: Product;
  preview?: boolean;
  selectedBlockId?: string | null;
  productDescription?: boolean;
}>();

const settings = computed(() =>
  getBlockSettings<{
    src?: string;
    alt?: string;
    url?: string;
    displayType?: "single" | "carousel";
    aspectRatio?: string;
    borderRadius?: StorefrontBorderRadius;
  }>(props.block),
);
const productImage = computed(
  () => productImages.value[0] ?? "",
);
const productImages = computed(() => {
  if (!props.productContext) return [];

  return [
    props.productContext.media,
    ...props.productContext.variants
      .filter((variant) => variant.active)
      .flatMap((variant) => variant.mediaList),
  ].filter((src, index, images) => src !== "" && images.indexOf(src) === index);
});
const isProductCarousel = computed(
  () =>
    !!props.productContext &&
    settings.value.displayType === "carousel" &&
    productImages.value.length > 1,
);
const aspectRatio = computed(() => {
  const value = settings.value.aspectRatio || "auto";
  const ratios: Record<string, string> = {
    auto: "auto",
    portrait: "3 / 4",
    square: "1 / 1",
    landscape: "16 / 9",
  };
  return ratios[value] || value;
});
const { theme } = useStorefront();
const fixedAspectRatio = computed(() => aspectRatio.value !== "auto");
const borderRadiusStyle = computed(() =>
  getBorderRadiusStyle(settings.value.borderRadius, theme.value),
);

const padding = computed(
  () => props.block.settings.padding as StorefrontPadding,
);
const paddingStyle = computed(() =>
  getPaddingStyle(padding.value, theme.value),
);
</script>

<template>
  <a
    v-if="settings.url && !productContext"
    :href="settings.url"
    class="block-image"
    :class="{ 'block-image--fixed-ratio': fixedAspectRatio }"
    :style="{
      ...(fixedAspectRatio ? { aspectRatio } : {}),
      ...borderRadiusStyle,
      ...paddingStyle,
    }"
  >
    <img
      v-if="settings.src"
      :src="settings.src || productImage"
      :alt="settings.alt || props?.productContext?.name || ''"
      :style="{ ...borderRadiusStyle, ...paddingStyle }"
    />
  </a>

  <div
    v-else
    class="block-image"
    :class="{ 'block-image--fixed-ratio': fixedAspectRatio }"
    :style="{
      ...(fixedAspectRatio ? { aspectRatio } : {}),
      ...borderRadiusStyle,
      ...paddingStyle,
    }"
  >
    <v-carousel
      v-if="isProductCarousel"
      height="100%"
      show-arrows="hover"
      hide-delimiters
      class="block-image__carousel"
    >
      <v-carousel-item v-for="(src, index) in productImages" :key="src">
        <img
          :src="src"
          :alt="`${productContext?.name ?? ''} ${index + 1}`"
        />
      </v-carousel-item>
    </v-carousel>
    <img
      v-else-if="settings.src || productImage"
      :src="settings.src || productImage"
      :alt="settings.alt || props?.productContext?.name || ''"
      :style="{ ...borderRadiusStyle, ...paddingStyle }"
    />
    <div v-else class="block-block-image-placeholder">
      {{ $t("ui.no_image_selected") }}
    </div>
  </div>
</template>

<style scoped>
.block-image {
  display: block;
  margin: 0;
  width: 100%;
  overflow: hidden;
}
.block-image img {
  display: block;
  width: 100%;
  height: auto;
  object-fit: cover;
}
.block-image--fixed-ratio img {
  height: 100%;
}
.block-image__carousel,
.block-image__carousel :deep(.v-window),
.block-image__carousel :deep(.v-window__container),
.block-image__carousel :deep(.v-carousel-item) {
  height: 100%;
}
.block-image__carousel img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.block-image figcaption {
  font-size: 12px;
  opacity: 0.6;
  margin-top: 6px;
}
.block-image-placeholder {
  min-height: 120px;
  border: 1px dashed #9ca3af;
  border-radius: var(--store-radius);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #6b7280;
}
.block-image-placeholder small {
  font-size: 12px;
}
</style>
