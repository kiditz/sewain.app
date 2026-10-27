<script setup lang="ts">
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import CheckoutFlow from "~/features/storefront-checkout/components/CheckoutFlow.vue";
import type { StorefrontBlock } from "~/types/storefront";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
}>();

const headerBlocks = computed(() =>
  props.blocks.filter((block) => ["EYEBROW", "HEADING", "TEXT"].includes(block.type)),
);
</script>

<template>
  <section class="checkout-template-section">
    <div v-if="headerBlocks.length" class="checkout-template-section__header">
      <BlockRenderer
        :blocks="headerBlocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
    </div>

    <div v-if="preview" class="checkout-template-preview">
      <div class="checkout-template-preview__form">
        <div class="checkout-template-preview__field" />
        <div class="checkout-template-preview__field" />
        <div class="checkout-template-preview__field checkout-template-preview__field--large" />
        <div class="checkout-template-preview__payment">
          <v-icon icon="mdi-credit-card-outline" />
          <span>{{ $t("ui.payment_options_and_order_total") }}</span>
        </div>
      </div>
      <div class="checkout-template-preview__summary">
        <strong>{{ $t("checkout.summaryTitle") }}</strong>
        <div />
        <div />
        <div />
      </div>
    </div>
    <CheckoutFlow v-else :show-header="false" />
  </section>
</template>

<style scoped>
.checkout-template-section {
  width: 100%;
  color: var(--store-text);
}

.checkout-template-section__header {
  max-width: var(--store-width, 1200px);
  margin: 0 auto 24px;
  padding: 24px 24px 0;
}

.checkout-template-preview {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  max-width: var(--store-width, 1200px);
  margin: 0 auto;
  padding: 24px;
}

.checkout-template-preview__form,
.checkout-template-preview__summary {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 22px;
  border: 1px solid color-mix(in srgb, var(--store-text) 14%, transparent);
  border-radius: var(--store-radius);
  background: var(--store-surface);
}

.checkout-template-preview__field,
.checkout-template-preview__summary div {
  height: 42px;
  border: 1px solid color-mix(in srgb, var(--store-text) 13%, transparent);
  border-radius: 8px;
}

.checkout-template-preview__field--large {
  height: 88px;
}

.checkout-template-preview__payment {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--store-primary) 8%, transparent);
}

@media (max-width: 720px) {
  .checkout-template-preview {
    grid-template-columns: 1fr;
  }
}
</style>
