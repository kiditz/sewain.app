<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import type {Product} from '~/types/product';
import AddToCartBlock from './AddToCartBlock.vue';
const props=defineProps<{block:StorefrontBlock;productContext?:Product;preview?:boolean}>();const store=useStorefrontProductStore();const {t}=useI18n();
const route=useRoute();
const onProductPage=computed(()=>route.path.startsWith('/products/'));
const button=computed(()=>({...props.block,type:'ADD_TO_CART' as const}));
</script>
<template><template v-if="productContext||onProductPage||preview"><v-btn v-if="productContext&&(!onProductPage||store.product?.id!==productContext.id)" :to="preview?undefined:`/products/${productContext.slug}`" :disabled="preview" color="primary">{{t('blockCatalog.chooseOptions')}}</v-btn><AddToCartBlock v-else :block="button" :preview="preview||!onProductPage" /></template></template>
