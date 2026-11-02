<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import BlockRenderer from '~/components/storefront/BlockRenderer.vue';
import {useStorefront} from '../../composables/useStorefront';
import {contentSectionSettings} from '../../utils/content-section-layout';
import {getLayoutWidthStyle,getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{settings:Record<string,unknown>;blocks:StorefrontBlock[];preview?:boolean;selectedBlockId?:string|null}>();
const {theme}=useStorefront();const {t}=useI18n();
const position=ref(50);
const images=computed(()=>props.blocks.filter(block=>block.type==='IMAGE'&&block.enabled).slice(0,2));
const headers=computed(()=>props.blocks.filter(block=>block.type!=='IMAGE'));
const normalized=computed(()=>contentSectionSettings(props.settings));
const style=computed(()=>({...getLayoutWidthStyle(normalized.value.layout,theme.value),...getPaddingStyle(normalized.value.padding,theme.value,true)}));
</script>
<template><section :style="style"><BlockRenderer :blocks="headers" :preview="preview" :selected-block-id="selectedBlockId" /><div class="image-compare" :style="{aspectRatio:String(settings.aspectRatio || '1.6')}"><v-img v-if="images[1]?.settings.src" :src="String(images[1].settings.src)" :alt="String(images[1].settings.alt || '')" cover class="image-compare__image" :data-block-id="images[1].id" /><v-img v-if="images[0]?.settings.src" :src="String(images[0].settings.src)" :alt="String(images[0].settings.alt || '')" cover class="image-compare__image" :style="{clipPath:`inset(0 ${100-position}% 0 0)`}" :data-block-id="images[0].id" /><p v-if="!images.some(image=>image.settings.src)" class="image-compare__empty">{{ t('sectionCatalog.compareImages') }}</p><div class="image-compare__line" :style="{left:`${position}%`}" aria-hidden="true" /><span class="image-compare__before">{{ settings.beforeLabel || t('sectionCatalog.before') }}</span><span class="image-compare__after">{{ settings.afterLabel || t('sectionCatalog.after') }}</span><input v-model.number="position" type="range" min="0" max="100" :aria-label="t('sectionCatalog.compare')" class="image-compare__slider" /></div></section></template>
<style scoped>
.image-compare {position:relative;isolation:isolate;min-height:180px;overflow:hidden;background:var(--store-secondary);border-radius:var(--store-radius)}.image-compare__image{position:absolute;inset:0;width:100%;height:100%}.image-compare__line{position:absolute;top:0;bottom:0;border-left:2px solid white;pointer-events:none}.image-compare__slider{position:absolute;bottom:16px;left:5%;width:90%;accent-color:var(--store-primary)}.image-compare__before,.image-compare__after{position:absolute;top:16px;padding:4px 12px;background:#0009;color:white;border-radius:4px}.image-compare__before{left:16px}.image-compare__after{right:16px}.image-compare__empty{position:absolute;inset:0;display:grid;place-items:center;padding:24px;text-align:center}
</style>
