<script setup lang="ts">
import type { StorefrontTheme } from '~/types/storefront';
defineProps<{ name: string; colors?: StorefrontTheme['colors'] }>();
</script>
<template>
  <div class="theme-thumbnail" :style="{ background: colors?.background, color: colors?.text, '--theme-primary': colors?.primary, '--theme-secondary': colors?.secondary, '--theme-surface': colors?.surface }" aria-hidden="true">
    <div class="theme-thumbnail__navigation"><strong>{{ name }}</strong><span>☰</span></div>
    <div class="theme-thumbnail__hero"><span>{{ name }}</span><div class="theme-thumbnail__line" /><div class="theme-thumbnail__button" /></div>
    <div class="theme-thumbnail__grid"><div v-for="n in 3" :key="n"><div class="theme-thumbnail__product" /><div class="theme-thumbnail__line" /></div></div>
  </div>
</template>
<style scoped>
.theme-thumbnail { aspect-ratio: 4 / 3; padding: 16px; background: #fbfaf7; color: #202a25; overflow: hidden; }
.theme-thumbnail__navigation { display: flex; align-items: center; justify-content: space-between; padding: 6px 0 14px; font-size: 12px; }
.theme-thumbnail__hero { min-height: 120px; padding: 20px; background: var(--theme-secondary, #d9e7de); border-radius: 6px; display: flex; flex-direction: column; gap: 12px; }
.theme-thumbnail__hero > span { font-family: Georgia, serif; font-size: 26px; }
.theme-thumbnail__line { height: 5px; background: currentColor; opacity: .2; width: 65%; border-radius: 4px; }
.theme-thumbnail__button { width: 64px; height: 17px; background: var(--theme-primary, #315c4c); border-radius: 4px; }
.theme-thumbnail__grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 16px; }
.theme-thumbnail__product { aspect-ratio: 1; margin-bottom: 10px; border-radius: 6px; background: var(--theme-surface, white); border: 1px solid color-mix(in srgb, currentColor 10%, transparent); }
</style>
