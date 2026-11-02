<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import TextBlock from './TextBlock.vue';
defineProps<{block:StorefrontBlock;preview?:boolean}>();
</script>
<template><div class="marquee" :class="{'marquee--paused':preview}" :style="{'--marquee-duration':`${Math.min(120,Math.max(5,Number(block.settings.speed)||25))}s`,animationDirection:block.settings.reverse?'reverse':undefined}"><div class="marquee__track" :style="{animationDirection:block.settings.reverse?'reverse':'normal'}"><TextBlock :block="block" /><TextBlock v-if="!preview" :block="block" aria-hidden="true" /></div></div></template>
<style scoped>
.marquee {overflow:hidden;width:100%}.marquee__track {display:flex;gap:32px;width:max-content;animation:marquee var(--marquee-duration) linear infinite}.marquee__track > :deep(*) {white-space:nowrap;flex:none;font-size:clamp(24px,5vw,72px)}
.marquee:hover .marquee__track,.marquee--paused .marquee__track {animation-play-state:paused}@keyframes marquee {to{transform:translateX(calc(-50% - 16px))}}@media(prefers-reduced-motion:reduce){.marquee__track{animation:none;flex-wrap:wrap;width:100%}.marquee__track > :deep(*){white-space:normal}.marquee__track > :deep([aria-hidden]){display:none}}
</style>
