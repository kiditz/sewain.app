<script setup lang="ts">
import type { DataTableHeader } from "vuetify";
import type { Customer, CustomerStatistics } from "~/types/customer";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t } = useI18n();
const router = useRouter();
const { findCustomers, getCustomerStatistics } = useCustomer();
const { formatDate, formatMoney, loadRegionalSettings } = useSettings();

const customers = ref<Customer[]>([]);
const totalItems = ref(0);
const statistics = ref<CustomerStatistics>({ total: 0, active: 0, newThisMonth: 0, repeatCustomers: 0 });
const loading = ref(false);
const errorMessage = ref("");
const keyword = ref("");
const enabled = ref<"ALL" | "true" | "false">("ALL");
const page = ref(1);
const size = ref(10);
const sortBy = ref([{ key: "createdAt", order: "desc" as const }]);
let debounceTimer: ReturnType<typeof setTimeout> | undefined;
let requestSequence = 0;

const statusOptions = computed(() => [
  { title: t("customers.filters.all"), value: "ALL" },
  { title: t("customers.status.active"), value: "true" },
  { title: t("customers.status.inactive"), value: "false" },
]);

const headers = computed<DataTableHeader[]>(() => [
  { title: t("customers.table.customer"), key: "fullName", sortable: true },
  { title: t("customers.table.contact"), key: "contact", sortable: false },
  { title: t("customers.table.orders"), key: "orderCount", sortable: false },
  { title: t("customers.table.totalSpent"), key: "totalSpent", sortable: false, align: "end" },
  { title: t("customers.table.lastOrder"), key: "lastOrderAt", sortable: false },
  { title: t("customers.table.status"), key: "enabled", sortable: true },
]);

const statCards = computed(() => [
  { label: t("customers.stats.total"), value: statistics.value.total, icon: "mdi-account-group-outline", color: "primary" },
  { label: t("customers.stats.active"), value: statistics.value.active, icon: "mdi-account-check-outline", color: "success" },
  { label: t("customers.stats.newThisMonth"), value: statistics.value.newThisMonth, icon: "mdi-account-plus-outline", color: "info" },
  { label: t("customers.stats.repeatCustomers"), value: statistics.value.repeatCustomers, icon: "mdi-repeat", color: "warning" },
]);

async function loadCustomers() {
  const sequence = ++requestSequence;
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await findCustomers({
      page: page.value - 1,
      size: size.value,
      keyword: keyword.value.trim() || undefined,
      enabled: enabled.value === "ALL" ? undefined : enabled.value === "true",
      sortBy: sortBy.value[0]?.key ?? "createdAt",
      direction: sortBy.value[0]?.order ?? "desc",
    });
    if (sequence !== requestSequence) return;
    customers.value = response.items;
    totalItems.value = response.totalItems;
  } catch {
    if (sequence === requestSequence) errorMessage.value = t("customers.loadError");
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

async function loadStatistics() {
  try {
    statistics.value = await getCustomerStatistics();
  } catch {
    errorMessage.value = t("customers.loadError");
  }
}

function initials(fullName: string) {
  return fullName.trim().split(/\s+/).slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join("");
}

watch([page, size, enabled, sortBy], () => void loadCustomers(), { deep: true });
watch(enabled, () => { page.value = 1; });
watch(keyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 1;
    void loadCustomers();
  }, 300);
});

onMounted(async () => {
  await loadRegionalSettings();
  void loadCustomers();
  void loadStatistics();
});
onBeforeUnmount(() => { if (debounceTimer) clearTimeout(debounceTimer); });
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <h1 class="text-headline-small font-weight-bold">{{ t("customers.title") }}</h1>
        <p class="text-body-medium text-medium-emphasis mt-1">{{ t("customers.subtitle") }}</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" to="/merchant/customers/create">
        {{ t("customers.createCustomer") }}
      </v-btn>
    </div>

    <v-row class="mb-2">
      <v-col v-for="stat in statCards" :key="stat.label" cols="12" sm="6" lg="3">
        <v-card rounded="lg" variant="outlined">
          <v-card-text class="d-flex align-center ga-4">
            <v-avatar :color="`${stat.color}-lighten-5`" rounded="lg" size="48">
              <v-icon :icon="stat.icon" :color="`${stat.color}-darken-1`" />
            </v-avatar>
            <div>
              <div class="text-body-medium text-medium-emphasis">{{ stat.label }}</div>
              <div class="text-headline-small font-weight-bold">{{ stat.value }}</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card rounded="lg" variant="outlined">
      <v-card-text>
        <v-row align="center">
          <v-col cols="12" md="7">
            <v-text-field v-model="keyword" :placeholder="t('customers.searchPlaceholder')" prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable />
          </v-col>
          <v-col cols="12" md="3">
            <v-select v-model="enabled" :items="statusOptions" :label="t('customers.filters.status')" variant="outlined" density="compact" hide-details />
          </v-col>
          <v-col cols="12" md="2" class="text-md-end">
            <span class="text-body-medium text-medium-emphasis">{{ t("customers.resultCount", { count: totalItems }) }}</span>
          </v-col>
        </v-row>
      </v-card-text>
      <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mx-4 mb-4">{{ errorMessage }}</v-alert>
      <v-data-table-server
        v-model:page="page"
        v-model:items-per-page="size"
        v-model:sort-by="sortBy"
        :headers="headers"
        :items="customers"
        :items-length="totalItems"
        :items-per-page-options="[10, 25, 50]"
        :loading="loading"
        :no-data-text="t('customers.noCustomers')"
        hover
        item-value="id"
        @click:row="(_event, { item }) => router.push(`/merchant/customers/${item.id}`)"
      >
        <template #item.fullName="{ item }">
          <div class="d-flex align-center ga-3 py-2">
            <v-avatar color="primary-lighten-5" size="40"><span class="text-primary font-weight-bold">{{ initials(item.fullName) }}</span></v-avatar>
            <div>
              <div class="font-weight-medium">{{ item.fullName }}</div>
              <span v-if="item.source && item.source !== 'MANUAL'" class="text-body-small text-medium-emphasis">{{ t(`customerContacts.sources.${item.source}`) }}</span>
              <div class="text-body-small text-medium-emphasis">{{ t("customers.customerSince") }} {{ formatDate(item.createdAt) }}</div>
            </div>
          </div>
        </template>
        <template #item.contact="{ item }">
          <div>{{ item.email }}</div>
          <v-chip v-if="item.emailSubscribed" size="x-small" color="success" class="my-1">{{ t('customerContacts.subscribed') }}</v-chip>
          <div class="text-body-small text-medium-emphasis">{{ item.phone || "—" }}</div>
        </template>
        <template #item.orderCount="{ item }">{{ t("customers.orderCount", { count: item.orderCount }) }}</template>
        <template #item.totalSpent="{ item }"><span class="font-weight-medium">{{ formatMoney(item.totalSpent) }}</span></template>
        <template #item.lastOrderAt="{ item }">{{ formatDate(item.lastOrderAt) }}</template>
        <template #item.enabled="{ item }">
          <v-chip :color="item.enabled ? 'success' : 'default'" variant="tonal" size="small">
            {{ t(`customers.status.${item.enabled ? "active" : "inactive"}`) }}
          </v-chip>
        </template>
      </v-data-table-server>
    </v-card>
  </v-container>
</template>
