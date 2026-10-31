<script setup lang="ts">
import { computed } from "vue";
import EditorFieldRow from "../../EditorFieldRow.vue";
import { socialPlatforms, socialLinkHref } from "../../../utils/social-links";
const props = defineProps<{ settings: Record<string, unknown> }>();
const emit = defineEmits<{ update: [key: string, value: unknown] }>();
const { t } = useI18n();
const network = computed(() => typeof props.settings.network === "string" ? props.settings.network : "instagram");
const url = computed(() => typeof props.settings.url === "string" ? props.settings.url : "");
const label = computed(() => typeof props.settings.label === "string" ? props.settings.label : "");
const urlErrors = computed<string[]>(() => url.value.trim() && !socialLinkHref(url.value) ? [t("storefrontFooter.invalidUrl")] : []);
function selectNetwork(value: unknown) {
  if (typeof value === "string" && socialPlatforms.some(platform => platform.value === value)) emit("update", "network", value);
}
function updateText(key: "url" | "label", value: unknown) {
  emit("update", key, typeof value === "string" ? value : "");
}
</script>
<template>
  <div class="social-link-fields">
    <EditorFieldRow :label="t('storefrontFooter.platform')">
      <v-select :model-value="network" :items="socialPlatforms" item-title="title" item-value="value" variant="outlined" density="compact" hide-details :aria-label="t('storefrontFooter.platform')" @update:model-value="selectNetwork" />
    </EditorFieldRow>
    <EditorFieldRow :label="t('storefrontFooter.url')">
      <v-text-field :model-value="url" placeholder="https://instagram.com/yourstore" variant="outlined" density="compact" clearable :error-messages="urlErrors" :aria-label="t('storefrontFooter.url')" @update:model-value="updateText('url', $event)" />
    </EditorFieldRow>
    <EditorFieldRow :label="t('storefrontFooter.label')">
      <v-text-field :model-value="label" variant="outlined" density="compact" hide-details :aria-label="t('storefrontFooter.label')" @update:model-value="updateText('label', $event)" />
    </EditorFieldRow>
  </div>
</template>
<style scoped>
.social-link-fields { display: flex; flex-direction: column; gap: 12px; }
</style>
