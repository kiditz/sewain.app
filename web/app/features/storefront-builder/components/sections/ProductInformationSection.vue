<script setup lang="ts">
import { useStorefrontCatalog } from "../../composables/useStorefrontCatalog";
import type { StorefrontBlock, SectionLayout } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";
import { productInformationSettings } from "../../utils/product-information-layout";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
const store = useStorefrontProductStore();
const { product, gallery, selectedMedia } = storeToRefs(store);
const activeImageIndex = computed({
  get: () => {
    const index = gallery.value.indexOf(selectedMedia.value);
    return index < 0 ? 0 : index;
  },
  set: (index: number) => {
    const image = gallery.value[index];
    if (image) selectedMedia.value = image;
  },
});
const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
}>();
const { theme } = useStorefront();
const settings = computed(() => productInformationSettings(props.settings));
const layout = computed(() => settings.value.layout);
const sectionStyle = computed(() => ({
  ...getLayoutStyle(layout.value, theme.value),
  ...getPaddingStyle(settings.value.padding, theme.value),
  width: "100%",
  display: layout.value.direction === "horizontal" ? "grid" : "flex",
}));
const detailsLayout = computed<SectionLayout>(() => ({
  ...layout.value, direction: "vertical", alignment: "left", position: "top", width: "full", height: "auto",
}));
const catalog = useStorefrontCatalog();
if (props.preview && !store.product) {
  const { data: example } = await useAsyncData(catalog.queryKey("preview-product"), () => catalog.fetchProducts({ page: 0, size: 1, available: true }));
  if (example.value?.items[0]) store.setProduct(example.value.items[0]);
}
function selectImage(index: number) {
  activeImageIndex.value = index;
}
</script>
<template>
  <section class="product-information" :style="sectionStyle" :class="{ 'product-information--media-right': settings.mediaPosition === 'right' }">
    <div class="product-information__media">
      <div class="product-image">
        <v-carousel
          v-if="gallery.length"
          v-model="activeImageIndex"
          :height="460"
          :show-arrows="gallery.length > 1 ? 'hover' : false"
          hide-delimiters
          class="product-image__carousel"
        >
          <v-carousel-item v-for="(image, index) in gallery" :key="image">
            <img
              :src="image"
              :alt="`${product?.name ?? 'Product'} ${index + 1}`"
            />
          </v-carousel-item>
        </v-carousel>
        <span v-else>{{ $t("ui.product_image") }}</span>
      </div>

      <div v-if="gallery.length > 1" class="product-thumbnails">
        <v-slide-group v-model="activeImageIndex" show-arrows>
          <v-slide-group-item
            v-for="(image, index) in gallery"
            :key="image"
            :value="index"
            v-slot="{ isSelected }"
          >
            <button
              type="button"
              class="product-thumbnail"
              :class="{ 'product-thumbnail--active': isSelected }"
              :aria-label="`View product image ${index + 1}`"
              :aria-pressed="isSelected"
              @click="selectImage(index)"
            >
              <img :src="image" :alt="`${product?.name} ${index + 1}`" />
            </button>
          </v-slide-group-item>
        </v-slide-group>
      </div>
    </div>
    <div class="product-information__details">
      <BlockRenderer
        :blocks="blocks"
        :layout="detailsLayout"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
    </div>
  </section>
</template>
<style scoped>
.product-information {
  container: product-information / inline-size;
  box-sizing: border-box;
  min-width: 0;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  margin-inline: auto;
}
.product-information__media {
  min-width: 0;
  width: 100%;
}
.product-information--media-right .product-information__media {
  order: 1;
}
.product-image {
  height: 460px;
  border-radius: var(--store-radius);
  background: var(--store-image-background, var(--store-surface));
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
}
.product-image__carousel,
.product-image :deep(.v-window),
.product-image :deep(.v-window__container),
.product-image :deep(.v-carousel-item) {
  height: 100% !important;
}
.product-image__carousel {
  width: 100%;
}
.product-image__carousel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.product-image > span {
  position: absolute;
  top: 20px;
  left: 20px;
  font-size: 11px;
  letter-spacing: 0.12em;
  font-weight: 700;
  opacity: 0.55;
}
.product-thumbnails {
  margin-top: 12px;
  max-width: 100%;
  overflow: hidden;
}
.product-thumbnail {
  display: block;
  width: 76px;
  height: 76px;
  margin-inline-end: 10px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: var(--store-image-background, var(--store-surface));
}
.product-thumbnail--active {
  border-color: var(--store-primary);
}
.product-thumbnail img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-information__details {
  min-width: 0;
  width: 100%;
}
@container product-information (max-width: 760px) {
  .product-information__media,
  .product-information__details {
    grid-column: 1 / -1;
  }
  .product-information__media,
  .product-information--media-right .product-information__media {
    order: 0;
  }
  .product-image {
    height: 320px;
  }
}
</style>
