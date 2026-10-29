<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { QuillEditor } from "@vueup/vue-quill";
const props = withDefaults(defineProps<{ block: StorefrontBlock; fieldKey?: "text" | "label" }>(), { fieldKey: "text" });
const text = computed(() => String(props.block.settings[props.fieldKey] ?? ""));
const toolbar = [
  [{ header: [1, 2, 3, 4, 5, 6, false] }],
  ["bold", "italic", "underline"],
  [{ list: "bullet" }, { list: "ordered" }],
  ["blockquote", "code-block"],
];
const options = { formats: ["header", "bold", "italic", "underline", "list", "blockquote", "code-block"] };
const emit = defineEmits<{ update: [key: string, value: unknown] }>();
</script>
<template>
  <div class="px-4 py-2">
    <QuillEditor
      :key="`${block.id}-${fieldKey}`"
      :content="text"
      content-type="html"
      theme="snow"
      :toolbar="toolbar"
      :options="options"
      @update:content="emit('update', fieldKey, $event)"
    />
  </div>
</template>
