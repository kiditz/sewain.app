<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
defineProps<{block:StorefrontBlock;preview?:boolean}>();
</script>
<template><div :style="{height:`${Math.min(1000,Math.max(0,Number(block.settings.height)||0))}px`,width:'100%'}" :class="{'spacer-preview':preview}" aria-hidden="true" /></template>
<style scoped>.spacer-preview{background:repeating-linear-gradient(45deg,transparent,transparent 8px,#8881 8px,#8881 16px);border:1px dashed #8886}</style>
