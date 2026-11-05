<script setup lang="ts">
import StorefrontLayout from '~/components/storefront/StorefrontLayout.vue';
import StorefrontRenderer from '~/components/storefront/StorefrontRenderer.vue';
import {createThemeDemoCatalog,createThemeDemoDocument,themeDemoCatalogKey} from '~/features/storefront-builder/utils/theme-demo';
import {themeCatalog} from '~/features/storefront-builder/utils/theme-library';
import {useStorefront} from '~/features/storefront-builder/composables/useStorefront';
import type {PageKind} from '~/types/storefront';
definePageMeta({layout:false,middleware:['merchant'],key:route=>route.fullPath});
const route=useRoute();
const preset=themeCatalog.find(entry=>entry.id===route.params.preset);
if(!preset)throw createError({statusCode:404,statusMessage:'Theme not found'});
const {locale,setLocale}=useI18n();
if(route.query.locale==='en'||route.query.locale==='id')await setLocale(route.query.locale);
const demo=createThemeDemoCatalog(locale.value);
provide(themeDemoCatalogKey,demo);
const document=createThemeDemoDocument(preset.id,locale.value);
const store=useStorefront();
// This route runs in a separate iframe app. It never loads/saves editor drafts.
store.theme.value=document.theme;
store.pages.value=document.pages;
store.headerMenuId.value='main-menu';
store.MenuItems.value=[{id:'main-menu',name:locale.value.startsWith('id')?'Menu utama':'Main menu',items:['HOME','PRODUCT_GRID','COLLECTIONS'].map((pageId,index)=>({id:`demo-menu-${index}`,pageId,label:(locale.value.startsWith('id')?['Beranda','Produk','Koleksi']:['Home','Products','Collections'])[index]}))}];
store.collections.value=demo.collections;
store.products.value=demo.products;
useStorefrontProductStore().setProduct(demo.products[0]!);
const kind=String(route.query.kind??'HOME') as PageKind;
const page=document.pages.find(page=>page.kind===kind)??document.pages[0]!;
const capture=route.query.capture==='1';
const ready=ref(false);
onMounted(()=>{ready.value=true;});
</script>
<template>
  <v-app><div class="theme-demo" :class="{'theme-demo--capture':capture}" :data-theme-ready="ready" data-theme-demo>
    <StorefrontLayout :theme="document.theme" preview demo><StorefrontRenderer :sections="page.sections" :product="demo.products[0]" preview /></StorefrontLayout>
  </div></v-app>
</template>
<style scoped>
.theme-demo {min-width:0;pointer-events:none}
.theme-demo--capture {width:1200px;zoom:calc(100vw / 1200px)}
.theme-demo :deep(.builder-handle){display:none!important}
</style>
