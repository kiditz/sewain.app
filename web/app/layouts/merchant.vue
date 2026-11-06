<script setup lang="ts">
import DashboardAppBar from "~/components/dashboard/AppBar.vue";
import DashboardNavigation from "~/components/dashboard/Navigation.vue";

const { loadRegionalSettings, regional } = useSettings();
const { setLocale } = useI18n();
const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}`;
await callOnce(`merchant-regional:${scope}`, async () => {
  await loadRegionalSettings();
}, { mode: "navigation" });
await setLocale(regional.value.locale);

</script>

<template>
  <v-app>
    <dashboard-navigation />
    <dashboard-app-bar />
    
    <v-main class="page-wrapper">
      <v-container fluid class="pa-0">
        <snackbar/>    
        <slot />
      </v-container>
    </v-main>
  </v-app>
</template>
