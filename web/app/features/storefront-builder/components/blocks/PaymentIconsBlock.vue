<script setup lang="ts">
import type { StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import { paymentIcons } from "../../utils/payment-icons";
import { getPaddingStyle } from "../../utils/block-settings";
import { useStorefront } from "../../composables/useStorefront";

const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { theme } = useStorefront();
const { t } = useI18n();
const methods = computed(() =>
  Array.isArray(props.block.settings.methods)
    ? props.block.settings.methods.filter(
        (key): key is keyof typeof paymentIcons =>
          typeof key === "string" && Object.hasOwn(paymentIcons, key),
      )
    : [],
);
const iconSize = computed(() => Math.min(80, Math.max(16, Number(props.block.settings.size) || 28)));
</script>

<template>
  <div
    v-if="methods.length || preview"
    class="payment-icons"
    :style="getPaddingStyle(block.settings.padding as StorefrontPadding, theme)"
  >
    <v-tooltip v-for="method in methods" :key="method" :text="t(`blockCatalog.payments.${method}`)">
      <template #activator="{ props: activator }">
        <span
          v-bind="activator"
          class="payment-icons__badge"
          :style="{ '--payment-icon-size': `${iconSize}px` }"
        >
          <img
            :src="paymentIcons[method]"
            :alt="t(`blockCatalog.payments.${method}`)"
            :height="iconSize"
            loading="lazy"
            decoding="async"
          />
          <small v-if="method === 'bank_transfer' || method === 'cash'" aria-hidden="true">
            {{ t(`blockCatalog.payments.${method}`) }}
          </small>
        </span>
      </template>
    </v-tooltip>
    <span v-if="!methods.length && preview" class="text-body-small">
      {{ t("blockCatalog.selectPayments") }}
    </span>
  </div>
</template>

<style scoped>
.payment-icons {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--store-text);
}
.payment-icons__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: calc(var(--payment-icon-size) * 2);
  min-height: calc(var(--payment-icon-size) + 16px);
  max-width: 100%;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #111827;
  box-sizing: border-box;
}
.payment-icons__badge img {
  display: block;
  width: auto;
  height: var(--payment-icon-size);
  max-width: calc(var(--payment-icon-size) * 3);
  object-fit: contain;
}
.payment-icons__badge small {
  font-size: 10px;
}
</style>
