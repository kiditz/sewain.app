<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import type {
  SectionLayout,
  StorefrontBlock,
  StorefrontPadding,
} from "~/types/storefront";

const props = defineProps<{ block: StorefrontBlock }>();
const storefront = useStorefront();

const padding = computed(
  () => props.block.settings.padding as StorefrontPadding,
);
const layout = computed(
  () => props.block.settings.layout as SectionLayout | undefined,
);
const layoutStyle = computed(() =>
  getLayoutStyle(layout.value, storefront.theme.value),
);
const paddingStyle = computed(() =>
  getPaddingStyle(padding.value, storefront.theme.value),
);
const headerStyle = computed(() => ({
  ...layoutStyle.value,
  ...paddingStyle.value,
}));
</script>

<template>
  <div class="header-block" :style="headerStyle">
    <slot />
  </div>
</template>
