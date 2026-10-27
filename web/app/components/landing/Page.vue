<template>
  <main class="landing-page">
    <div class="landing-hero-background"></div>
    <landing-app-bar/>
    <landing-hero />
    <landing-features />
    <landing-how-it-works />
    <landing-dashboard-preview />
    <landing-pricing />
    <landing-cta />
  </main>
</template>
<script setup lang="ts">
definePageMeta({
  middleware: "auth",
});
</script>
