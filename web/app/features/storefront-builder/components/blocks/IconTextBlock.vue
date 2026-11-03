<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import TextBlock from './TextBlock.vue';
import {contentIcons} from '../../utils/content-icons';
import {useStorefront} from '../../composables/useStorefront';
import {getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{block:StorefrontBlock}>();
const {theme}=useStorefront();
const textBlock=computed(()=>({...props.block,settings:{...props.block.settings,padding:{top:0,right:0,bottom:0,left:0}}}));
const icon=computed(()=>contentIcons[String(props.block.settings.icon) as keyof typeof contentIcons] ?? contentIcons.CheckCircleOutlined);
</script>
<template><div class="icon-text" :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)"><component :is="icon" class="icon-text__icon" aria-hidden="true" /><h3>{{ block.settings.title }}</h3><TextBlock :block="textBlock" /></div></template>
<style scoped>.icon-text {display:flex;flex-direction:column;gap:12px}.icon-text__icon {font-size:32px;color:var(--store-primary)}</style>
