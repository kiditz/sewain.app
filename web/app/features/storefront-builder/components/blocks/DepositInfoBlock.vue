<script setup lang="ts">
import BlockLabel from "./BlockLabel.vue";
import { storeToRefs } from "pinia";
import { getBlockSettings } from "../../utils/block-settings";
import type { StorefrontBlock } from "~/types/storefront";
import { useStorefrontProductStore } from "~/stores/storefront-product";

const props = defineProps<{
  block: StorefrontBlock;
}>();

const settings = computed(() =>
  getBlockSettings<{
    label?: string;
  }>(props.block),
);

const store = useStorefrontProductStore();
const { depositSubtotal } = storeToRefs(store);


</script>

<template>
  <div class="deposit-info">
    <BlockLabel :value="settings.label || 'Refundable deposit'" />
    <strong>{{ money(depositSubtotal) }}</strong>
  </div>
</template>

<style scoped>
.deposit-info {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: var(--store-secondary);
  border-radius: var(--store-radius);
  font-size: 13px;
}
</style>