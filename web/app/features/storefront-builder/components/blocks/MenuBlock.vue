<script setup lang="ts">
import type { SectionLayout, StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { getPaddingStyle, getLayoutStyle } from "../../utils/block-settings";
import StorefrontMenuLinks from "~/components/storefront/StorefrontMenuLinks.vue";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { theme } = useStorefront();
const menuId = computed(() => props.block.settings.menuId as string | null | undefined);
const style = computed(() => {
  return {
  ...getPaddingStyle(props.block.settings.padding as StorefrontPadding, theme.value),  
  ...getLayoutStyle(props.block.settings.layout as SectionLayout, theme.value),
  }
});

</script>
<template>
  <StorefrontMenuLinks :menu-id="menuId" :preview="preview" :style="style" />
</template>
