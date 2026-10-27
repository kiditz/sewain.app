<script setup lang="ts">
import type { SectionLayout } from "~/types/storefront";

const props = defineProps<{
  settings: Record<string, unknown>;
}>();

const layout = computed(() => props.settings.layout as SectionLayout);

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

function updateLayout<K extends keyof SectionLayout>(
  key: K,
  value: SectionLayout[K],
) {
  emit("update", "layout", {
    ...layout.value,
    [key]: value,
  });
}

function hasLayoutValue(key: keyof SectionLayout): boolean {
  return (
    Object.hasOwn(layout.value, key) &&
    layout.value[key] != null
  );
}

const hasRenderableLayout = computed(
  () =>
    Object.keys(layout.value).some((key) =>
      hasLayoutValue(key as keyof SectionLayout),
    ),
);
</script>

<template>
  <div v-if="hasRenderableLayout" class="d-flex flex-column">
    <div class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.layout") }}
    </div>
    <v-divider />
    <div class="d-flex flex-column px-4 py-4 ga-2">
      <div v-if="hasLayoutValue('direction')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.direction") }}</div>

        <v-btn-toggle
          :model-value="layout.direction"
          mandatory
          divided
          variant="outlined"
          density="compact"
          @update:model-value="updateLayout('direction', $event)"
        >
          <v-btn value="vertical">{{ $t("ui.vertical") }}</v-btn>
          <v-btn value="horizontal">{{ $t("ui.horizontal") }}</v-btn>
        </v-btn-toggle>
      </div>

      <div v-if="hasLayoutValue('alignTextBaseline')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.align_text_baseline") }}</div>

        <v-switch
          class="ml-auto"
          :model-value="layout.alignTextBaseline"
          hide-details
          variant="outlined"
          color="primary"
          density="compact"
          @update:model-value="updateLayout('alignTextBaseline', $event!)"
        />
      </div>

      <div v-if="hasLayoutValue('alignment')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.alignment") }}</div>

        <v-btn-toggle
          v-if="layout.direction == 'vertical'"
          :model-value="layout.alignment"
          mandatory
          divided
          variant="outlined"
          density="compact"
          @update:model-value="updateLayout('alignment', $event)"
        >
          <v-btn value="left">{{ $t("ui.left") }}</v-btn>
          <v-btn value="center">{{ $t("ui.center") }}</v-btn>
          <v-btn value="right">{{ $t("ui.right") }}</v-btn>
        </v-btn-toggle>
        <v-select
          v-else
          :model-value="layout.alignment"
          variant="outlined"
          density="compact"
          :items="[
            { title: $t('ui.left'), value: 'left' },
            { title: $t('ui.center'), value: 'center' },
            { title: $t('ui.right'), value: 'right' },
            { title: $t('ui.space_between'), value: 'space-between' },
            { title: $t('ui.space_around'), value: 'space-around' },
            { title: $t('ui.space_evenly'), value: 'space-evenly' },
          ]"
          hide-details
          @update:model-value="updateLayout('alignment', $event)"
        />
      </div>

      <div v-if="hasLayoutValue('position')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.position") }}</div>

        <v-btn-toggle
          :model-value="layout.position"
          mandatory
          divided
          variant="outlined"
          density="compact"
          @update:model-value="updateLayout('position', $event)"
        >
          <v-btn value="top">{{ $t("ui.top") }}</v-btn>
          <v-btn value="center">{{ $t("ui.center") }}</v-btn>
          <v-btn value="bottom">{{ $t("ui.bottom") }}</v-btn>
        </v-btn-toggle>
      </div>

      <div v-if="hasLayoutValue('gap')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.gap") }}</div>
        <div class="d-flex align-center ga-2">
          <v-slider
            :model-value="layout.gap"
            min="10"
            max="100"
            step="5"
            hide-details
            density="compact"
            @update:model-value="updateLayout('gap', $event)"
          />
          <v-text-field
            :model-value="layout.gap"
            type="number"
            min="0"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="20"
            @update:model-value="updateLayout('gap', Number($event))"
          />
        </div>
      </div>

      <div v-if="hasLayoutValue('width')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.width") }}</div>

        <v-btn-toggle
          :model-value="layout.width"
          mandatory
          divided
          density="compact"
          variant="outlined"
          @update:model-value="updateLayout('width', $event)"
        >
          <v-btn value="page">{{ $t("ui.page") }}</v-btn>
          <v-btn value="full">{{ $t("ui.full") }}</v-btn>
        </v-btn-toggle>
      </div>

      <div v-if="hasLayoutValue('height')" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.height") }}</div>

        <v-select
          :model-value="layout.height"
          :items="[
            { title: $t('ui.auto'), value: 'auto' },
            { title: $t('ui.small'), value: 'small' },
            { title: $t('ui.medium'), value: 'medium' },
            { title: $t('ui.large'), value: 'large' },
            { title: $t('ui.custom'), value: 'custom' },
          ]"
          variant="outlined"
          density="compact"
          hide-details
          @update:model-value="updateLayout('height', $event)"
        />
      </div>

      <div
        v-if="layout.height === 'custom' && hasLayoutValue('customHeight')"
        class="editor-row"
      >
        <div class="text-body-medium">{{ $t("ui.custom_height") }}</div>

        <div class="d-flex align-center ga-3">
          <v-slider
            :model-value="layout.customHeight"
            min="10"
            max="1000"
            step="5"
            hide-details
            density="compact"
            @update:model-value="updateLayout('customHeight', $event)"
          />

          <v-text-field
            :model-value="layout.customHeight"
            type="number"
            min="0"
            max="100"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="20"
            @update:model-value="updateLayout('customHeight', Number($event))"
          />
        </div>
      </div>
    </div>
    <v-divider />
  </div>
</template>
<style scoped>
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
