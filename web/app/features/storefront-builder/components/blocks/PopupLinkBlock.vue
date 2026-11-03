<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import BlockLabel from './BlockLabel.vue';
import {slideContainingBlock} from '../../utils/slideshow';
const props=defineProps<{block:StorefrontBlock;preview?:boolean;selectedBlockId?:string|null}>();
const opened=ref(false);const {t}=useI18n();
watch(()=>props.selectedBlockId,id=>{if(props.preview&&id!==props.block.id&&slideContainingBlock([props.block],id))opened.value=true;});
</script>
<template><div><v-btn variant="text" @click="opened=!opened"><BlockLabel :value="String(block.settings.label || '')" /></v-btn><v-card v-if="preview&&opened" variant="outlined" class="mt-3" :title="String(block.settings.title||'')"><v-card-text><slot /></v-card-text></v-card><v-dialog v-if="!preview" v-model="opened" max-width="760" scrollable><v-card :title="String(block.settings.title || '')"><v-card-text><slot /></v-card-text><v-card-actions><v-spacer/><v-btn @click="opened=false">{{t('themeLibrary.close')}}</v-btn></v-card-actions></v-card></v-dialog></div></template>
