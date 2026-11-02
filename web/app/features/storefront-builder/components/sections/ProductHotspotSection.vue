<script setup lang="ts">
import type {StorefrontBlock,SectionLayout} from '~/types/storefront';
import BlockRenderer from '~/components/storefront/BlockRenderer.vue';
import {useStorefront} from '../../composables/useStorefront';
import {contentSectionSettings} from '../../utils/content-section-layout';
import {getLayoutWidthStyle,getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{settings:Record<string,unknown>;blocks:StorefrontBlock[];preview?:boolean;selectedBlockId?:string|null}>();
const {theme}=useStorefront();const {t}=useI18n();
const normalized=computed(()=>contentSectionSettings(props.settings));
const style=computed(()=>({...getLayoutWidthStyle(normalized.value.layout,theme.value),...getPaddingStyle(normalized.value.padding,theme.value,true)}));
const image=computed(()=>props.blocks.find(block=>block.type==='IMAGE'&&block.enabled));
const markers=computed(()=>props.blocks.filter(block=>block.type==='HOTSPOT'));
const headers=computed(()=>props.blocks.filter(block=>!['IMAGE','HOTSPOT'].includes(block.type)));
const markerLayout={direction:'vertical',width:'full',height:'auto',alignment:'left',position:'top',gap:0} as SectionLayout;
</script>
<template><section :style="style"><BlockRenderer :blocks="headers" :preview="preview" :selected-block-id="selectedBlockId" /><div class="hotspot-image" :style="{aspectRatio:String(settings.aspectRatio || '1.6')}"><v-img v-if="image?.settings.src" :src="String(image.settings.src)" :alt="String(image.settings.alt || '')" cover height="100%" :data-block-id="image.id" /><p v-else class="hotspot-image__empty">{{ t('sectionCatalog.hotspotImage') }}</p><BlockRenderer class="hotspot-image__markers" :blocks="markers" :layout="markerLayout" :preview="preview" :selected-block-id="selectedBlockId" /></div></section></template>
<style scoped>.hotspot-image{position:relative;min-height:220px;background:var(--store-secondary);border-radius:var(--store-radius)}.hotspot-image__empty{display:grid;place-items:center;height:100%;padding:24px;text-align:center}.hotspot-image__markers{position:absolute;inset:0;height:100%!important}.hotspot-image__markers :deep([data-block-type="HOTSPOT"]){position:static}</style>
