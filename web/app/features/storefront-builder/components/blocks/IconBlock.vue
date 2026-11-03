<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import {contentIcons} from '../../utils/content-icons';
import {getPaddingStyle} from '../../utils/block-settings';
import {useStorefront} from '../../composables/useStorefront';
const props=defineProps<{block:StorefrontBlock}>();const {theme}=useStorefront();
const icon=computed(()=>{const key=String(props.block.settings.icon);return Object.hasOwn(contentIcons,key)?contentIcons[key as keyof typeof contentIcons]:contentIcons.StarOutlined;});
const layout=computed(()=>props.block.settings.layout as {width?:string;alignment?:string}|undefined);
const style=computed(()=>({width:layout.value?.width==='fill'?'100%':undefined,justifyContent:layout.value?.alignment==='right'?'flex-end':layout.value?.alignment==='left'?'flex-start':'center',...getPaddingStyle(props.block.settings.padding as StorefrontPadding,theme.value),fontSize:`${Math.min(256,Math.max(8,Number(props.block.settings.size)||48))}px`,color:String(props.block.settings.color||'var(--store-primary)')}));
</script>
<template><span class="store-icon" :style="style"><component :is="icon" aria-hidden="true" /></span></template>
<style scoped>.store-icon{display:inline-flex;align-items:center;justify-content:center}</style>
