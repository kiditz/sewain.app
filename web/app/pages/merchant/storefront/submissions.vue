<script setup lang="ts">
definePageMeta({ layout: 'merchant', middleware: ['merchant'] });
// Keep earlier inbox links working; customer contacts now live in Customer.
await navigateTo('/merchant/customers', { replace: true });
</script>
<template><div /></template>
