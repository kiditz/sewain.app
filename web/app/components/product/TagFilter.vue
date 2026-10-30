<script setup lang="ts">
import type { Tag } from "~/types/tag";
const model = defineModel<string[]>({ default: () => [] });
const api = useTag();
const { t } = useI18n();
const { apiErrorMessage } = useApiError();
const snackbar = useSnackbarStore();
const search = ref("");
const loading = ref(false);
const cache = ref<Record<string, Tag>>({});
const resultIds = ref<string[]>([]);
const options = computed(() => [...new Set([...model.value, ...resultIds.value])].flatMap(id => cache.value[id] ? [cache.value[id]!] : []));
let timer: ReturnType<typeof setTimeout> | undefined;
let sequence = 0;
let disposed = false;
async function load() {
  const request = ++sequence;
  loading.value = true;
  try {
    const tags = await api.find(search.value.trim() ? { search: search.value.trim() } : {});
    if (disposed || request !== sequence) return;
    for (const tag of tags) cache.value[tag.id] = tag;
    resultIds.value = tags.map(tag => tag.id);
  } catch (error) {
    if (!disposed && request === sequence) snackbar.openSnackbar({ type: "error", message: apiErrorMessage(error, "product.tags.loadFailed") });
  } finally { if (!disposed && request === sequence) loading.value = false; }
}
watch(search, () => {
  ++sequence;
  if (timer) clearTimeout(timer);
  timer = setTimeout(() => { void load(); }, 250);
});
onMounted(() => { void load(); });
onBeforeUnmount(() => { disposed = true; ++sequence; if (timer) clearTimeout(timer); });
</script>
<template>
  <v-autocomplete v-model="model" v-model:search="search" :items="options" item-title="name" item-value="id" :label="t('product.filters.tags')" :placeholder="t('product.filters.tagsPlaceholder')" :hint="t('product.filters.tagsHint')" :loading="loading" persistent-hint multiple chips closable-chips clearable no-filter variant="outlined" density="compact" />
</template>
