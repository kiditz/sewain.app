<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import type {Product} from '~/types/product';
import TextBlock from './TextBlock.vue';
import {getPaddingStyle} from '../../utils/block-settings';
import {useStorefront} from '../../composables/useStorefront';
const props=defineProps<{block:StorefrontBlock;productContext?:Product;preview?:boolean}>();
const productStore=useStorefrontProductStore();const {theme}=useStorefront();
const route=useRoute();
const product=computed(()=>props.productContext??(props.preview||route.path.startsWith('/products/')?productStore.product:undefined));
const sku=computed(()=>{if(!product.value)return '';if(!props.productContext||props.productContext.id===productStore.product?.id)return productStore.selectedVariant?.sku??'';return props.productContext.variants.find(variant=>variant.active)?.sku??'';});
const style=computed(()=>({...getPaddingStyle(props.block.settings.padding as StorefrontPadding,theme.value),textAlign:((props.block.settings.layout as {alignment?:'left'|'center'|'right'}|undefined)?.alignment??'left')}));
const description=computed(()=>({...props.block,settings:{...props.block.settings,text:product.value?.description||''}}));
</script>
<template><TextBlock v-if="block.type==='PRODUCT_DESCRIPTION'" :block="description" /><div v-else-if="sku||preview" :style="style">{{ block.settings.prefix }}: {{ sku || '—' }}</div></template>
