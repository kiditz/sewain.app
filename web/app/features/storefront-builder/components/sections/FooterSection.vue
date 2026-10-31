<script setup lang="ts">
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import type { SectionLayout, StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import { footerDefaults } from "../../utils/footer-section";
import { socialLinkHref } from "../../utils/social-links";
import { storefrontBrandingKey } from "../../utils/storefront-branding";
const props = defineProps<{ settings: Record<string, unknown>; blocks: StorefrontBlock[]; preview?: boolean; selectedBlockId?: string | null }>();
const { theme } = useStorefront();
const { t } = useI18n();
const branding = inject(storefrontBrandingKey, null);
const { current: merchant } = storeToRefs(useMerchantStore());
const defaults = footerDefaults();
const layout = computed(() => ({ ...defaults.layout, ...props.settings.layout as Partial<SectionLayout> }));
const blockLayout = computed<SectionLayout>(() => ({ ...layout.value, height: "auto" }));
const style = computed(() => ({ ...getLayoutStyle(layout.value, theme.value), ...getPaddingStyle({ ...defaults.padding, ...props.settings.padding as StorefrontPadding }, theme.value), boxSizing: "border-box" as const }));
function escapeText(value: string) { return value.replace(/[&<>"']/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]!); }
function prepare(blocks: StorefrontBlock[]): StorefrontBlock[] {
  return blocks.filter(block => props.preview || block.type !== "SOCIAL_LINK" || socialLinkHref(block.settings.url)).map(block => {
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
  <footer class="footer-section"><div class="footer-section__inner" :style="style"><BlockRenderer :blocks="blocks" :layout="blockLayout" :preview="preview" :selected-block-id="selectedBlockId" /></div></footer>
</template>
<style scoped>
.footer-section { container: storefront-footer / inline-size; background: var(--store-secondary); color: var(--store-text); margin-top: var(--store-spacing); width: 100%; }
.footer-section__inner { min-width: 0; }
.footer-section__inner :deep(.builder-block) { min-width: 0; max-width: 100%; }
.footer-section__inner :deep([data-block-type="HEADER"]) { width: auto !important; flex: 1 1 auto !important; }
.footer-section__inner :deep(.block-heading) { font-size: 20px; }
@container storefront-footer (max-width: 640px) {
  .footer-section__inner :deep(.block-renderer) { flex-wrap: wrap; }
  .footer-section__inner :deep([data-block-type="HEADER"]) { flex-basis: 100% !important; }
}
</style>
