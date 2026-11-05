<script setup lang="ts">
import type { Product } from "~/types/product";
import { useStorefrontCatalog } from "../composables/useStorefrontCatalog";
const model = defineModel<string[]>({ default: () => [] });
const props=withDefaults(defineProps<{label:string;multiple?:boolean}>(),{multiple:true});
const selection=computed<string[]|string|null>({
  get:()=>props.multiple?model.value:model.value.at(-1)??null,
  set:value=>{model.value=Array.isArray(value)?value:value?[value]:[];},
});
const catalog = useStorefrontCatalog();
const { t } = useI18n();
const cache = ref<Record<string, Product>>({});
const resultIds = ref<string[]>([]);
const search = ref("");
const page = ref(0);
const total = ref(0);
const loading = ref(false);
const error = ref(false);
let sequence = 0;
let disposed = false;
let timer: ReturnType<typeof setTimeout> | undefined;
const options = computed(() => [...new Set([...model.value, ...resultIds.value])]
  .flatMap(id => cache.value[id] ? [{ title: cache.value[id]!.name, value: id }] : []));
function remember(products: Product[]) { for (const product of products) cache.value[product.id] = product; }
async function fetchPage(nextPage = 0) {
  const request = ++sequence;
  loading.value = true;
  error.value = false;
  try {
    const result = await catalog.fetchProducts({ page: nextPage, size: 20, keyword: search.value.trim() || undefined });
    if (disposed || request !== sequence) return;
    remember(result.items);
    resultIds.value = nextPage ? [...new Set([...resultIds.value, ...result.items.map(item => item.id)])] : result.items.map(item => item.id);
    total.value = result.totalItems;
    page.value = nextPage;
  } catch { if (!disposed && request === sequence) error.value = true; }
  finally { if (!disposed && request === sequence) loading.value = false; }
}
async function loadSelected(ids: string[]) {
  const missing = ids.filter(id => !cache.value[id]);
  try {
    for (let offset = 0; offset < missing.length; offset += 100) {
      const batch = missing.slice(offset, offset + 100);
      const response = await catalog.fetchProducts({ id: batch.join(","), page: 0, size: batch.length });
      if (disposed) return;
      remember(response.items);
    }
  } catch { if (!disposed) error.value = true; }
}
watch(model, ids => { void loadSelected(ids); }, { immediate: true, deep: true });
watch(search, () => {
  ++sequence;
  if (timer) clearTimeout(timer);
  loading.value = true;
  timer = setTimeout(() => { void fetchPage(); }, 250);
});
onMounted(() => { void fetchPage(); });
onBeforeUnmount(() => { disposed = true; ++sequence; if (timer) clearTimeout(timer); });
</script>
<template>
  <v-autocomplete v-model="selection" v-model:search="search" :items="options" :label="label" :aria-label="label" :loading="loading"
    :error-messages="error ? [t('productGrid.loadFailed')] : []" :multiple="multiple" :clearable="!multiple" :chips="multiple" :closable-chips="multiple" no-filter hide-details="auto" variant="outlined" density="compact">
    <template #append-item>
      <v-list-item v-if="resultIds.length < total">
        <v-btn variant="text" :loading="loading" :disabled="loading" @click.stop="fetchPage(page + 1)">{{ t('ui.load_more') }}</v-btn>
      </v-list-item>
    </template>
  </v-autocomplete>
</template>
