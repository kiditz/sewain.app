<script setup lang="ts">
import { VLayout, VThemeProvider } from "vuetify/components";
import { useTheme } from "vuetify";
import { storefrontBrandingKey, storefrontDrawerTargetKey } from "~/features/storefront-builder/utils/storefront-branding";
import type { StorefrontTheme } from "~/types/storefront";

const props = defineProps<{
  theme: StorefrontTheme;
  preview?: boolean;
}>();
const layoutId = `storefront-layout-${useId()}`;
const vuetify = useTheme();
const vuetifyThemeName = `storefront-${useId().replace(/[^a-zA-Z0-9_-]/g, "")}`;
const mode = computed(() => props.theme.mode === "dark" ? "dark" : "light");
watchEffect(() => {
  const colors = props.theme.colors;
  vuetify.themes.value[vuetifyThemeName] = {
    dark: mode.value === "dark",
    colors: {
      primary: colors.primary, secondary: colors.secondary,
      background: colors.background, surface: colors.surface,
      "surface-bright": colors.surface, "surface-light": colors.secondary,
      "surface-variant": colors.secondary,
      "on-background": colors.text, "on-surface": colors.text,
      "on-surface-variant": colors.text,
    },
    variables: {},
  };
});
onUnmounted(() => { delete vuetify.themes.value[vuetifyThemeName]; });
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
  "--store-image-background": props.theme.colors.surface,
  "--store-text": props.theme.colors.text,
  "--store-on-primary": "rgb(var(--v-theme-on-primary))",
  "--store-heading-font": props.theme.typography.headingFont,
  "--store-radius": `${props.theme.layout.borderRadius}px`,
  "--store-width": `${props.theme.layout.contentMaxWidth}px`,
  "--store-spacing": `${props.theme.layout.sectionSpacing}px`,
  fontFamily: props.theme.typography.bodyFont,
  fontSize: `${props.theme.typography.baseFontSize}px`,
  colorScheme: mode.value,
}));

const cartStore = useCartStore();
await cartStore.getCart().catch(() => undefined);
</script>

<template>
  <VThemeProvider :theme="vuetifyThemeName">
  <VLayout :id="layoutId" class="storefront" :class="[`${vuetify.prefix}theme--${vuetifyThemeName}`, { 'storefront--preview': preview }]" :style="{ ...style, overflow: 'visible' }">
    <main class="store-main">
      <slot />
    </main>


  </VLayout>
  </VThemeProvider>
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

.store-main {
  flex: 1 0 auto;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
 .store-main :deep(.storefront-sections) { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.store-main :deep(.builder-section[data-section-type="FOOTER"]) { margin-top: auto; }
</style>
