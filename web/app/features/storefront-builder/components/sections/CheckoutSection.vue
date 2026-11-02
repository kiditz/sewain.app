<script setup lang="ts">
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import CheckoutFlow from "~/features/storefront-checkout/components/CheckoutFlow.vue";
import type { StorefrontBlock, SectionLayout, StorefrontPadding } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { checkoutSectionDefaults } from "../../utils/checkout-layout";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
const props = defineProps<{ settings: Record<string, unknown>; blocks: StorefrontBlock[]; preview?: boolean; selectedBlockId?: string | null }>();
const { theme } = useStorefront();
const defaults = checkoutSectionDefaults();
const layout = computed(() => ({ ...defaults.layout, ...props.settings.layout as Partial<SectionLayout> }));
const sectionStyle = computed(() => ({ ...getLayoutStyle(layout.value, theme.value), flexDirection: "column" as const, alignItems: "stretch", ...getPaddingStyle({ ...defaults.padding, ...props.settings.padding as StorefrontPadding }, theme.value) }));
const contentStyle = computed(() => ({ ...getLayoutStyle({ ...layout.value, width: "full", height: "auto" }, theme.value) }));
const blockLayout = computed<SectionLayout>(() => ({ ...layout.value, direction: "vertical", alignment: "left", position: "top", width: "full", height: "auto" }));
const headers = computed(() => props.blocks.filter(block => block.type === "HEADER"));
const main = computed(() => props.blocks.filter(block => !["HEADER", "CHECKOUT_SUMMARY", "CHECKOUT_SUBMIT"].includes(block.type)));
const side = computed(() => props.blocks.filter(block => ["CHECKOUT_SUMMARY", "CHECKOUT_SUBMIT"].includes(block.type)));
const hasSide = computed(() => side.value.some(block => block.enabled));
</script>
<template>
  <section class="checkout-template-section" :style="sectionStyle">
    <BlockRenderer v-if="headers.some(block => block.enabled)" :blocks="headers" :layout="blockLayout" :preview="preview" :selected-block-id="selectedBlockId" />
    <CheckoutFlow :show-header="false" :preview="preview" embedded :layout-style="contentStyle">
      <div class="checkout-template-section__main" :class="{ 'checkout-template-section__main--full': !hasSide || layout.direction === 'vertical' }">
        <BlockRenderer :blocks="main" :layout="blockLayout" :preview="preview" :selected-block-id="selectedBlockId" />
      </div>
      <div v-if="hasSide" class="checkout-template-section__side" :class="{ 'checkout-template-section__side--full': layout.direction === 'vertical' }">
        <BlockRenderer :blocks="side" :layout="blockLayout" :preview="preview" :selected-block-id="selectedBlockId" />
      </div>
    </CheckoutFlow>
  </section>
</template>
<style scoped>
.checkout-template-section { container: checkout-section / inline-size; width: 100%; min-width: 0; box-sizing: border-box; color: var(--store-text); }
.checkout-template-section__main { flex: 1 1 0; min-width: 0; }
.checkout-template-section__side { flex: 0 1 360px; min-width: 0; }
.checkout-template-section__main--full, .checkout-template-section__side--full { flex: 0 1 auto; width: 100%; }
@container checkout-section (max-width: 900px) {
  .checkout-template-section :deep(.checkout-layout) { flex-direction: column !important; align-items: stretch !important; }
  .checkout-template-section__main, .checkout-template-section__side { flex: 0 1 auto; width: 100%; }
  .checkout-template-section :deep(.checkout-section-heading) { flex-wrap: wrap; }
  .checkout-template-section :deep(.checkout-summary__row), .checkout-template-section :deep(.checkout-summary__total) { flex-wrap: wrap; }
}
</style>
