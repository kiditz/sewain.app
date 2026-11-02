<script setup lang="ts">
import type { StorefrontBlock, SectionLayout } from "~/types/storefront";
import type { Product } from "~/types/product";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutHeight, getPaddingStyle } from "../../utils/block-settings";
import {
  slideshowSettings,
  slideContainingBlock,
  nextSlideId,
} from "../../utils/slideshow";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: Product;
}>();
const { theme } = useStorefront();
const { t } = useI18n();
const settings = computed(() => slideshowSettings(props.settings));
const slides = computed(() =>
  props.blocks.filter((block) => block.type === "SLIDE" && block.enabled),
);
const activeId = ref<string | null>(null);
const rootLayout: SectionLayout = {
  direction: "vertical",
  alignment: "left",
  position: "top",
  gap: 0,
  width: "full",
  height: "auto",
  customHeight: 0,
  alignTextBaseline: false,
};
const sectionStyle = computed(() => ({
  ...getPaddingStyle(settings.value.padding, theme.value),
  maxWidth:
    settings.value.layout.width === "page"
      ? `${theme.value.layout.contentMaxWidth}px`
      : undefined,
  "--slideshow-slide-height":
    settings.value.layout.height === "auto"
      ? "0px"
      : getLayoutHeight(settings.value.layout as SectionLayout),
}));

watch(
  slides,
  (items) => {
    if (!items.some((slide) => slide.id === activeId.value))
      activeId.value = items[0]?.id ?? null;
  },
  { immediate: true },
);
watch(
  [() => props.selectedBlockId, slides],
  ([id]) => {
    if (!props.preview) return;
    const selected = slideContainingBlock(slides.value, id);
    if (selected) activeId.value = selected.id;
  },
  { immediate: true },
);

const ready = ref(false);
const hovered = ref(false);
const focused = ref(false);
const paused = ref(false);
const reducedMotion = ref(false);
const hidden = ref(false);
const playing = computed(
  () =>
    ready.value &&
    !props.preview &&
    settings.value.autoplay &&
    slides.value.length > 1 &&
    !hovered.value &&
    !focused.value &&
    !paused.value &&
    !reducedMotion.value &&
    !hidden.value,
);
let timer: ReturnType<typeof setTimeout> | undefined;
let motion: MediaQueryList | undefined;
function updateMotion() {
  reducedMotion.value = motion?.matches ?? false;
}
function updateVisibility() {
  hidden.value = document.hidden;
}
function focusOut(event: FocusEvent) {
  if (
    !(event.currentTarget as HTMLElement).contains(
      event.relatedTarget as Node | null,
    )
  )
    focused.value = false;
}
watch([playing, activeId, () => settings.value.interval], () => {
  if (timer) clearTimeout(timer);
  if (playing.value)
    timer = setTimeout(() => {
      activeId.value = nextSlideId(slides.value, activeId.value);
    }, settings.value.interval * 1000);
});
onMounted(() => {
  motion = window.matchMedia("(prefers-reduced-motion: reduce)");
  updateMotion();
  updateVisibility();
  motion.addEventListener("change", updateMotion);
  document.addEventListener("visibilitychange", updateVisibility);
  ready.value = true;
});
onBeforeUnmount(() => {
  if (timer) clearTimeout(timer);
  motion?.removeEventListener("change", updateMotion);
  document.removeEventListener("visibilitychange", updateVisibility);
});
</script>

<template>
  <section
    v-if="slides.length || preview"
    class="slideshow-section"
    :style="sectionStyle"
    :aria-label="t('storefrontSlideshow.title')"
    aria-roledescription="carousel"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
    @focusin="focused = true"
    @focusout="focusOut"
  >
    <p v-if="!slides.length" class="slideshow-section__empty">
      {{ t("storefrontSlideshow.empty") }}
    </p>
    <v-window
      v-else
      v-model="activeId"
      continuous
      :show-arrows="settings.showArrows && slides.length > 1"
      :aria-live="playing ? 'off' : 'polite'"
      class="slideshow-section__window"
    >
      <v-window-item
        v-for="(slide, index) in slides"
        :key="slide.id"
        :value="slide.id"
        role="group"
        aria-roledescription="slide"
        :aria-label="
          t('storefrontSlideshow.slideOf', {
            current: index + 1,
            total: slides.length,
          })
        "
      >
        <BlockRenderer
          :blocks="[slide]"
          :layout="rootLayout"
          :preview="preview"
          :selected-block-id="selectedBlockId"
          :product-context="product"
        />
      </v-window-item>
    </v-window>
    <div
      v-if="slides.length > 1 && (settings.showIndicators || settings.autoplay)"
      class="slideshow-section__controls"
    >
      <template v-if="settings.showIndicators">
        <v-btn
          v-for="(slide, index) in slides"
          :key="slide.id"
          icon="mdi-circle"
          size="x-small"
          variant="text"
          :color="activeId === slide.id ? 'primary' : undefined"
          :aria-label="
            t('storefrontSlideshow.goToSlide', { number: index + 1 })
          "
          :aria-current="activeId === slide.id ? 'true' : undefined"
          @click.stop="activeId = slide.id"
        />
      </template>
      <v-btn
        v-if="settings.autoplay && !preview && !reducedMotion"
        :icon="paused ? 'mdi-play' : 'mdi-pause'"
        size="small"
        variant="text"
        :aria-label="
          t(paused ? 'storefrontSlideshow.play' : 'storefrontSlideshow.pause')
        "
        @click.stop="paused = !paused"
      />
    </div>
  </section>
</template>

<style scoped>
.slideshow-section {
  width: 100%;
  min-width: 0;
  margin-inline: auto;
  box-sizing: border-box;
  container: storefront-slideshow / inline-size;
  color: var(--store-text);
  background: var(--store-surface);
}
.slideshow-section__window {
  min-width: 0;
}
.slideshow-section__controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
}
.slideshow-section__empty {
  padding: 48px 24px;
  text-align: center;
}
@media (prefers-reduced-motion: reduce) {
  .slideshow-section :deep(.v-window-item) {
    transition: none !important;
  }
}
</style>
