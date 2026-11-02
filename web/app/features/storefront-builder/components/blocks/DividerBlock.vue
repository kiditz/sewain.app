<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import {useStorefront} from '../../composables/useStorefront';
import {getPaddingStyle} from '../../utils/block-settings';
defineProps<{block:StorefrontBlock}>();
const {theme}=useStorefront();
</script>
<template><div :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)"><hr :style="{border:0,borderTop:`${Math.min(20,Math.max(1,Number(block.settings.thickness)||1))}px solid ${block.settings.color || 'var(--store-secondary)'}`}" /></div></template>
