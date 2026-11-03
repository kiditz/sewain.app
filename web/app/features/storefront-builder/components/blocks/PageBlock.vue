<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import StorefrontRenderer from '~/components/storefront/StorefrontRenderer.vue';
import {useStorefront} from '../../composables/useStorefront';
import {canEmbedPage} from "../../utils/block-text";
import {embeddedPageContext} from '../../utils/block-context';
import {getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{block:StorefrontBlock;preview?:boolean}>();const {pages,theme}=useStorefront();const {t}=useI18n();
const ancestors=inject(embeddedPageContext,computed<string[]>(()=>[]));
const id=computed(()=>String(props.block.settings.pageId||''));
const page=computed(()=>pages.value.find(page=>page.id===id.value&&page.kind==='CUSTOM'));
const recursive=computed(()=>!canEmbedPage(id.value,ancestors.value));
provide(embeddedPageContext,computed(()=>[...ancestors.value,id.value]));
const sections=computed(()=>page.value?.sections.filter(section=>!['NAVIGATION','FOOTER'].includes(section.type))??[]);
</script>
<template><div :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)"><p v-if="preview&&(!page||recursive)">{{t(recursive?'blockCatalog.pageLoop':'blockCatalog.choosePage')}}</p><template v-if="page&&!recursive"><h2 v-if="block.settings.showTitle" class="mb-4">{{page.title}}</h2><div :style="preview?{pointerEvents:'none'}:undefined"><StorefrontRenderer :key="id" :sections="sections" :preview="preview" /></div></template></div></template>
