<script setup lang="ts">
import BlockLabel from "./BlockLabel.vue";
import { storeToRefs } from "pinia";
import { getBlockSettings } from "../../utils/block-settings";
import type { StorefrontBlock } from "~/types/storefront";
import { useStorefrontProductStore } from "~/stores/storefront-product";
const props = defineProps<{ block: StorefrontBlock }>();
const settings = computed(() =>
  getBlockSettings<{
    label?: string;
    rentalLabel?: string;
    depositLabel?: string;
    totalLabel?: string;
    showDuration?: boolean;
    showQuantity?: boolean;
    showDeposit?: boolean;
  }>(props.block),
);
const store = useStorefrontProductStore();
const { rentalSubtotal, depositSubtotal, total, quantity, selectedVariant } =
  storeToRefs(store);
const money = (amount: number) =>
  new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(amount);
</script>
<template>
  <section class="price-summary">
    <h3 class="summary-title">
      <BlockLabel :value="settings.label || 'Ringkasan pembayaran'" />
    </h3>

    <div class="summary-items">
      <div class="summary-row">
        <div class="item-info">
          <span>{{ settings.rentalLabel || "Biaya sewa" }}</span>
          <small v-if="settings.showQuantity !== false">
            {{ quantity }} {{ $t("ui.unit") }}
          </small>
        </div>
        <strong>{{ money(rentalSubtotal) }}</strong>
      </div>
      <div v-if="settings.showDeposit !== false" class="summary-row">
        <div class="item-info">
          <span> {{ settings.depositLabel || "Refundable deposit" }} </span>
          <small>
            {{ money(selectedVariant?.depositAmount ?? 0) }} {{ $t("ui.per_unit") }}
            <template v-if="quantity > 1"> × {{ quantity }} </template>
          </small>
        </div>
        <strong>{{ money(depositSubtotal) }}</strong>
      </div>
    </div>
    <div class="summary-total">
      <span>{{ settings.totalLabel || "Total pembayaran" }}</span>
      <strong>{{ money(total) }}</strong>
    </div>
    <p class="deposit-note">
      {{ $t("ui.refundable_deposit_akan_dikembalikan_setelah_barang_disewa_dikem") }}
    </p>
  </section>
</template>
<style scoped>
.price-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--store-secondary);
  border-radius: var(--store-radius);
  background: var(--store-surface);
  color: var(--store-text);
}
.summary-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.rental-period {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  border-radius: var(--store-radius);
  background: var(--store-secondary);
}
.period-heading,
.period-row,
.summary-row,
.summary-total {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.period-heading,
.period-row,
.summary-row {
  font-size: 13px;
}
.period-dates,
.summary-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.period-heading strong,
.period-row strong,
.summary-row > strong {
  text-align: right;
}
.period-row span,
.item-info small {
  color: var(--store-text);
  opacity: 0.65;
}
.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.item-info small {
  font-size: 11px;
}
.summary-total {
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--store-secondary);
  font-size: 15px;
  font-weight: 600;
}
.summary-total strong {
  font-size: 18px;
  text-align: right;
}
.deposit-note {
  margin: -4px 0 0;
  font-size: 11px;
  line-height: 1.5;
  opacity: 0.65;
}
</style>
