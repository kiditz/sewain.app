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
// One loader per mounted section. Explicitly refresh when the selected ID changes;
// tag each response so a previous product cannot flash while the next one loads.
const {data:result,status,error}=await useAsyncData(
  store.queryKey('featured-product-section',{instance:useId()}),
  async()=>{
    const id=productId.value;
    if(!id)return {id,product:null};
    const response=await store.fetchProducts({id,page:0,size:1});
    const selected=response.items.find(item=>item.id===id);
    return {id,product:selected?{...selected,variants:selected.variants??[]}:null};
  },
  {watch:[productId],default:()=>({id:'',product:null})},
);
const product=computed(()=>result.value?.id===productId.value?result.value.product:null);
const unavailable=computed(()=>!!productId.value&&result.value?.id===productId.value&&status.value==='success'&&!product.value);
const productImage=computed(()=>product.value?.media||product.value?.variants?.filter(variant=>variant.active).flatMap(variant=>variant.mediaList).find(Boolean)||'');
const snackbar=useSnackbarStore();
watch([error,unavailable],([failure,missing])=>{
  if(props.preview&&(failure||missing))snackbar.openSnackbar({type:'error',message:t(failure?'productGrid.loadFailed':'apiErrors.product_notfound')});
},{immediate:true});
const displayBlocks=computed(()=>product.value ? props.blocks : props.blocks.filter(block=>!['PRODUCT_TITLE','PRODUCT_PRICE','PRODUCT_DESCRIPTION','SKU','BUY_BUTTONS'].includes(block.type)));

</script>
<template><section v-if="product || preview" class="featured-product-section" :class="{'featured-product-section--highlight':settings.variant==='highlight'}" :style="style"><div class="featured-product-section__layout" :style="layoutStyle"><div class="featured-product-section__media"><v-img v-if="product && productImage" :src="productImage" :alt="product.name" cover aspect-ratio="1" /><div v-else class="featured-product-section__placeholder"><v-progress-circular v-if="status==='pending'" indeterminate size="28" /><p v-else>{{t(error?'productGrid.loadFailed':unavailable?'apiErrors.product_notfound':product?'ui.no_image_selected':'sectionCatalog.chooseSectionProduct')}}</p></div></div><div class="featured-product-section__details"><BlockRenderer :blocks="displayBlocks" :preview="preview" :selected-block-id="selectedBlockId" :product-context="product || undefined" /><v-btn v-if="product" :to="preview?undefined:`/products/${product.slug}`" :disabled="preview" color="primary" class="mt-5">{{ settings.buttonLabel || t('sectionCatalog.viewProduct') }}</v-btn></div></div></section></template>
<style scoped>
.featured-product-section{container:featured-product / inline-size}
.featured-product-section__layout{grid-template-columns:minmax(0,1fr) minmax(0,1fr)}
.featured-product-section__media{min-width:0;width:100%;background:var(--store-secondary);border-radius:var(--store-radius);overflow:hidden}
.featured-product-section__placeholder{padding:64px 24px;min-height:240px;display:grid;place-items:center;text-align:center}.featured-product-section__details{min-width:0;width:100%}
.featured-product-section--highlight > .featured-product-section__layout{grid-template-columns:minmax(0,2fr) minmax(0,1fr)}
@container featured-product (max-width:640px){.featured-product-section__layout{display:flex!important;flex-direction:column!important}}
</style>
