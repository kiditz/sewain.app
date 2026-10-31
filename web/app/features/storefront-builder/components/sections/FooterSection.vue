<script setup lang="ts">
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import type { SectionLayout, StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import type { Product } from "~/types/product";
import { useStorefront } from "../../composables/useStorefront";
import { getPaddingStyle } from "../../utils/block-settings";
import { footerDefaults } from "../../utils/footer-section";
import { storefrontBrandingKey } from "../../utils/storefront-branding";
const props = defineProps<{ settings: Record<string, unknown>; blocks: StorefrontBlock[]; preview?: boolean; selectedBlockId?: string | null; product?: Product }>();
const { theme } = useStorefront();
const { t } = useI18n();
const branding = inject(storefrontBrandingKey, null);
const { current: merchant } = storeToRefs(useMerchantStore());
const defaults = footerDefaults();
const layout = computed(() => ({ ...defaults.layout, ...props.settings.layout as Partial<SectionLayout> }));
const contentStyle = computed(() => ({
  ...getPaddingStyle({ ...defaults.padding, ...props.settings.padding as StorefrontPadding }, theme.value),
  maxWidth: layout.value.width === "page" ? `${theme.value.layout.contentMaxWidth}px` : undefined,
}));
function escapeText(value: string) { return value.replace(/[&<>"']/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]!); }
function prepare(blocks: StorefrontBlock[]): StorefrontBlock[] {
  return blocks.map(block => {
    const copy = { ...block, settings: { ...block.settings } };
    if (copy.type === "HEADING" && !copy.settings.text) copy.settings.text = escapeText(branding?.merchantName.value || merchant.value?.name || "Your Store");
    if (copy.type === "TEXT") copy.settings.text = String(copy.settings.text || escapeText(t("ui.rent_more_own_less"))).replaceAll("{year}", String(new Date().getFullYear()));
    if (block.blocks) copy.blocks = prepare(block.blocks);
    return copy;
  });
}
const blocks = computed(() => prepare(props.blocks));
</script>
<template>
  <footer class="footer-section">
    <div class="footer-section__inner" :style="contentStyle">
      <BlockRenderer
        :blocks="blocks"
        :layout="layout"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product-context="product"
      />
    </div>
  </footer>
</template>
<style scoped>
.footer-section { container: storefront-footer / inline-size; background: var(--store-secondary); color: var(--store-text); width: 100%; min-width: 0; }
.footer-section__inner { width: 100%; min-width: 0; margin: 0 auto; box-sizing: border-box; }
.footer-section__inner :deep(.builder-block) { min-width: 0; max-width: 100%; }
@container storefront-footer (max-width: 640px) {
  .footer-section__inner :deep(.block-renderer) { flex-wrap: wrap; }
}
</style>
