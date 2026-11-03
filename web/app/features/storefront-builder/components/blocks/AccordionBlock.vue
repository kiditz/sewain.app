<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import {getPaddingStyle} from '../../utils/block-settings';
import {useStorefront} from '../../composables/useStorefront';
import {slideContainingBlock} from '../../utils/slideshow';
const props=defineProps<{block:StorefrontBlock;preview?:boolean;selectedBlockId?:string|null}>();
const {theme}=useStorefront();
const expanded=ref<number|undefined>(props.block.settings.open?0:undefined);
watch(()=>props.block.settings.open,value=>{expanded.value=value?0:undefined;});
watch(()=>props.selectedBlockId,id=>{if(props.preview&&id!==props.block.id&&slideContainingBlock([props.block],id))expanded.value=0;});
</script>
<template><v-expansion-panels v-model="expanded" :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)"><v-expansion-panel :title="String(block.settings.title || '')"><v-expansion-panel-text><slot /></v-expansion-panel-text></v-expansion-panel></v-expansion-panels></template>
