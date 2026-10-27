<script setup lang="ts">
import type { StorefrontSection } from "~/types/storefront";

defineProps<{
  section: StorefrontSection;
  sectionLabel: string;
}>();

const emit = defineEmits<{
  delete: [];
  touch: [];
}>();
</script>

<template>
  <div class="d-flex justify-space-between align-center px-4 py-2">
    <div class="d-flex flex-column">
      <strong>{{ sectionLabel }}</strong>
      <small class="text-body-small text-medium-emphasis">{{ $t("ui.section_settings") }}</small>
    </div>

    <v-btn
      v-if="section.type !== 'HEADER'"
      icon="mdi-trash-can-outline"
      color="error"
      variant="text"
      size="small"
      :aria-label="$t('ui.delete_section')"
      @click="emit('delete')"
    />
  </div>
  <v-divider />

  <div class="d-flex align-center px-4">
    <div class="text-body-medium">{{ $t("ui.show_section") }}</div>
    <v-switch
      v-model="section.enabled"
      class="ml-auto"
      color="primary"
      hide-details
      @update:model-value="emit('touch')"
    />
  </div>
  <v-divider />
</template>
