<script setup lang="ts">
import type { StorefrontPadding } from "~/types/storefront";

const props = defineProps<{
  hideHeading?: boolean;
  settings: Record<string, unknown>;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const padding = computed(() => props.settings.padding as StorefrontPadding);

function update<K extends keyof StorefrontPadding>(
  key: K,
  value: StorefrontPadding[K],
) {
  emit("update", "padding", {
    ...padding.value,
    [key]: value,
  });
}
</script>

<template>
  <div class="d-flex flex-column">
    <div v-if="!hideHeading" class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.padding") }}
    </div>
    <v-divider v-if="!hideHeading" />
    <div class="d-flex flex-column px-4 py-1 ga-2">
      <div class="editor-row" v-if="padding?.top != null">
        <div class="text-body-medium">{{ $t("ui.top") }}</div>
        <div class="d-flex align-center">
          <v-slider
            :model-value="padding.top"
            min="0"
            max="50"
            step="1"
            width="1em"
            hide-details
            density="compact"
            @update:model-value="update('top', $event)"
          />

          <v-text-field
            :model-value="padding.top"
            type="number"
            min="0"
            max="50"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="1em"
            @update:model-value="update('top', Number($event))"
          />
        </div>
      </div>
      <div class="editor-row" v-if="padding?.bottom != null">
        <div class="text-body-medium">{{ $t("ui.bottom") }}</div>
        <div class="d-flex align-center">
          <v-slider
            :model-value="padding.bottom"
            min="0"
            max="50"
            step="1"
            hide-details
            density="compact"
            @update:model-value="update('bottom', $event)"
          />

          <v-text-field
            :model-value="padding.bottom"
            type="number"
            min="0"
            max="50"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="1em"
            @update:model-value="update('bottom', Number($event))"
          />
        </div>
      </div>
      <div class="editor-row" v-if="padding?.left != null">
        <div class="text-body-medium">{{ $t("ui.left") }}</div>
        <div class="d-flex align-center">
          <v-slider
            :model-value="padding.left"
            min="0"
            max="50"
            step="1"
            hide-details
            density="compact"
            @update:model-value="update('left', $event)"
          />

          <v-text-field
            :model-value="padding.left"
            type="number"
            min="0"
            max="50"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="1em"
            @update:model-value="update('left', Number($event))"
          />
        </div>
      </div>
      <div class="editor-row" v-if="padding?.right != null">
        <div class="text-body-medium">{{ $t("ui.right") }}</div>
        <div class="d-flex align-center">
          <v-slider
            :model-value="padding.right"
            min="0"
            max="50"
            step="1"
            hide-details
            density="compact"
            @update:model-value="update('right', $event)"
          />

          <v-text-field
            :model-value="padding.right"
            type="number"
            min="0"
            max="50"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="1em"
            @update:model-value="update('right', Number($event))"
          />
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-row {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  grid-template-rows: 1fr;
  align-items: center;
  justify-content: right;
  gap: 12px;
}
.editor-row :deep(.v-btn-toggle) {
  min-width: 0;
  width: 100%;
  overflow: hidden;
}

.editor-row :deep(.v-btn) {
  min-width: 0;
  flex: 1 1 0;
  font-size: small;
}
</style>
