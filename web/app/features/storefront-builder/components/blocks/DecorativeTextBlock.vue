<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import TextBlock from './TextBlock.vue';
import {storefrontBrandingKey} from '../../utils/storefront-branding';
import {copyrightBlockText} from '../../utils/block-text';
const props=defineProps<{block:StorefrontBlock}>();
const branding=inject(storefrontBrandingKey,null);
const rendered=computed(()=>({...props.block,settings:{...props.block.settings,text:props.block.type==='COPYRIGHT'?copyrightBlockText(props.block.settings.text,branding?.merchantName.value || '',new Date().getFullYear()):props.block.settings.text}}));
const style=computed(()=>props.block.type==='JUMBO_TEXT'?{fontSize:`clamp(32px, 12cqw, ${Math.min(300,Math.max(32,Number(props.block.settings.fontSize)||120))}px)`,lineHeight:'1'}:undefined);
</script>
<template><TextBlock :block="rendered" :style="style" /></template>
