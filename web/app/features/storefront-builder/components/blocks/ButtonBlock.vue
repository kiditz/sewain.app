<script setup lang="ts">
import { getBlockSettings } from "../../utils/block-settings";
import type { StorefrontBlock } from "~/types/storefront";

const props = defineProps<{ block: StorefrontBlock }>();

const settings = computed(() =>
  getBlockSettings<{
    label?: string;
    url?: string;
    style?: string;
  }>(props.block),
);
</script>

<template>
  <v-btn
    class="block-button"
    variant="outlined"
    :class="`button-${settings.style || 'primary'}`"
    :href="String(settings.url || '#')"
    >{{ settings.label }} <span aria-hidden="true">↗</span></v-btn
  >
</template>

<style scoped>
.block-button {
  gap: 12px;
  align-items: center;

  border-radius: var(--store-radius);
  background: var(--store-primary);
  color: #fff;
  text-decoration: none;
  font-weight: 600;
}
</style>
