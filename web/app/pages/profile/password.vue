<script setup lang="ts">
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});

const { t } = useI18n();
useHead({
  title: () => t("profile.password.title"),
});
</script>
<template>
  <v-row>
    <v-col cols="12" md="4" lg="3">
      <profile-card />
    </v-col>
    <v-col cols="12" md="8">
      <profile-password-form />
    </v-col>
  </v-row>
</template>
