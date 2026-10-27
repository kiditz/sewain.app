<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";

defineProps<{
  block: StorefrontBlock;
  blockLabel: string;
}>();

const emit = defineEmits<{
  delete: [];
  touch: [];
}>();
</script>

<template>
  <div class="d-flex justify-space-between align-center px-4 py-2">
    <div class="d-flex flex-column">
      <strong>{{ blockLabel }}</strong>
      <small>{{ $t("ui.block_settings") }}</small>
    </div>

    <v-btn
      icon="mdi-trash-can-outline"
      color="error"
      variant="text"
      size="small"
      :aria-label="$t('ui.delete_block')"
      @click="emit('delete')"
    />
  </div>
  <v-divider />
  <div class="d-flex align-center px-4">
    <div class="text-body-medium">{{ $t("ui.show_block") }}</div>
    <v-switch
      v-model="block.enabled"
      class="ml-auto"
      color="primary"
      hide-details
      @update:model-value="emit('touch')"
    />
  </div>
  <v-divider />
</template>
<style scoped>
.sidebar-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.sidebar-title > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-title strong {
  font-size: 14px;
}

.sidebar-title small {
  font-size: 12px;
  color: #858b94;
}
</style>
