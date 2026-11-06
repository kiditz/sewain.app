<script setup lang="ts">
import {sanitizeRichText} from "~/utils/content-security";
const props = defineProps<{ value?: string }>();
const html=computed(()=>sanitizeRichText(props.value));
</script>
<template><span class="block-label ql-editor" v-html="html" /></template>
<style scoped>
.block-label { display: inline-block; min-height: 0; max-width: 100%; padding: 0; font: inherit; color: inherit; text-align: inherit; white-space: normal; overflow-wrap: anywhere; cursor: inherit; }
.block-label :deep(*) { cursor: inherit; }
</style>
