<script setup lang="ts">
import {PlusOutlined} from '@ant-design/icons-vue';
import type {StorefrontBlock} from '~/types/storefront';
import {useStorefrontCatalog} from '../../composables/useStorefrontCatalog';
const props=defineProps<{block:StorefrontBlock;preview?:boolean}>();
const catalog=useStorefrontCatalog();
const {t}=useI18n();
const id=computed(()=>String(props.block.settings.productId || ''));
const {data:product}=await useAsyncData(()=>catalog.queryKey('hotspot',{id:id.value}),async()=>id.value?(await catalog.fetchProducts({id:id.value,size:1})).items[0]??null:null);
const position=computed(()=>({left:`${Math.min(100,Math.max(0,Number(props.block.settings.x)||0))}%`,top:`${Math.min(100,Math.max(0,Number(props.block.settings.y)||0))}%`}));
</script>
<template><div v-if="product || preview" class="product-hotspot" :style="position"><v-menu location="bottom" :close-on-content-click="false"><template #activator="{props:activator}"><v-btn v-bind="activator" icon size="small" color="surface" :aria-label="product?.name || t('sectionCatalog.chooseProduct')"><PlusOutlined /></v-btn></template><v-card width="260" rounded="lg"><v-img v-if="product?.media" :src="product.media" height="180" cover /><v-card-text>{{ product?.name || t('sectionCatalog.chooseProduct') }}</v-card-text><v-card-actions v-if="product"><v-btn :to="preview?undefined:`/products/${product.slug}`" :disabled="preview">{{ t('sectionCatalog.viewProduct') }}</v-btn></v-card-actions></v-card></v-menu></div></template>
<style scoped>.product-hotspot {position:absolute;transform:translate(-50%,-50%);z-index:2}</style>
