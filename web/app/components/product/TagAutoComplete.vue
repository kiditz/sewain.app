<script setup lang="ts">
import type { Tag } from "~/types/tag";

const model = defineModel<string[]>({ default: () => [] });
const props = withDefaults(defineProps<{ disabled?: boolean; initialItems?: Tag[] }>(), {
  disabled: false, initialItems: () => [],
});
const emit = defineEmits<{ created: [tag: Tag]; updated: [tag: Tag]; deleted: [id: string] }>();
const { t } = useI18n();
const { apiErrorMessage } = useApiError();
const api = useTag();
const cache = ref<Record<string, Tag>>({});
const resultIds = ref<string[]>([]);
const search = ref("");
const menuOpen = ref(false);
const loading = ref(false);
const saving = ref(false);
const deleting = ref(false);
const error = ref("");
const dialogError = ref("");
const dialog = ref(false);
const deleteDialog = ref(false);
const editing = ref<Tag | null>(null);
const deletingTag = ref<Tag | null>(null);
const name = ref("");
const submitted = ref(false);
const options = computed(() => [...new Set([...model.value, ...resultIds.value])]
  .map(id => cache.value[id]).filter((tag): tag is Tag => !!tag));
const normalized = (value: string) => value.trim().toLocaleLowerCase();
const canCreate = computed(() => !!search.value.trim() && !loading.value && !saving.value && !deleting.value && !props.disabled
  && !options.value.some(tag => normalized(tag.name) === normalized(search.value) || normalized(tag.slug) === normalized(search.value)));
const nameError = computed(() => !name.value.trim() ? t("product.tags.nameRequired")
  : name.value.trim().length > 100 ? t("product.tags.nameTooLong") : "");
let timer: ReturnType<typeof setTimeout> | undefined;
let requestId = 0;
let mutationVersion = 0;
let disposed = false;
function remember(tags: Tag[]) { for (const tag of tags) cache.value[tag.id] = tag; }

async function fetchItems(keyword = "") {
  const version = ++requestId;
  loading.value = true;
  error.value = "";
  try {
    const tags = await api.find(keyword ? { search: keyword } : {});
    if (disposed || version !== requestId) return;
    remember(tags);
    resultIds.value = tags.map(tag => tag.id);
  } catch (cause) {
    if (!disposed && version === requestId) error.value = t(apiErrorMessage(cause, "product.tags.loadFailed"));
  } finally { if (!disposed && version === requestId) loading.value = false; }
}
async function loadSelected(ids: string[]) {
  const missing = [...new Set(ids)].filter(id => !cache.value[id]);
  const version = mutationVersion;
  const results = await Promise.allSettled(missing.map(id => api.findById(id)));
  if (disposed || version !== mutationVersion) return;
  remember(results.flatMap(result => result.status === "fulfilled" ? [result.value] : []));
  if (results.some(result => result.status === "rejected")) error.value = t("product.tags.loadSelectedFailed");
}
function beginEdit(tag: Tag) {
  menuOpen.value = false;
  editing.value = tag;
  name.value = tag.name;
  submitted.value = false;
  dialogError.value = "";
  dialog.value = true;
}
function beginDelete(tag: Tag) {
  menuOpen.value = false;
  deletingTag.value = tag;
  dialogError.value = "";
  deleteDialog.value = true;
}
function invalidateSearch() {
  ++requestId;
  ++mutationVersion;
  if (timer) clearTimeout(timer);
  loading.value = false;
}
async function createTag() {
  const tagName = search.value.trim();
  if (!canCreate.value) return;
  if (tagName.length > 100) { error.value = t("product.tags.nameTooLong"); return; }
  saving.value = true;
  menuOpen.value = false;
  error.value = "";
  invalidateSearch();
  try {
    const tag = await api.create({ name: tagName });
    remember([tag]);
    if (!resultIds.value.includes(tag.id)) resultIds.value.unshift(tag.id);
    model.value = [...new Set([...model.value, tag.id])];
    emit("created", tag);
    search.value = "";
  } catch (cause) { error.value = t(apiErrorMessage(cause, "product.tags.saveFailed")); }
  finally { saving.value = false; }
}
async function saveTag() {
  submitted.value = true;
  if (!editing.value || saving.value || props.disabled || nameError.value) return;
  saving.value = true;
  dialogError.value = "";
  invalidateSearch();
  try {
    const tag = await api.update(editing.value.id, { name: name.value.trim() });
    remember([tag]);
    if (!resultIds.value.includes(tag.id)) resultIds.value.unshift(tag.id);
    emit("updated", tag);
    dialog.value = false;
    search.value = "";
  } catch (cause) { dialogError.value = t(apiErrorMessage(cause, "product.tags.saveFailed")); }
  finally { saving.value = false; }
}
async function deleteTag() {
  if (!deletingTag.value || deleting.value || props.disabled) return;
  deleting.value = true;
  dialogError.value = "";
  invalidateSearch();
  try {
    const id = deletingTag.value.id;
    await api.remove(id);
    delete cache.value[id];
    resultIds.value = resultIds.value.filter(value => value !== id);
    model.value = model.value.filter(value => value !== id);
    emit("deleted", id);
    deleteDialog.value = false;
    void fetchItems(search.value.trim());
  } catch (cause) {
    error.value = t(apiErrorMessage(cause, "product.tags.deleteFailed"));
    deleteDialog.value = false;
  }
  finally { deleting.value = false; }
}
watch(() => props.initialItems, tags => remember(tags), { immediate: true });
watch(model, ids => { void loadSelected(ids); }, { immediate: true, deep: true });
watch(search, value => {
  if (timer) clearTimeout(timer);
  ++requestId; // invalidate older searches as soon as the input changes
  if (!props.disabled) {
    loading.value = true;
    timer = setTimeout(() => { void fetchItems(value.trim()); }, 250);
  }
});
watch(() => props.disabled, disabled => {
  if (timer) clearTimeout(timer);
  if (disabled) { ++requestId; loading.value = false; }
  else void fetchItems(search.value.trim());
});
onMounted(() => { void fetchItems(); });
onBeforeUnmount(() => { disposed = true; ++requestId; if (timer) clearTimeout(timer); });
</script>

