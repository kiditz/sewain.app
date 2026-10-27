<script setup lang="ts">
import { computed } from "vue";
import type {
  TextAlignment,
  TextLayout,
  TextMaxWidth,
  TextWidth,
} from "~/types/storefront";

const props = defineProps<{
  settings: Record<string, unknown>;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const layout = computed<TextLayout>(() => {
  const value = props.settings.layout as TextLayout;
  return { ...value, alignment: value.alignment ?? "left" };
});

function updateLayout<K extends keyof TextLayout>(
  key: K,
  value: TextLayout[K],
) {
  emit("update", "layout", { ...layout.value, [key]: value });
}
</script>

<template>
  <div class="d-flex flex-column">
    <div class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.layout") }}
    </div>
    <v-divider />
    <div class="d-flex flex-column px-4 py-4 ga-2">
      <div class="editor-row">
        <div class="text-body-medium">{{ $t("ui.width") }}</div>
        <v-btn-toggle
          :model-value="layout.width"
          mandatory
          divided
          variant="outlined"
          density="compact"
          @update:model-value="updateLayout('width', $event as TextWidth)"
        >
          <v-btn value="match">{{ $t("ui.match") }}</v-btn>
          <v-btn value="fill">{{ $t("ui.fill") }}</v-btn>
        </v-btn-toggle>
      </div>

      <div class="editor-row">
        <div class="text-body-medium">{{ $t("ui.alignment") }}</div>
        <v-btn-toggle
          :model-value="layout.alignment"
          mandatory
          divided
          variant="outlined"
          density="compact"
          @update:model-value="updateLayout('alignment', $event as TextAlignment)"
        >
          <v-btn value="left" :aria-label="$t('ui.left')">
            <v-icon>mdi-format-align-left</v-icon>
          </v-btn>
          <v-btn value="center" :aria-label="$t('ui.center')">
            <v-icon>mdi-format-align-center</v-icon>
          </v-btn>
          <v-btn value="right" :aria-label="$t('ui.right')">
            <v-icon>mdi-format-align-right</v-icon>
          </v-btn>
        </v-btn-toggle>
      </div>

      <div class="editor-row">
        <div class="text-body-medium">{{ $t("ui.max_width") }}</div>
        <v-btn-toggle
          :model-value="layout.maxWidth"
          mandatory
          divided
          variant="outlined"
          density="compact"
          @update:model-value="updateLayout('maxWidth', $event as TextMaxWidth)"
        >
          <v-btn value="narrow">{{ $t("ui.narrow") }}</v-btn>
          <v-btn value="normal">{{ $t("ui.normal") }}</v-btn>
          <v-btn value="none">{{ $t("ui.none") }}</v-btn>
        </v-btn-toggle>
      </div>
    </div>
    <v-divider />
  </div>
</template>

<style scoped>
.editor-row {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  align-items: center;
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
