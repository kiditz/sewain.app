<script setup lang="ts">
import {resourceCardLayoutKey} from '../utils/resource-list-layout';
const props=withDefaults(defineProps<{layout:string;columns?:number;mobileColumns?:number;gap?:number}>(),{columns:3,mobileColumns:1,gap:18});
provide(resourceCardLayoutKey,computed(()=>props.layout));
const style=computed(()=>({
  '--resource-columns':String(Math.min(8,Math.max(1,Number(props.columns)||3))),
  '--resource-mobile-columns':props.mobileColumns===2?'2':'1',
  '--resource-gap':`${Number.isFinite(Number(props.gap))?Math.min(96,Math.max(0,Number(props.gap))):18}px`,
}));
</script>
<template><div class="resource-layout" :class="`resource-layout--${layout}`" :style="style"><div class="resource-layout__grid"><slot /></div></div></template>
<style scoped>
.resource-layout {container:resource-list / inline-size;width:100%;min-width:0}
.resource-layout__grid {display:grid;grid-template-columns:repeat(var(--resource-columns),minmax(0,1fr));gap:var(--resource-gap);width:100%;min-width:0}
.resource-layout__grid > :deep(.resource-layout__item) {min-width:0;overflow-wrap:anywhere}
.resource-layout--carousel > .resource-layout__grid {display:flex;overflow-x:auto;overscroll-behavior-inline:contain;scroll-snap-type:x mandatory;scrollbar-width:thin}
.resource-layout--carousel > .resource-layout__grid > :deep(.resource-layout__item) {flex:0 0 calc((100% - (var(--resource-columns) - 1) * var(--resource-gap)) / var(--resource-columns));scroll-snap-align:start}
.resource-layout--text > .resource-layout__grid {display:flex;flex-direction:column;gap:0}
.resource-layout--spotlight > .resource-layout__grid {grid-template-columns:repeat(2,minmax(0,1fr))}
.resource-layout--bento > .resource-layout__grid {grid-template-columns:repeat(12,minmax(0,1fr));--resource-layout-gap:min(var(--resource-gap),5cqw);column-gap:var(--resource-layout-gap);row-gap:calc(var(--resource-layout-gap) * 1.5);align-items:stretch}
.resource-layout--editorial > .resource-layout__grid {grid-template-columns:repeat(12,minmax(0,1fr));--resource-layout-gap:min(var(--resource-gap),5cqw);gap:var(--resource-layout-gap);grid-auto-rows:max(8px,calc((100cqw - 11 * var(--resource-layout-gap)) / 12));align-items:stretch}
.resource-layout--bento > .resource-layout__grid > :deep(.resource-layout__item),.resource-layout--editorial > .resource-layout__grid > :deep(.resource-layout__item) {grid-column:var(--resource-desktop-column);grid-row:var(--resource-desktop-row);min-height:0}
/* The layout owns media geometry, while card text and padding stay editable. */
.resource-layout--bento :deep(.block-image) {height:clamp(180px,25cqw,320px)!important;max-height:none!important;aspect-ratio:auto!important}
.resource-layout--bento :deep(.block-image img),.resource-layout--editorial :deep(.block-image img) {width:100%;height:100%;object-fit:cover}
.resource-layout--bento :deep(.block-block-image-placeholder),.resource-layout--editorial :deep(.block-block-image-placeholder) {height:100%;display:grid;place-items:center}
.resource-layout--editorial :deep(.resource-layout__item > .block-renderer),.resource-layout--editorial :deep(.resource-layout__item > .block-renderer > .builder-block),.resource-layout--editorial :deep(.collection-card-block),.resource-layout--editorial :deep(.product-card-template),.resource-layout--editorial :deep(.collection-card-block > .block-renderer),.resource-layout--editorial :deep(.product-card-template > .block-renderer) {height:100%!important;min-height:0}
.resource-layout--editorial :deep(.collection-card-block > .block-renderer),.resource-layout--editorial :deep(.product-card-template > .block-renderer) {justify-content:flex-start!important}
.resource-layout--editorial :deep(.builder-block[data-block-type="IMAGE"]) {flex:1 1 0;min-height:0}
.resource-layout--editorial :deep(.builder-block:not([data-block-type="IMAGE"])) {flex-shrink:0}
.resource-layout--editorial :deep(.block-image) {height:100%!important;max-height:none!important;aspect-ratio:auto!important}
@container resource-list (max-width:900px) {
  .resource-layout--bento > .resource-layout__grid {grid-template-columns:repeat(2,minmax(0,1fr));--resource-layout-gap:var(--resource-gap)}
  .resource-layout--bento > .resource-layout__grid > :deep(.resource-layout__item) {grid-column:var(--resource-mobile-column);grid-row:var(--resource-mobile-row)}
  .resource-layout--bento :deep(.block-image) {height:clamp(140px,42cqw,260px)!important}
}
@container resource-list (max-width:749px) {
  .resource-layout--editorial > .resource-layout__grid {display:flex;flex-direction:column;gap:max(32px,calc(var(--resource-gap) * 2))}
  .resource-layout--editorial > .resource-layout__grid > :deep(.resource-layout__item) {width:var(--resource-mobile-width);align-self:var(--resource-mobile-align);aspect-ratio:var(--resource-mobile-ratio)}
}
@container resource-list (max-width:600px) {
  .resource-layout--grid > .resource-layout__grid {grid-template-columns:repeat(var(--resource-mobile-columns),minmax(0,1fr))}
  .resource-layout--carousel > .resource-layout__grid > :deep(.resource-layout__item) {flex-basis:calc((100% - (var(--resource-mobile-columns) - 1) * var(--resource-gap)) / var(--resource-mobile-columns))}
  .resource-layout--spotlight > .resource-layout__grid {grid-template-columns:1fr}
}
</style>
