<script setup lang="ts">
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"],
});
const { t } = useI18n();
useHead({
  title: () => t("profile.title"),
});
const links = [
  {
    title: t("dashboard.title"),
    disabled: false,
    href: "/dashboard",
  },
  {
    title: t("profile.title"),
    disabled: true,
    href: "/profile",
  },
];
</script>
<template>
  <v-row align="center" no-gutters>
    <v-col sm="12" cols="12">
      <v-breadcrumbs class="pa-1 mb-0" :items="links"> </v-breadcrumbs>
      <div class="text-title-large mt-1 mb-3">{{ t("profile.title") }}</div>
    </v-col>
  </v-row>

  <v-row>
    <v-col cols="12" md="4" lg="3">
      <profile-card />
    </v-col>
    <v-col cols="12" md="8">
      <profile-form />
    </v-col>
  </v-row>
</template>
