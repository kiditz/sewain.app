<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { QuillEditor } from "@vueup/vue-quill";

const props = defineProps<{
  block: StorefrontBlock;
}>();
const text = computed(() => String(props.block.settings.text ?? ""));

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();
</script>
<template>
  <div class="px-4 py-2">
    <QuillEditor
      :content="text"
      content-type="html"
      theme="snow"
      toolbar="essential"
      @update:content="emit('update', 'text', $event)"
    />
  </div>
</template>
