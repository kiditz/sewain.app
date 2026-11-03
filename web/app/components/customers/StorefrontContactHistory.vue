<script setup lang="ts">
import type { PageResponse } from '~/types/pagination';
import ConfirmDeleteDialog from '~/components/ConfirmDeleteDialog.vue';
interface ContactEntry { id: string; customerId: string; kind: string; name: string; email: string; message: string; consent: boolean; createdAt: string }
const props = defineProps<{ customerId: string }>();
const api = useApi();
const { t } = useI18n();
const { formatDate } = useSettings();
const snackbar = useSnackbarStore();
const page = ref(1);
const deleting = ref<ContactEntry | null>(null);
const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? 'guest'}`;
watch(() => props.customerId, () => { page.value = 1; });
const { data, pending, error, refresh } = await useAsyncData(
  () => `customer-contact-history:${scope}:${props.customerId}:${page.value}`,
  () => api<PageResponse<ContactEntry>>('/storefront/submissions', { query: { customerId: props.customerId, page: page.value - 1, size: 10 } }),
);
const entries = computed(() => data.value?.items ?? []);
const total = computed(() => data.value?.totalItems ?? 0);
async function remove() {
  if (!deleting.value) return;
  const id = deleting.value.id;
  deleting.value = null;
  try {
    await api(`/storefront/submissions/${id}`, { method: 'DELETE' });
    await refresh();
    page.value = Math.min(page.value, Math.max(1, Math.ceil(total.value / 10)));
  } catch { snackbar.openSnackbar({ message: t('storefrontForms.failed'), type: 'error' }); }
}
</script>
<template>
  <v-card variant="outlined" rounded="lg">
    <v-card-title class="pa-4">{{ t('customerContacts.history') }}</v-card-title>
    <v-divider />
    <v-card-text>
      <div v-if="error" class="d-flex align-center ga-3"><span>{{ t('storefrontForms.loadFailed') }}</span><v-btn variant="text" @click="refresh()">{{ t('themeLibrary.retry') }}</v-btn></div>
      <v-progress-linear v-else-if="pending" indeterminate />
      <p v-else-if="!entries.length" class="text-medium-emphasis">{{ t('customerContacts.empty') }}</p>
      <article v-for="entry in entries" :key="entry.id" class="contact-entry">
        <div class="d-flex align-center flex-wrap ga-2">
          <v-chip size="small" variant="tonal">{{ t(entry.kind === 'CONTACT' ? 'storefrontForms.contact' : 'storefrontForms.signup') }}</v-chip>
          <strong>{{ entry.name || entry.email }}</strong><span class="text-body-small text-medium-emphasis">{{ formatDate(entry.createdAt) }}</span>
          <v-spacer /><v-btn icon="mdi-delete-outline" variant="text" size="small" :aria-label="t('common.remove')" @click="deleting = entry" />
        </div>
        <p v-if="entry.message" class="contact-entry__message">{{ entry.message }}</p>
        <p v-if="entry.kind === 'EMAIL_SIGNUP' && entry.consent" class="text-body-small text-medium-emphasis mt-2">{{ t('customerContacts.consentRecorded') }}</p>
      </article>
      <v-pagination v-if="total > 10" v-model="page" :length="Math.ceil(total / 10)" class="mt-4" />
    </v-card-text>
    <ConfirmDeleteDialog :model-value="!!deleting" :title="t('storefrontForms.deleteTitle')" :message="t('storefrontForms.deleteHint')" @update:model-value="!$event && (deleting = null)" @confirmed="remove" />
  </v-card>
</template>
<style scoped>
.contact-entry { padding: 16px 0; border-bottom: 1px solid rgba(var(--v-theme-on-surface), .1); }
.contact-entry:last-of-type { border-bottom: 0; }
.contact-entry__message { margin-top: 12px; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.6; }
</style>
