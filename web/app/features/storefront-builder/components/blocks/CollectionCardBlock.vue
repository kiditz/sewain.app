<script setup lang="ts">
import type {CollectionSummary} from '~/types/collection';
import type {StorefrontBlock,SectionLayout,StorefrontPadding} from '~/types/storefront';
import {useStorefront} from '../../composables/useStorefront';
import {collectionBlockContext} from '../../utils/block-context';
import {getLayoutStyle,getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{block:StorefrontBlock;collectionContext?:CollectionSummary;preview?:boolean}>();const store=useStorefront();const router=useRouter();const {t}=useI18n();
if(!props.collectionContext)await store.loadCollections().catch(()=>undefined);
const collection=computed(()=>props.collectionContext??store.collections.value.find(collection=>collection.id===props.block.settings.collectionId));
provide(collectionBlockContext,collection);
const style=computed(()=>({...getLayoutStyle(props.block.settings.layout as SectionLayout,store.theme.value),...getPaddingStyle(props.block.settings.padding as StorefrontPadding,store.theme.value)}));
function open(event:MouseEvent|KeyboardEvent){if(props.preview||!collection.value)return;if(event.type==='click'&&(event.target as HTMLElement).closest('a,button,input'))return;void router.push(`/collections/${encodeURIComponent(collection.value.slug)}`);}
</script>
<template><div v-if="collection||preview" :style="style" class="collection-card-block" :role="!preview&&collection?'link':undefined" :tabindex="!preview&&collection?0:undefined" @click="open" @keydown.enter.self="open"><p v-if="!collection&&preview">{{t('blockCatalog.chooseCollection')}}</p><slot /></div></template>
<style scoped>.collection-card-block{border-radius:var(--store-radius);background:var(--store-surface);color:var(--store-text);cursor:pointer}</style>
