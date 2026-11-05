<script setup lang="ts">
import {storefrontPresets} from '../utils/storefront-theme';
import type { StorefrontTheme } from '~/types/storefront';
const props=defineProps<{name:string;source?:string;colors?:StorefrontTheme['colors']}>();
const presetId=computed(()=>props.source??props.name.toLowerCase());
const failed=ref(false);
watch(presetId,()=>{failed.value=false;});
const screenshot=computed(()=>!failed.value&&storefrontPresets.some(name=>name.toLowerCase()===presetId.value)?`/themes/previews/${presetId.value}.png`:null);
</script>
<template>
  <div class="theme-thumbnail" :class="[`theme-thumbnail--${presetId}`, {'theme-thumbnail--screenshot':screenshot}]" :style="{ background: colors?.background, color: colors?.text, '--theme-primary': colors?.primary, '--theme-secondary': colors?.secondary, '--theme-surface': colors?.surface }" aria-hidden="true">
    <img v-if="screenshot" :src="screenshot" alt="" loading="lazy" class="theme-thumbnail__screenshot" @error="failed=true" />
    <template v-else>
    <div class="theme-thumbnail__navigation"><strong>{{ name }}</strong><span>☰</span></div>
    <div class="theme-thumbnail__hero"><span>{{ name }}</span><div class="theme-thumbnail__line" /><div class="theme-thumbnail__button" /></div>
    <div class="theme-thumbnail__grid"><div v-for="n in 3" :key="n"><div class="theme-thumbnail__product" /><div class="theme-thumbnail__line" /></div></div>
    </template>
  </div>
</template>
<style scoped>
.theme-thumbnail { border-radius: inherit; aspect-ratio: 4 / 3; padding: 16px; background: #fbfaf7; color: #202a25; overflow: hidden; }
.theme-thumbnail__navigation { display: flex; align-items: center; justify-content: space-between; padding: 6px 0 14px; font-size: 12px; }
.theme-thumbnail__hero { min-height: 120px; padding: 20px; background: var(--theme-secondary, #d9e7de); border-radius: 6px; display: flex; flex-direction: column; gap: 12px; }
.theme-thumbnail__hero > span { font-family: Georgia, serif; font-size: 26px; }
.theme-thumbnail__line { height: 5px; background: currentColor; opacity: .2; width: 65%; border-radius: 4px; }
.theme-thumbnail__button { width: 64px; height: 17px; background: var(--theme-primary, #315c4c); border-radius: 4px; }
.theme-thumbnail__grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 16px; }
.theme-thumbnail__product { aspect-ratio: 1; margin-bottom: 10px; border-radius: 6px; background: var(--theme-surface, white); border: 1px solid color-mix(in srgb, currentColor 10%, transparent); }
.theme-thumbnail--screenshot {padding:0}
.theme-thumbnail__screenshot {display:block;width:100%;height:100%;position:absolute;inset:0;object-fit:cover;object-position:top}
.theme-thumbnail {position:relative}
/* Schematic cards reflect each preset's composition without remote assets. */
.theme-thumbnail--trail .theme-thumbnail__hero {min-height:150px;justify-content:flex-end;border-radius:2px}
.theme-thumbnail--trail .theme-thumbnail__grid {grid-template-columns:1fr 2fr 1fr}
.theme-thumbnail--coast .theme-thumbnail__hero {margin-inline:12px;border-radius:18px;background:linear-gradient(135deg,var(--theme-secondary),var(--theme-surface))}
.theme-thumbnail--coast .theme-thumbnail__grid {grid-template-columns:2fr 1fr 1fr}
.theme-thumbnail--atelier .theme-thumbnail__hero {background:transparent;padding:12px 0;min-height:140px}
.theme-thumbnail--atelier .theme-thumbnail__hero > span {font-size:40px;font-style:italic}
.theme-thumbnail--atelier .theme-thumbnail__grid {grid-template-columns:1fr 1.5fr;gap:20px}
.theme-thumbnail--atelier .theme-thumbnail__grid > :nth-child(2) {margin-top:24px}
.theme-thumbnail--atelier .theme-thumbnail__grid > :nth-child(3) {display:none}
.theme-thumbnail--bloom .theme-thumbnail__hero {border-radius:28px;align-items:center;text-align:center}
.theme-thumbnail--bloom .theme-thumbnail__product {border-radius:20px}
.theme-thumbnail--mono .theme-thumbnail__hero {border-radius:0;min-height:130px;border-bottom:3px solid var(--theme-primary)}
.theme-thumbnail--mono .theme-thumbnail__hero > span,.theme-thumbnail--studio .theme-thumbnail__hero > span,.theme-thumbnail--trail .theme-thumbnail__hero > span,.theme-thumbnail--coast .theme-thumbnail__hero > span,.theme-thumbnail--midnight .theme-thumbnail__hero > span {font-family:Inter,Arial,sans-serif;font-weight:700}
.theme-thumbnail--mono .theme-thumbnail__grid {gap:6px;grid-template-columns:repeat(4,1fr)}
.theme-thumbnail--mono .theme-thumbnail__product {border-radius:0}
.theme-thumbnail--midnight .theme-thumbnail__hero {margin-inline:12px;box-shadow:10px 8px 0 color-mix(in srgb,var(--theme-primary) 30%,var(--theme-surface))}
.theme-thumbnail--midnight .theme-thumbnail__grid {grid-template-columns:1fr 2fr 1fr;margin-top:24px}
.theme-thumbnail--terra .theme-thumbnail__hero {display:grid;grid-template-columns:1fr 1fr;background:transparent;min-height:150px;padding:12px 0}
.theme-thumbnail--terra .theme-thumbnail__hero > span {grid-row:span 2;font-size:30px}
.theme-thumbnail--terra .theme-thumbnail__hero .theme-thumbnail__line {height:90px;width:100%;background:var(--theme-secondary);opacity:1;border-radius:0}
.theme-thumbnail--terra .theme-thumbnail__grid {grid-template-columns:1fr 2fr 1fr}
</style>
