<script setup lang="ts">
import { VLayout } from "vuetify/components";
import { storefrontBrandingKey, storefrontDrawerTargetKey } from "~/features/storefront-builder/utils/storefront-branding";
import type { StorefrontTheme } from "~/types/storefront";

const props = defineProps<{
  theme: StorefrontTheme;
  preview?: boolean;
}>();
const layoutId = `storefront-layout-${useId()}`;
provide(storefrontDrawerTargetKey, `#${layoutId}`);
const { current: merchant } = storeToRefs(useMerchantStore());
const { host } = useSiteContext();
const api = useApi();
const { data: branding } = await useAsyncData(
  `storefront-branding:${host}:${merchant.value?.slug ?? "guest"}`,
  () => api<{ name: string; logoUrl: string | null; faviconUrl: string | null }>("/storefront/branding"),
);
const merchantName = computed(() => branding.value?.name || merchant.value?.name || "Your Store");
const logoUrl = computed(() => branding.value ? branding.value.logoUrl : merchant.value?.storeInformation?.logoUrl);
provide(storefrontBrandingKey, { merchantName, logoUrl });
const faviconUrl = computed(() => branding.value ? branding.value.faviconUrl : merchant.value?.storeInformation?.faviconUrl);

useHead(() => ({
  link: [{ key: "favicon", rel: "icon", type: undefined, href: faviconUrl.value || "/favicon.ico" }],
  titleTemplate: (title) => {
    if (!title || title === "Sewain") return merchantName.value;
    return `${title} | ${merchantName.value}`;
  },
}));

const style = computed(() => ({
  "--store-primary": props.theme.colors.primary,
  "--store-secondary": props.theme.colors.secondary,
  "--store-background": props.theme.colors.background,
  "--store-surface": props.theme.colors.surface,
  "--store-text": props.theme.colors.text,
  "--store-heading-font": props.theme.typography.headingFont,
  "--store-radius": `${props.theme.layout.borderRadius}px`,
  "--store-width": `${props.theme.layout.contentMaxWidth}px`,
  "--store-spacing": `${props.theme.layout.sectionSpacing}px`,
  fontFamily: props.theme.typography.bodyFont,
  fontSize: `${props.theme.typography.baseFontSize}px`,
}));

const cartStore = useCartStore();
await cartStore.getCart().catch(() => undefined);
</script>

<template>
  <component :is="preview ? VLayout : 'div'" :id="layoutId" class="storefront" :class="{ 'storefront--preview': preview }" :style="style">
    <main class="store-main">
      <slot />
    </main>

    <v-footer class="store-footer">
      <v-container class="store-footer__inner">
        <v-row align="center" justify="space-between">
          <v-col cols="12" sm="auto">
            <div class="store-footer__brand">
              {{ merchantName }}
            </div>
            <div class="store-footer__tagline">
              {{ $t("ui.rent_more_own_less") }}
            </div>
          </v-col>

          <v-col cols="12" sm="auto" class="text-sm-right">
            © {{ new Date().getFullYear() }}
          </v-col>
        </v-row>
      </v-container>
    </v-footer>
  </component>
</template>

<style scoped>
.storefront {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--store-background);
  color: var(--store-text);
}

.storefront--preview { min-height: 640px; isolation: isolate; }
.store-footer__inner { max-width: var(--store-width); }

.store-main {
  flex: 1 0 auto;
  min-width: 0;
}

.store-footer {
  margin-top: var(--store-spacing);
  background: var(--store-secondary);
  color: var(--store-text);
}

.store-footer__inner {
  padding-top: 36px;
  padding-bottom: 36px;
}

.store-footer__brand {
  font-weight: 700;
}

.store-footer__tagline {
  opacity: 0.8;
}

</style>
