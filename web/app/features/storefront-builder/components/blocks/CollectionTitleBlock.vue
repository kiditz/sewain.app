<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import type {CollectionSummary} from '~/types/collection';
import {useStorefront} from "../../composables/useStorefront";
import TextBlock from './TextBlock.vue';
import {collectionBlockContext} from '../../utils/block-context';
import {escapedBlockText} from '../../utils/block-text';
const props=defineProps<{block:StorefrontBlock;collectionContext?:CollectionSummary;preview?:boolean}>();const inherited=inject(collectionBlockContext,null);const {t}=useI18n();
const store=useStorefront();const route=useRoute();
if(!props.collectionContext&&!inherited&&route.path.startsWith('/collections/'))await store.loadCollections().catch(()=>undefined);
const collection=computed(()=>props.collectionContext??inherited?.value??(route.path.startsWith('/collections/')?store.collections.value.find(collection=>collection.slug===route.params.slug):undefined));
const text=computed(()=>({...props.block,settings:{...props.block.settings,text:escapedBlockText(collection.value?.name??(props.preview?t('blockCatalog.collectionTitle'):''))}}));
</script>
<template><TextBlock :block="text" /></template>
