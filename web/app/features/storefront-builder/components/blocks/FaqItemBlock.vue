<script setup lang="ts">
import type { StorefrontBlock, StorefrontPadding } from '~/types/storefront';
import {useStorefront} from '../../composables/useStorefront';
import {getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{block:StorefrontBlock;preview?:boolean;selectedBlockId?:string|null}>();
const {theme}=useStorefront();
const expanded=ref<number>();
watch(()=>props.selectedBlockId,id=>{if(props.preview && id===props.block.id) expanded.value=0;},{immediate:true});
</script>
<template>
  <v-expansion-panels v-model="expanded" :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)">
    <v-expansion-panel :title="String(block.settings.question || '')" elevation="0" variant="accordion">
      <v-expansion-panel-text><p class="faq-answer">{{ block.settings.answer }}</p></v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>
<style scoped>.faq-answer { white-space: pre-line; line-height: 1.7; }</style>
