```vue
<script setup lang="ts">
import type { Media } from "~/types/theme";
import type { Media as MediaD } from "~/types/media";
import {
  DeleteOutlined,
  FileImageTwoTone,
  PlusOutlined,
} from "@ant-design/icons-vue";

const props = defineProps<{
  settings: Record<string, unknown>;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const medias = computed<Media[]>(() => {
  const value = props.settings.medias;
  return Array.isArray(value) ? (value as Media[]) : [];
});

const displayMedias = computed<Media[]>(() =>
  medias.value.length > 0 ? medias.value : [{ src: "", alt: "" }],
);

const selectedMediaIndex = ref<number | null>(null);
const showMedia = ref(false);

function addMedia() {
  emit("update", "medias", [...medias.value, { src: "", alt: "" }]);
}

function removeMedia(index: number) {
  emit(
    "update",
    "medias",
    medias.value.filter((_, i) => i !== index),
  );
}

function onSelected(media: MediaD | MediaD[]) {
  const selected = Array.isArray(media) ? media[0] : media;
  const index = selectedMediaIndex.value;

  if (!selected || index === null) return;

  const updatedMedias = [...displayMedias.value];

  updatedMedias[index] = {
    ...updatedMedias[index],
    src: selected.url,
    alt: selected.filename ?? "",
  };

  emit("update", "medias", updatedMedias);

  selectedMediaIndex.value = null;
  showMedia.value = false;
}

function openMediaDialog(index: number) {
  selectedMediaIndex.value = index;
  showMedia.value = true;
}
</script>

<template>
  <media-dialog v-model="showMedia" @selected="onSelected" />

  <div class="d-flex flex-column">
    <div class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.media") }}
    </div>
    <v-divider />

    <div
      v-for="(media, index) in displayMedias"
      :key="index"
      class="d-flex flex-column pa-4"
    >
      <div class="d-flex align-center justify-space-between ga-4 mb-2">
        <div class="text-body-medium font-weight-medium">
          {{ $t("ui.media") }} {{ index + 1 }}
        </div>

        <v-btn
          variant="tonal"
          density="compact"
          color="error"
          @click="removeMedia(index)"
        >
          <delete-outlined style="font-size: 12px" />
        </v-btn>
      </div>

      <div class="media-item__preview">
        <v-img v-if="media.src" :src="media.src" :alt="media.alt" cover rounded>
          <div class="media-item__overlay">
            <v-btn
              variant="text"
              color="white"
              density="compact"
              prepend-icon="mdi-image-outline"
              @click.stop="openMediaDialog(index)"
            >
              {{ $t("common.change") }}
            </v-btn>
          </div>
        </v-img>

        <button
          v-else
          type="button"
          class="media-item__empty"
          @click="openMediaDialog(index)"
        >
          <file-image-two-tone style="font-size: 28px" />
          <span class="text-body-small">{{ $t("ui.add_media") }}</span>
        </button>
      </div>
    </div>

    <div class="px-4">
      <v-btn
        block
        variant="outlined"
        :prepend-icon="PlusOutlined"
        @click="addMedia"
      >
        {{ $t("ui.add_media") }}
      </v-btn>
    </div>
    <v-divider class="mt-4"/>
  </div>
</template>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.media-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(var(--v-theme-on-surface), 0.06);
}

.media-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.media-item__preview {
  width: 100%;
  height: 120px;
  overflow: hidden;
  border-radius: 8px;
  background: rgba(var(--v-theme-on-surface), 0.04);
}

.media-item__preview :deep(.v-img) {
  width: 100%;
  height: 100%;
}

.media-item__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
}

.media-item__empty {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed rgba(var(--v-theme-on-surface), 0.3);
  border-radius: 8px;
  background: transparent;
  color: rgba(var(--v-theme-on-surface), 0.6);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background-color 0.2s,
    color 0.2s;
}

.media-item__empty:hover {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.04);
  color: rgb(var(--v-theme-primary));
}
</style>
```
