<script setup lang="ts">
import type { Media } from "~/types/media";

const props = defineProps<{
  settings: Record<string, unknown>;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const showMediaDialog = ref(false);
const src = computed(() => String(props.settings.src ?? ""));

function selectMedia(value: Media | Media[]) {
  const media = Array.isArray(value) ? value[0] : value;
  if (!media) return;

  emit("update", "src", media.url);
  showMediaDialog.value = false;
}
</script>

<template>
  <media-dialog v-model="showMediaDialog" @selected="selectMedia" />

  <div class="image-field">
    <div class="text-title-medium font-weight-bold px-4 py-2">
      {{ $t("ui.media") }}
    </div>
    <v-divider />
    <div class="pa-4">
      <v-img
        v-if="src"
        :src="src"
        height="140"
        cover
        class="image-field__preview"
      >
        <div class="image-field__overlay">
          <v-btn size="small" variant="tonal" @click.stop="showMediaDialog = true">
            {{ $t("common.change") }}
          </v-btn>
        </div>
      </v-img>
      <v-btn
        v-else
        block
        height="140"
        variant="outlined"
        class="image-field__empty"
        @click="showMediaDialog = true"
      >
        {{ $t("ui.add_media") }}
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.image-field__preview {
  border-radius: 8px;
}

.image-field__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.28);
}

.image-field__empty {
  border-style: dashed;
}
</style>
