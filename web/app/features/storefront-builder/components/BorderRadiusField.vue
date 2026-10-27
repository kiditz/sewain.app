<script setup lang="ts">
import type { StorefrontBorderRadius } from "~/types/storefront";

const props = defineProps<{
  settings: Record<string, unknown>;
  defaultValue?: number;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const borderRadius = ref<StorefrontBorderRadius>({});
watch(
  () => props.settings.borderRadius,
  (value) => {
    borderRadius.value = { ...(value as StorefrontBorderRadius | undefined) };
  },
  { deep: true, immediate: true },
);
const mode = ref<"all" | "one">("all");
const corners: Array<{
  key: keyof StorefrontBorderRadius;
  label: string;
}> = [
  { key: "topLeft", label: "ui.top_left" },
  { key: "topRight", label: "ui.top_right" },
  { key: "bottomRight", label: "ui.bottom_right" },
  { key: "bottomLeft", label: "ui.bottom_left" },
];
const fallback = computed(() => props.defaultValue ?? 0);
const allValue = computed(
  () =>
    borderRadius.value?.topLeft ??
    borderRadius.value?.topRight ??
    borderRadius.value?.bottomRight ??
    borderRadius.value?.bottomLeft ??
    fallback.value,
);

function updateAll(value: number) {
  const nextValue = {
    ...borderRadius.value,
    topLeft: value,
    topRight: value,
    bottomRight: value,
    bottomLeft: value,
  };
  borderRadius.value = nextValue;
  emit("update", "borderRadius", nextValue);
}

function updateSide(side: keyof StorefrontBorderRadius, value: number) {
  const nextValue = {
    ...borderRadius.value,
    [side]: value,
  };
  borderRadius.value = nextValue;
  emit("update", "borderRadius", nextValue);
}
</script>

<template>
  <div class="d-flex flex-column">
    <div class="d-flex align-center justify-space-between px-4 py-2">
      <div class="text-title-medium font-weight-bold">
        {{ $t("ui.corner_radius") }}
      </div>
      <v-btn-toggle
        v-model="mode"
        mandatory
        divided
        variant="outlined"
        density="compact"
      >
        <v-btn value="all">{{ $t("ui.all") }}</v-btn>
        <v-btn value="one">{{ $t("ui.one") }}</v-btn>
      </v-btn-toggle>
    </div>
    <v-divider />
    <div v-if="mode === 'all'" class="editor-row">
      <div class="text-body-medium">{{ $t("ui.all") }}</div>
      <div class="d-flex align-center">
        <v-slider
          :model-value="allValue"
          min="0"
          max="100"
          step="1"
          hide-details
          density="compact"
          @update:model-value="updateAll(Number($event))"
        />
        <v-text-field
          :model-value="allValue"
          type="number"
          min="0"
          max="100"
          suffix="px"
          hide-details
          density="compact"
          variant="outlined"
          width="1em"
          @update:model-value="updateAll(Number($event))"
        />
      </div>
    </div>
    <div v-else class="d-flex flex-column">
      <div v-for="corner in corners" :key="corner.key" class="editor-row">
        <div class="text-body-medium">{{ $t(corner.label) }}</div>
        <div class="d-flex align-center">
          <v-slider
            :model-value="borderRadius?.[corner.key] ?? fallback"
            min="0"
            max="100"
            step="1"
            hide-details
            density="compact"
            @update:model-value="updateSide(corner.key, Number($event))"
          />
          <v-text-field
            :model-value="borderRadius?.[corner.key] ?? fallback"
            type="number"
            min="0"
            max="100"
            suffix="px"
            hide-details
            density="compact"
            variant="outlined"
            width="1em"
            @update:model-value="updateSide(corner.key, Number($event))"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.editor-row {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 4px 16px;
}

.v-btn-toggle {
  min-width: 0;
  overflow: hidden;
}

.v-btn {
  min-width: 0;
  font-size: small;
}
</style>
