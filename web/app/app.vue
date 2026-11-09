<script setup lang="ts">
import { shouldNoindex } from "#shared/utils/seo-indexing";

const route = useRoute();
useSeoMeta({
  robots: () => shouldNoindex(route.path, Object.keys(route.query)) ? "noindex, follow" : undefined,
});
</script>

<template>
  <div>
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>
