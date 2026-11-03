<script setup lang="ts">
import StorefrontContactHistory from "~/components/customers/StorefrontContactHistory.vue";
import type { Customer } from "~/types/customer";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t } = useI18n();
const route = useRoute();
const { findCustomer, setCustomerStatus } = useCustomer();
const { formatDate, formatMoney, loadRegionalSettings } = useSettings();
const snackbar = useSnackbarStore();
const customerId = computed(() => String(route.params.id));
const statusLoading = ref(false);

const { data: customer, pending, error, refresh } = await useAsyncData<Customer>(
  () => `merchant-customer-${customerId.value}`,
  () => findCustomer(customerId.value),
  { watch: [customerId] },
);

async function toggleStatus() {
  if (!customer.value) return;
  statusLoading.value = true;
  try {
    customer.value = await setCustomerStatus(customer.value.id, !customer.value.enabled);
    snackbar.openSnackbar({ type: "success", message: t("customers.detail.statusSaved") });
  } catch {
    snackbar.openSnackbar({ type: "error", message: t("customers.detail.statusError") });
  } finally {
    statusLoading.value = false;
  }
}

onMounted(async () => {
  await loadRegionalSettings();
  if (error.value) snackbar.openSnackbar({ type: "error", message: t("customers.loadError") });
});
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center ga-3 mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" to="/merchant/customers" :aria-label="t('customers.detail.back')" />
      <div class="flex-grow-1">
        <h1 class="text-headline-small font-weight-bold">{{ customer?.fullName ?? t("customers.detail.title") }}</h1>
        <p class="text-body-medium text-medium-emphasis mt-1">{{ t("customers.detail.subtitle") }}</p>
      </div>
      <v-btn variant="outlined" prepend-icon="mdi-refresh" :loading="pending" @click="refresh">{{ t("customers.detail.refresh") }}</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ t("customers.loadError") }}</v-alert>
    <v-skeleton-loader v-else-if="pending" type="article, table" />
    <template v-else-if="customer">
      <v-row class="mb-1">
        <v-col cols="12" sm="6" lg="3">
          <v-card rounded="lg" variant="outlined"><v-card-text><div class="text-body-small text-medium-emphasis">{{ t("customers.table.orders") }}</div><div class="text-headline-small font-weight-bold">{{ customer.orderCount }}</div></v-card-text></v-card>
        </v-col>
        <v-col cols="12" sm="6" lg="3">
          <v-card rounded="lg" variant="outlined"><v-card-text><div class="text-body-small text-medium-emphasis">{{ t("customers.table.totalSpent") }}</div><div class="text-headline-small font-weight-bold">{{ formatMoney(customer.totalSpent) }}</div></v-card-text></v-card>
        </v-col>
        <v-col cols="12" sm="6" lg="3">
          <v-card rounded="lg" variant="outlined"><v-card-text><div class="text-body-small text-medium-emphasis">{{ t("customers.table.lastOrder") }}</div><div class="text-title-medium font-weight-bold">{{ formatDate(customer.lastOrderAt) }}</div></v-card-text></v-card>
        </v-col>
        <v-col cols="12" sm="6" lg="3">
          <v-card rounded="lg" variant="outlined"><v-card-text><div class="text-body-small text-medium-emphasis">{{ t("customers.customerSince") }}</div><div class="text-title-medium font-weight-bold">{{ formatDate(customer.createdAt) }}</div></v-card-text></v-card>
        </v-col>
      </v-row>

      <v-row>
        <v-col cols="12" lg="8">
          <v-card rounded="lg" variant="outlined">
            <v-card-title class="d-flex align-center justify-space-between pa-4">
              <span>{{ t("customers.detail.information") }}</span>
              <v-chip :color="customer.enabled ? 'success' : 'default'" variant="tonal">
                {{ t(`customers.status.${customer.enabled ? "active" : "inactive"}`) }}
              </v-chip>
            </v-card-title>
            <v-divider />
            <v-card-text>
              <v-row>
                <v-col cols="12" sm="6"><div class="text-body-small text-medium-emphasis">{{ t("customers.create.fullName") }}</div><div class="font-weight-medium">{{ customer.fullName }}</div></v-col>
                <v-col cols="12" sm="6"><div class="text-body-small text-medium-emphasis">{{ t("customers.create.email") }}</div><a :href="`mailto:${customer.email}`">{{ customer.email }}</a></v-col>
                <v-col cols="12" sm="6"><div class="text-body-small text-medium-emphasis">{{ t("customers.create.phone") }}</div><a v-if="customer.phone" :href="`tel:${customer.phone}`">{{ customer.phone }}</a><span v-else>—</span></v-col>
                <v-col cols="12" sm="6"><div class="text-body-small text-medium-emphasis">{{ t("customerContacts.sourceLabel") }}</div><div>{{ t(`customerContacts.sources.${customer.source || 'MANUAL'}`) }}</div></v-col>
                <v-col cols="12" sm="6"><div class="text-body-small text-medium-emphasis">{{ t("customerContacts.emailStatus") }}</div><v-chip size="small" :color="customer.emailSubscribed ? 'success' : 'default'">{{ t(customer.emailSubscribed ? 'customerContacts.subscribed' : 'customerContacts.notSubscribed') }}</v-chip><div v-if="customer.emailConsentAt" class="text-body-small mt-1">{{ formatDate(customer.emailConsentAt) }}</div></v-col>
                <v-col cols="12"><div class="text-body-small text-medium-emphasis">{{ t("customers.create.notes") }}</div><div class="text-pre-wrap">{{ customer.notes || "—" }}</div></v-col>
              </v-row>
            </v-card-text>
            <v-divider />
            <v-card-actions class="justify-end pa-4">
              <v-btn :color="customer.enabled ? 'error' : 'success'" variant="tonal" :loading="statusLoading" @click="toggleStatus">
                {{ t(`customers.detail.${customer.enabled ? "deactivate" : "activate"}`) }}
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
        <v-col cols="12" lg="4">
          <v-card rounded="lg" variant="outlined">
            <v-card-title class="pa-4">{{ t("customers.detail.rentalHistory") }}</v-card-title>
            <v-divider />
            <v-card-text class="text-medium-emphasis">
              {{ customer.orderCount ? t("customers.detail.historySummary", { count: customer.orderCount }) : t("customers.detail.noHistory") }}
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
      <StorefrontContactHistory :customer-id="customer.id" class="mt-6" />
    </template>
  </v-container>
</template>
