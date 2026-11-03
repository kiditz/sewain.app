<script setup lang="ts">
import type {StorefrontBlock,StorefrontPadding} from '~/types/storefront';
import {paymentIcons} from '../../utils/payment-icons';
import {getPaddingStyle} from '../../utils/block-settings';
import {useStorefront} from '../../composables/useStorefront';
const props=defineProps<{block:StorefrontBlock;preview?:boolean}>();const {theme}=useStorefront();const {t}=useI18n();
const methods=computed(()=>Array.isArray(props.block.settings.methods)?props.block.settings.methods.filter((key):key is keyof typeof paymentIcons=>typeof key==='string'&&Object.hasOwn(paymentIcons,key)):[]);
</script>
<template><div v-if="methods.length||preview" class="payment-icons" :style="getPaddingStyle(block.settings.padding as StorefrontPadding,theme)"><v-tooltip v-for="method in methods" :key="method" :text="t(`blockCatalog.payments.${method}`)"><template #activator="{props:activator}"><span v-bind="activator" role="img" :aria-label="t(`blockCatalog.payments.${method}`)" :style="{fontSize:`${Math.min(80,Math.max(16,Number(block.settings.size)||28))}px`}"><component :is="paymentIcons[method]" aria-hidden="true" /><small>{{t(`blockCatalog.payments.${method}`)}}</small></span></template></v-tooltip><span v-if="!methods.length&&preview" class="text-body-small">{{t('blockCatalog.selectPayments')}}</span></div></template>
<style scoped>.payment-icons > span{display:inline-flex;align-items:center;gap:6px;padding:4px 8px;border:1px solid var(--store-secondary);border-radius:4px}.payment-icons small{font-size:10px}.payment-icons{display:flex;align-items:center;gap:16px;flex-wrap:wrap;color:var(--store-text)}</style>
