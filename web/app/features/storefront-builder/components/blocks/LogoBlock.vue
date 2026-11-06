<script setup lang="ts">
import {safeNavigationUrl} from "~/utils/content-security";
import type { StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import { storefrontBrandingKey } from "../../utils/storefront-branding";
import { getPaddingStyle } from "../../utils/block-settings";
import { useStorefront } from "../../composables/useStorefront";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const branding = inject(storefrontBrandingKey, null);
const { current: merchant } = storeToRefs(useMerchantStore());
const { theme } = useStorefront();
const src = computed(() =>
  String(
    branding?.logoUrl.value ||  "",
  ),
);
const alt = computed(() =>
  String(
      branding?.merchantName.value ||
      "Logo",
  ),
);
const url = computed(() => String(props.block.settings.url || "/"));
function dimension(value: unknown, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
const style = computed(() => ({
  width: `${dimension(props.block.settings.width, 140)}px`,
  height: `${dimension(props.block.settings.height, 48)}px`,
  ...getPaddingStyle(
    props.block.settings.padding as StorefrontPadding,
    theme.value,
  ),
}));
</script>
<template>
  <a
    v-if="src || preview"
    :href="safeNavigationUrl(url) || undefined"
    class="block-logo"
    :style="style"
    @click="preview && $event.preventDefault()"
  >
    <img v-if="src" :src="src" :alt="alt" />
    <span v-else class="block-logo__placeholder">Logo</span>
  </a>
</template>
<style scoped>
.block-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  max-width: min(100%, var(--navigation-logo-max-width, 100%));
  max-height: var(--navigation-logo-max-height, none);
  color: var(--store-text);
  text-decoration: none;
}
.block-logo img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.block-logo__placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  border: 1px dashed currentColor;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0.6;
}
</style>
