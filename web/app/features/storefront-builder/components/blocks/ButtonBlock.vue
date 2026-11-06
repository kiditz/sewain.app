<script setup lang="ts">
import {safeNavigationUrl} from "~/utils/content-security";
import BlockLabel from "./BlockLabel.vue";
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
    :href="safeNavigationUrl(settings.url) || undefined"
    ><BlockLabel :value="settings.label" /> <span aria-hidden="true">↗</span></v-btn
  >
</template>

<style scoped>
.block-button.v-btn { height: auto; min-height: 36px; white-space: normal; }
.block-button {
  gap: 12px;
  align-items: center;
  cursor: pointer;
  border-radius: var(--store-radius);
  background: var(--store-primary);
  color: var(--store-on-primary, #fff);
  text-decoration: none;
  font-weight: 600;
}
</style>
