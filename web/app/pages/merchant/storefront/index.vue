<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

definePageMeta({
  layout: "merchant",
  middleware: ["merchant"],
});

const storefront = useStorefront();
await storefront.load();
const { theme } = storefront;
const requestUrl = useRequestURL();
const { current: merchant } = storeToRefs(useMerchantStore());

const storefrontUrl = computed(() => {
  if (!merchant.value?.slug) return undefined;

  const customDomain = merchant.value.customDomain?.trim();
  const url = customDomain
    ? new URL(
        customDomain.startsWith("http")
          ? customDomain
          : `https://${customDomain}`,
      )
    : new URL(requestUrl);

  if (!customDomain) {
    url.hostname = requestUrl.hostname.endsWith(".localhost")
      ? `${merchant.value.slug}.sewain.app.localhost`
      : `${merchant.value.slug}.sewain.app`;
  }

  url.pathname = "/";
  url.search = "";
  url.hash = "";
  return url.toString();
});
</script>

<template>
  <v-container class="py-8" style="max-width: 1080px">
    <header class="mb-8">
      <div class="text-overline text-medium-emphasis">
        {{ $t("ui.storefront") }}
      </div>
      <h1 class="text-headline-medium font-weight-bold">
        {{ $t("ui.theme_settings") }}
      </h1>
      <p class="text-body-medium text-medium-emphasis mt-2">
        {{ $t("ui.customize_the_visual_identity_of_your_store") }}
      </p>
    </header>

    <v-card border flat rounded="lg" class="overflow-hidden">
      <div
        class="storefront-theme-preview pa-8 pa-md-12 rounded-lg"
        :style="{
          backgroundColor: theme.colors.background,
          color: theme.colors.text,
        }"
      >
        <div
          class="storefront-theme-preview__surface pa-6 pa-md-8"
          :style="{
            backgroundColor: theme.colors.surface,
            borderRadius: `${theme.layout.borderRadius}px`,
          }"
        >
          <div
            class="text-overline mb-3"
            :style="{ color: theme.colors.primary }"
          >
            {{ theme.name }}
          </div>
          <div
            class="text-h4 font-weight-bold mb-3"
            :style="{ fontFamily: theme.typography.headingFont }"
          >
            {{ $t("ui.storefront") }}
          </div>
          <div
            class="text-body-medium mb-6"
            :style="{ fontFamily: theme.typography.bodyFont }"
          >
            {{ $t("ui.customize_the_visual_identity_of_your_store") }}
          </div>
          <v-btn
            :href="storefrontUrl"
            target="_blank"
            rel="noopener noreferrer"
            :disabled="!storefrontUrl"
            :style="{
              backgroundColor: theme.colors.primary,
              color: theme.colors.surface,
            }"
            rounded="md"
          >
            {{ $t("ui.preview_storefront") }}
          </v-btn>
        </div>
      </div>

      <v-divider />

      <div
        class="d-flex align-center justify-space-between flex-wrap ga-4 pa-5"
      >
        <div>
          <div class="text-title-medium font-weight-medium">
            {{ theme.name }}
          </div>
          <div class="text-body-small text-medium-emphasis">
            {{ $t("ui.theme_settings") }}
          </div>
        </div>
        <div class="d-flex flex-wrap ga-2">
          <v-btn variant="outlined" prepend-icon="mdi-menu" to="/merchant/storefront/menus">Menu</v-btn>
          <v-btn
            variant="outlined"
            prepend-icon="mdi-tune-variant"
            to="/merchant/storefront/theme"
          >
            {{ $t("ui.theme_settings") }}
          </v-btn>
          <v-btn
            color="primary"
            prepend-icon="mdi-pencil-outline"
            to="/merchant/storefront/pages/home"
          >
            {{ $t("ui.customize_theme") }}
          </v-btn>
        </div>
      </div>
    </v-card>
  </v-container>
</template>
