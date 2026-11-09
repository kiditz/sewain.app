<script setup lang="ts">
import { useMerchantProvisioning, type ProvisioningStatus } from "~/composables/useMerchantProvisioning";
definePageMeta({ layout: "auth", middleware: ["auth"] });
const { t } = useI18n();
const store = useMerchantStore();
const { findMerchants } = useMerchant();
// The directory is public-schema metadata and works before tenant migration finishes.
store.setMerchants(await findMerchants());
if (!store.current) await navigateTo("/merchant/create", { replace: true });
else if (store.current.provisioningStatus === "READY") await navigateTo("/merchant", { replace: true });
const status = ref(store.current?.provisioningStatus ?? "PROVISIONING");
const delayed = ref(false);
const retrying = ref(false);
const entering = ref(false);
const enterFailed = ref(false);
let delayTimer: ReturnType<typeof setTimeout> | undefined;
const merchantId = store.current?.id ?? "";
const { connection, error, retry } = useMerchantProvisioning(merchantId, receive);
watch([connection, error], () => {
  if (connection.value !== "CONNECTED" || error.value) retrying.value = false;
});
async function receive(event: ProvisioningStatus) {
  status.value = event.status;
  retrying.value = false;
  if (store.current?.id === event.merchantId) {
    store.setCurrent({ ...store.current, provisioningStatus: event.status, provisioningVersion: event.version });
  }
  if (event.status !== "READY" || entering.value) return;
  entering.value = true; enterFailed.value = false;
  try {
    store.setMerchants(await findMerchants());
    await navigateTo("/merchant", { replace: true });
  } catch { enterFailed.value = true; }
  finally { entering.value = false; }
}
function retrySetup() {
  if (connection.value !== "CONNECTED") return;
  retrying.value = true; retry();
}
onMounted(() => { delayTimer = setTimeout(() => { delayed.value = true; }, 60000); });
onBeforeUnmount(() => { if (delayTimer) clearTimeout(delayTimer); });
</script>

<template>
  <main class="setup-state" aria-live="polite" :aria-busy="status === 'PROVISIONING'">
    <v-progress-circular v-if="status === 'PROVISIONING'" indeterminate color="primary" size="64" width="5" />
    <v-icon v-else :icon="status === 'READY' ? 'mdi-check-circle-outline' : 'mdi-alert-circle-outline'" :color="status === 'READY' ? 'success' : 'error'" size="64" />
    <h1 class="text-headline-small">{{ t(status === 'FAILED' ? 'provisioning.failedTitle' : status === 'READY' ? 'provisioning.readyTitle' : 'provisioning.title') }}</h1>
    <p>{{ t(status === 'FAILED' ? 'provisioning.failedBody' : status === 'READY' ? 'provisioning.readyBody' : 'provisioning.body') }}</p>
    <v-alert v-if="connection === 'AUTH_REQUIRED'" type="warning" variant="tonal">{{ t('provisioning.authRequired') }} <NuxtLink to="/login">{{ t('provisioning.login') }}</NuxtLink></v-alert>
    <v-alert v-else-if="connection !== 'CONNECTED'" type="info" variant="tonal">{{ t(connection === 'CONNECTING' ? 'provisioning.connecting' : 'provisioning.reconnecting') }}</v-alert>
    <v-alert v-if="error && connection !== 'AUTH_REQUIRED'" type="error" variant="tonal">{{ t('provisioning.requestFailed') }}</v-alert>
    <p v-if="delayed && status === 'PROVISIONING'" class="text-medium-emphasis">{{ t('provisioning.delayed') }}</p>
    <v-btn v-if="status === 'FAILED'" color="primary" :disabled="connection !== 'CONNECTED' || retrying" :loading="retrying" @click="retrySetup">{{ t('provisioning.retry') }}</v-btn>
    <v-alert v-if="enterFailed" type="warning" variant="tonal">{{ t('provisioning.enterFailed') }} <NuxtLink to="/merchant">{{ t('provisioning.continue') }}</NuxtLink></v-alert>
  </main>
</template>

<style scoped>
.setup-state { min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; text-align: center; padding: 32px 20px; }
</style>
