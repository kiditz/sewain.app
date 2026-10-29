<script setup lang="ts">
import type { StorefrontMenuItem } from "~/types/storefront";
import MenuLinkPicker from "../../MenuLinkPicker.vue";
import { useStorefront } from "../../../composables/useStorefront";
import { blockLinkUrl, savedBlockLinks, type BlockLinkDestination } from "../../../utils/block-link";
const props = defineProps<{ settings: Record<string, unknown>; fieldKey: string; label: string }>();
const emit = defineEmits<{ update: [key: string, value: unknown] }>();
const { pages } = useStorefront();
const id = `block-link-${useId()}`;
const url = computed(() => String(props.settings[props.fieldKey] ?? ""));
const item = computed<StorefrontMenuItem>(() => {
  const saved = savedBlockLinks(props.settings.linkTargets)[props.fieldKey];
  return saved?.url === url.value
    ? { id, pageId: saved.pageId, target: saved.target, url: url.value }
    : { id, url: url.value };
});
function select(destination: BlockLinkDestination) {
  const selectedUrl = blockLinkUrl(destination, pages.value);
  if (!selectedUrl) return;
  emit("update", props.fieldKey, selectedUrl);
  emit("update", "linkTargets", { ...savedBlockLinks(props.settings.linkTargets), [props.fieldKey]: { ...destination, url: selectedUrl } });
}
function clear() {
  const targets = { ...savedBlockLinks(props.settings.linkTargets) };
  delete targets[props.fieldKey];
  emit("update", props.fieldKey, "");
  emit("update", "linkTargets", targets);
}
</script>
<template>
  <div class="block-link-field">
    <MenuLinkPicker :id="id" :item="item" :pages="pages" :label="label" :placeholder="url ? 'Link tersimpan' : 'Pilih link'" include-cart @select="select" />
    <v-btn v-if="url" icon="mdi-close" variant="text" size="small" :aria-label="`Hapus ${label}`" @click="clear" />
  </div>
</template>
<style scoped>
.block-link-field { display: flex; align-items: center; gap: 4px; min-width: 0; width: 100%; }
.block-link-field :deep(.menu-link-picker__trigger) { min-width: 0; flex: 1; }
</style>
