<script setup lang="ts">
import type { StorefrontAppearance } from "~/types/storefront";

const props = defineProps<{
  settings: Record<string, unknown>;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const appearance = computed(
  () => props.settings.appearance as StorefrontAppearance,
);

function updateSetting(key: string, value: unknown) {
  emit("update", "appearance", {
    ...appearance.value,
    [key]: value,
  });
}
</script>

<template>
  <div class="d-flex flex-column py-2">
    <div class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.appearance") }}
    </div>
    <v-divider />
    <div class="d-flex flex-column px-4 py-4 ga-4">
      <div v-if="appearance.backgroundColor != null" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.background_color") }}</div>
        <v-color-input
          :model-value="String(appearance.backgroundColor)"
          color-format="hexa"
          variant="outlined"
          density="compact"
          hide-details
          @update:model-value="updateSetting('backgroundColor', $event)"
        />
      </div>

      <div v-if="appearance.overlayColor != null" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.overlay_color") }}</div>
        <v-color-input
          :model-value="String(appearance.overlayColor)"
          color-format="hexa"
          variant="outlined"
          density="compact"
          hide-details
          @update:model-value="updateSetting('overlayColor', $event)"
        />
      </div>

      <div v-if="appearance.overlayOpacity != null" class="editor-row">
        <div class="text-body-medium">{{ $t("ui.overlay_opacity") }}</div>
        <div class="d-flex align-center ga-3">
          <v-slider
            :model-value="Number(appearance.overlayOpacity)"
            min="0"
            max="100"
            step="1"
            hide-details
            density="compact"
            @update:model-value="
              updateSetting('overlayOpacity', Number($event))
            "
          />
          <v-text-field
            :model-value="Number(appearance.overlayOpacity)"
            type="number"
            min="0"
            max="100"
            suffix="%"
            hide-details
            density="compact"
            variant="outlined"
            width="1em"
            @update:model-value="
              updateSetting('overlayOpacity', Number($event))
            "
          />
        </div>
      </div>
    </div>
    <v-divider />
  </div>
</template>
