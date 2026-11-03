<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import BlockLabel from './BlockLabel.vue';
import {useStorefront} from '../../composables/useStorefront';
import {getPaddingStyle} from '../../utils/block-settings';
import {blockLinkUrl,savedBlockLinks} from '../../utils/block-link';
const props=defineProps<{block:StorefrontBlock;preview?:boolean}>();
const {theme,pages}=useStorefront();
const href=computed(()=>{const link=savedBlockLinks(props.block.settings.linkTargets).url;const value=link?blockLinkUrl(link,pages.value):String(props.block.settings.url || '');return /^(\/(?!\/)|https?:\/\/)/i.test(value)?value:'';});
</script>
<template><a v-if="href||preview" class="policy-link" :href="href || undefined" :target="block.settings.openInNewTab?'_blank':undefined" :rel="block.settings.openInNewTab?'noopener noreferrer':undefined" :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)" @click="preview&&$event.preventDefault()"><BlockLabel :value="String(block.settings.label || '')" /></a></template>
<style scoped>.policy-link{display:inline-flex;align-items:center;color:var(--store-text);cursor:pointer;text-decoration:underline;text-underline-offset:4px}</style>
