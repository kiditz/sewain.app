<script setup lang="ts">
import type { SectionLayout, StorefrontPadding, StorefrontBlock, StorefrontProduct } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";
import { imageBannerDefaults } from "../../utils/image-banner";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: StorefrontProduct;
}>();
const { theme } = useStorefront();
const defaults = imageBannerDefaults();
const layout = computed(() => ({ ...defaults.layout, ...props.settings.layout as Partial<SectionLayout> }));
const blockLayout = computed<SectionLayout>(() => ({ ...layout.value, height: "auto" }));
const sectionStyle = computed(() => ({
  background: String(props.settings.background || defaults.background),
  maxWidth: layout.value.width === "page" ? `${theme.value.layout.contentMaxWidth}px` : undefined,
}));
const contentStyle = computed(() => ({
  ...getLayoutStyle(layout.value, theme.value),
  ...getPaddingStyle({ ...defaults.padding, ...props.settings.padding as StorefrontPadding }, theme.value),
  maxWidth: undefined, margin: 0, width: "100%",
}));
const image = computed(() => {
  const value = props.settings.image;
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as { src?: string; alt?: string }
    : {};
});
</script>
<template>
  <section
    class="image-banner"
    :style="sectionStyle"
  >
    <div v-if="image.src" class="image-banner__background">
      <v-img :src="image.src" :alt="image.alt || ''" cover class="image-banner__image" />
    </div>
    <div class="image-banner__inner" :style="contentStyle">
      <BlockRenderer
        :blocks="blocks"
        :layout="blockLayout"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
    </div>
  </section>
</template>
<style scoped>
.image-banner { position: relative; isolation: isolate; container: image-banner / inline-size; width: 100%; min-width: 0; box-sizing: border-box; border-radius: var(--store-radius); margin: 0 auto; }
.image-banner__background { position: absolute; inset: 0; border-radius: inherit; overflow: hidden; z-index: -1; }
.image-banner__image { width: 100%; height: 100%; }
.image-banner__inner { position: relative; min-width: 0; box-sizing: border-box; }
.image-banner__inner :deep(.builder-block) { min-width: 0; max-width: 100%; }
.image-banner__inner :deep(.block-heading) { font-size: clamp(28px, 4vw, 42px); }
@container image-banner (max-width: 640px) {
  .image-banner__inner :deep(.block-renderer) { flex-direction: column !important; align-items: flex-start !important; }
}
</style>
