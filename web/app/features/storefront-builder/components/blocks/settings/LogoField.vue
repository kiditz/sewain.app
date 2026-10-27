<script setup lang="ts">
import type { Media } from "~/types/media";
const props = defineProps<{ settings: Record<string, unknown> }>();
const emit = defineEmits<{ update: [key: string, value: unknown] }>();
const showMediaDialog = ref(false);
const { current: merchant } = storeToRefs(useMerchantStore());
const src = computed(() => String(props.settings.src || merchant.value?.storeInformation?.logoUrl || ""));
function selectMedia(value: Media | Media[]) {
  const media = Array.isArray(value) ? value[0] : value;
  if (!media) return;
  emit("update", "src", media.url);
  showMediaDialog.value = false;
}
</script>
<template>
  <media-dialog v-model="showMediaDialog" @selected="selectMedia" />
  <div class="pa-4">
    <v-img v-if="src" :src="src" height="100" :cover="false" class="mb-3" alt="Logo" />
    <v-btn block variant="outlined" @click="showMediaDialog = true">{{ src ? $t('common.change') : $t('ui.add_media') }}</v-btn>
  </div>
</template>