<template>
  <div class="product-tags-autocomplete">
    <v-autocomplete
      v-model="model"
      v-model:search="search"
      v-model:menu="menuOpen"
      :items="options"
      item-title="name"
      item-value="id"
      :label="t('product.tags.label')"
      :placeholder="t('product.tags.searchPlaceholder')"
      :loading="loading || saving"
      :disabled="disabled || saving || deleting"
      :error-messages="error ? [error] : []"
      multiple chips closable-chips clearable no-filter
      variant="outlined" density="compact" hide-details="auto"
    >
      <template #item="{ props: itemProps, item }">
        <v-list-item v-bind="itemProps" :title="item.name">
          <template #append>
            <v-btn icon="mdi-pencil-outline" size="small" variant="text" :aria-label="t('product.tags.editNamed', { name: item.name })" @click.stop.prevent="beginEdit(item)" />
            <v-btn icon="mdi-delete-outline" size="small" variant="text" color="error" :aria-label="t('product.tags.deleteNamed', { name: item.name })" @click.stop.prevent="beginDelete(item)" />
          </template>
        </v-list-item>
      </template>
      <template #no-data>
        <v-list-item v-if="!canCreate" :title="loading ? t('product.tags.loading') : t('product.tags.noResults')" />
        <v-list-item v-else>
          <v-btn variant="text" color="primary" prepend-icon="mdi-plus" @click.stop="createTag">{{ t('product.tags.createNamed', { name: search.trim() }) }}</v-btn>
        </v-list-item>
      </template>
      <template #append-item>
        <v-list-item v-if="options.length && canCreate">
          <v-btn variant="text" color="primary" prepend-icon="mdi-plus" @click.stop="createTag">{{ t('product.tags.createNamed', { name: search.trim() }) }}</v-btn>
        </v-list-item>
      </template>
    </v-autocomplete>

    <v-dialog v-model="dialog" max-width="480" :persistent="saving">
      <v-card :title="t('product.tags.edit')">
        <v-form @submit.stop.prevent="saveTag">
          <v-card-text>
            <v-alert v-if="dialogError" type="error" class="mb-4">{{ dialogError }}</v-alert>
            <v-text-field v-model="name" :label="t('product.tags.name')" :error-messages="submitted && nameError ? [nameError] : []" :disabled="saving" maxlength="100" counter autofocus variant="outlined" />
          </v-card-text>
          <v-card-actions>
            <v-spacer />
            <v-btn :disabled="saving" @click="dialog = false">{{ t('common.cancel') }}</v-btn>
            <v-btn type="submit" color="primary" :loading="saving">{{ t('common.save') }}</v-btn>
          </v-card-actions>
        </v-form>
      </v-card>
    </v-dialog>
    <ConfirmDeleteDialog
      v-model="deleteDialog"
      :title="t('product.tags.delete')"
      :message="t('product.tags.deleteConfirm', { name: deletingTag?.name })"
      @confirmed="deleteTag"
    />
  </div>
</template>
