<script setup lang="ts">
const { t } = useI18n();
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});
useHead({ title: () => t("menu.rentalPlans") });
const links = [
  
  {
    title: t("rentalplan.title"),
    disabled: true,
    href: "/setting/rental-plan",
  },
];
</script>
<template>
<!-- <v-breadcrumbs class="pa-0 mb-0" :items="links"> </v-breadcrumbs> -->
<setting-rental-plan-form />

</template>