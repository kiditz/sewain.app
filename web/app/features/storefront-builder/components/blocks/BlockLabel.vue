<script setup lang="ts">
const props = defineProps<{ value?: string }>();
const html = computed(() => {
  const value = props.value ?? "";
  if (/<\/?(?:p|h[1-6]|strong|b|em|i|u|ol|ul|li|blockquote|pre|code|br|span)\b/i.test(value)) return value;
  return value.replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]!);
});
</script>
<template><span class="block-label ql-editor" v-html="html" /></template>
<style scoped>
.block-label { display: inline-block; min-height: 0; max-width: 100%; padding: 0; font: inherit; color: inherit; text-align: inherit; white-space: normal; overflow-wrap: anywhere; }
</style>
