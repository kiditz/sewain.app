<script setup lang="ts">
import { computed } from "vue";
import type {
  StorefrontBlock,
  StorefrontProduct,
  SectionLayout,
  StorefrontPadding,
} from "~/types/storefront";
import type { Media } from "~/types/theme";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getLayoutWidthStyle, getPaddingStyle } from "../../utils/block-settings";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: StorefrontProduct;
}>();

const storefront = useStorefront();

const layout = computed(() => props.settings.layout as SectionLayout);

const sectionStyle = computed(() => getLayoutWidthStyle(layout.value ?? { width: "page" }, storefront.theme.value));
const blockLayout = computed(() => ({ ...layout.value, width: "full" as const, height: "auto" as const }));

const padding = computed(() => props.settings.padding as StorefrontPadding);
const layoutStyle = computed(() =>
  getLayoutStyle({ ...layout.value, width: "full", height: layout.value?.height ?? "auto" }, storefront.theme.value),
);

const appearance = computed(() => {
  const value = props.settings.appearance;
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
});

const overlayOpacity = computed(() => {
  const value = Number(appearance.value.overlayOpacity ?? 30);
  return Number.isFinite(value) ? Math.min(100, Math.max(0, value)) / 100 : 0.3;
});

const backgroundStyle = computed(() => ({
  backgroundColor: String(
    appearance.value.backgroundColor ?? "#00000000",
  ),
}));

const overlayStyle = computed(() => ({
  backgroundColor: String(appearance.value.overlayColor ?? "#121212"),
  opacity: overlayOpacity.value,
}));

const medias = computed(() => {
  return ((props.settings.medias as Media[] | undefined) ?? []).filter(
    (media) => media.src,
  );
});

const paddingStyle = computed(() =>
  getPaddingStyle(padding.value, storefront.theme.value),
);
</script>

<template>
  <section class="hero-section" :style="{ ...backgroundStyle, ...sectionStyle }">
    <div v-if="medias.length" class="hero-section__background">
      <v-img
        v-for="(media, index) in medias"
        :key="`${media.src}-${index}`"
        class="hero-section__background-image"
        :src="media.src"
        :alt="media.alt ?? ''"
        cover
        aria-hidden="true"
      />
    </div>

    <div
      v-if="medias.length"
      class="hero-section__overlay"
      :style="overlayStyle"
    />

    <div class="hero-section__inner" :style="{...layoutStyle, ...paddingStyle}">
      <BlockRenderer
        :layout="blockLayout"
        :blocks="blocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
    </div>
  </section>
</template>

<style scoped>
.hero-section {
  position: relative;
  isolation: isolate;

  /* Jangan pakai overflow: hidden karena bisa memotong outline */
}

.hero-section__background {
  position: absolute;
  inset: 0;
  z-index: -2;
  display: flex;
  flex-direction: row;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.hero-section__background-image {
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
}

.hero-section__overlay {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

.hero-section__inner {
  position: relative;
  margin: auto;
}
</style>
