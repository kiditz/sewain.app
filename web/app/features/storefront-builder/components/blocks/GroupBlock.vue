<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import HeaderBlock from './HeaderBlock.vue';
defineProps<{block:StorefrontBlock;preview?:boolean}>();const {t}=useI18n();
</script>
<template><HeaderBlock :block="block"><p v-if="preview&&!block.blocks?.length" class="group-placeholder">{{t('blockCatalog.addChildren')}}</p><slot /></HeaderBlock></template>
<style scoped>.group-placeholder{padding:16px;border:1px dashed #8886;width:100%;font-size:13px;opacity:.7}</style>
