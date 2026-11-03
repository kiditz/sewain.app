<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import type {Product} from '~/types/product';
import StorefrontFormSection from '../sections/StorefrontFormSection.vue';
import ImageCompareSection from '../sections/ImageCompareSection.vue';
import FeaturedProductsSection from '../sections/FeaturedProductsSection.vue';
const props=defineProps<{block:StorefrontBlock;preview?:boolean;selectedBlockId?:string|null;productContext?:Product}>();
const component=computed(()=>props.block.type==='COMPARISON_SLIDER'?ImageCompareSection:props.block.type==='RECOMMENDED_PRODUCTS'?FeaturedProductsSection:StorefrontFormSection);
const productStore=useStorefrontProductStore();
const route=useRoute();
const product=computed(()=>props.productContext??(route.path.startsWith('/products/')?productStore.product:undefined)??undefined);
</script>
<template><component :is="component" :settings="block.settings" :blocks="block.blocks || []" :preview="preview" :selected-block-id="selectedBlockId" :product="product" /></template>
