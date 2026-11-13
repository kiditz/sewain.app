<script setup lang="ts">
definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const { t } = useI18n();
useHead({ title: () => t("menu.integrations") });
const capabilityLabels: Record<string, string> = {
  EMAIL_OUTBOUND: 'integrations.outbound',
  EMAIL_INBOUND: 'integrations.inbound',
  WHATSAPP_OUTBOUND: 'integrations.whatsappOutbound',
  WHATSAPP_INBOUND: 'integrations.whatsappInbound',
};
const integration = useEmailIntegration();
const { data: plugins, status, error, refresh } = await useAsyncData("merchant-integrations", integration.plugins);
</script>

<template>
  <v-container fluid class="pa-6">
    <h1 class="text-headline-small mb-2">{{ t("integrations.title") }}</h1>
    <p class="text-body-medium text-medium-emphasis mb-6">{{ t("integrations.subtitle") }}</p>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ t("integrations.loadFailed") }}
      <v-btn variant="text" @click="refresh()">{{ t("integrations.retry") }}</v-btn>
    </v-alert>
    <v-progress-linear v-if="status === 'pending'" indeterminate />
    <v-row>
      <v-col v-for="plugin in plugins || []" :key="plugin.code" cols="12" md="6" lg="4">
        <v-card variant="outlined" rounded="lg" class="pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-4">
            <v-icon :icon="plugin.code === 'whatsapp' ? 'mdi-whatsapp' : 'mdi-email-outline'" size="32" color="primary" />
            <v-chip :color="plugin.enabled ? 'success' : undefined" size="small" variant="tonal">
              {{ t(plugin.enabled ? "integrations.active" : plugin.installed ? "integrations.inactive" : "integrations.notConnected") }}
            </v-chip>
          </div>
          <h2 class="text-title-large mb-2">{{ plugin.name }}</h2>
          <p class="text-body-medium text-medium-emphasis mb-4">{{ plugin.code === 'email' ? t('integrations.emailDescription') : plugin.code === 'whatsapp' ? 'Hubungkan nomor WhatsApp toko untuk mengirim dan menerima pesan teks.' : plugin.description }}</p>
          <v-chip v-for="capability in plugin.capabilities" :key="capability" size="small" variant="outlined" class="me-2 mb-4">
            {{ capabilityLabels[capability] ? t(capabilityLabels[capability]) : capability }}
          </v-chip>
          <div>
            <v-btn v-if="['email', 'whatsapp'].includes(plugin.code)" color="primary" :to="`/merchant/integrations/${plugin.code}`">
              {{ t(plugin.installed ? "integrations.manage" : "integrations.connect") }}
            </v-btn>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
