<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import BlockRenderer from '~/components/storefront/BlockRenderer.vue';
import {useStorefront} from '../../composables/useStorefront';
import {contentSectionSettings} from '../../utils/content-section-layout';
import {getLayoutStyle,getLayoutWidthStyle,getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{settings:Record<string,unknown>;blocks:StorefrontBlock[];preview?:boolean;selectedBlockId?:string|null}>();
const store=useStorefront();const {t}=useI18n();
const normalized=computed(()=>contentSectionSettings(props.settings));
const style=computed(()=>({...getLayoutWidthStyle(normalized.value.layout,store.theme.value),...getPaddingStyle(normalized.value.padding,store.theme.value,true)}));
const layoutStyle=computed(()=>({...getLayoutStyle({...normalized.value.layout,width:'full'},store.theme.value),display:normalized.value.layout.direction==='horizontal'?'grid':'flex'}));
const productId=computed(()=>String(props.settings.productId || ''));
const {data:matches}=await useAsyncData(()=>store.queryKey('featured-product',{id:productId.value}),async()=>productId.value?(await store.fetchProducts({id:productId.value,size:1})).items:[]);
const product=computed(()=>matches.value?.[0]??null);
const displayBlocks=computed(()=>product.value ? props.blocks : props.blocks.filter(block=>!['PRODUCT_TITLE','PRODUCT_PRICE'].includes(block.type)));
</script>
<template><section v-if="product || preview" class="featured-product-section" :class="{'featured-product-section--highlight':settings.variant==='highlight'}" :style="style"><div class="featured-product-section__layout" :style="layoutStyle"><div class="featured-product-section__media"><v-img v-if="product?.media" :src="product.media" :alt="product.name" cover aspect-ratio="1" /><p v-else>{{ t('sectionCatalog.chooseProduct') }}</p></div><div class="featured-product-section__details"><BlockRenderer :blocks="displayBlocks" :preview="preview" :selected-block-id="selectedBlockId" :product-context="product || undefined" /><v-btn v-if="product" :to="preview?undefined:`/products/${product.slug}`" :disabled="preview" color="primary" class="mt-5">{{ settings.buttonLabel || t('sectionCatalog.viewProduct') }}</v-btn></div></div></section></template>
<style scoped>
.featured-product-section{container:featured-product / inline-size}
.featured-product-section__layout{grid-template-columns:minmax(0,1fr) minmax(0,1fr)}
.featured-product-section__media{min-width:0;width:100%;background:var(--store-secondary);border-radius:var(--store-radius);overflow:hidden}
.featured-product-section__media>p{padding:64px 24px;text-align:center}.featured-product-section__details{min-width:0;width:100%}
.featured-product-section--highlight > .featured-product-section__layout{grid-template-columns:minmax(0,2fr) minmax(0,1fr)}
@container featured-product (max-width:640px){.featured-product-section__layout{display:flex!important;flex-direction:column!important}}
</style>
