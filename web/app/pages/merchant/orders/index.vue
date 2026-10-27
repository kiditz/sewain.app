<script setup lang="ts">
import type { DataTableHeader } from "vuetify";
import type { MerchantOrder, OrderStatus } from "~/types/order";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t } = useI18n();
const router = useRouter();
const { findOrders } = useOrder();
const { formatMoney, formatDate, loadRegionalSettings } = useSettings();

const orders = ref<MerchantOrder[]>([]);
const totalItems = ref(0);
const loading = ref(false);
const loadError = ref("");
const keyword = ref("");
const status = ref<OrderStatus | "ALL">("ALL");
const page = ref(1);
const size = ref(10);
const sortBy = ref([{ key: "createdAt", order: "desc" as const }]);
let debounceTimer: ReturnType<typeof setTimeout> | undefined;
let requestSequence = 0;

const statusOptions = computed(() => [
  { title: t("orders.filters.all"), value: "ALL" },
  ...(["PENDING_PAYMENT", "CONFIRMED", "PROCESSING", "READY", "ACTIVE_RENTAL", "COMPLETED", "CANCELLED", "EXPIRED"] as OrderStatus[])
    .map((value) => ({ title: t(`orders.status.${value}`), value })),
]);

const headers = computed<DataTableHeader[]>(() => [
  { title: t("orders.table.order"), key: "orderNumber", sortable: true },
  { title: t("orders.table.customer"), key: "customerName", sortable: true },
  { title: t("orders.table.fulfillment"), key: "fulfillmentType", sortable: true },
  { title: t("orders.table.total"), key: "totalAmount", sortable: true, align: "end" },
  { title: t("orders.table.status"), key: "status", sortable: true },
  { title: t("orders.table.createdAt"), key: "createdAt", sortable: true },
]);

const statusColor = (value: OrderStatus) => ({
  PENDING_PAYMENT: "warning", CONFIRMED: "primary", PROCESSING: "info",
  READY: "info", ACTIVE_RENTAL: "deep-purple", COMPLETED: "success",
  CANCELLED: "error", EXPIRED: "default",
}[value]);

async function loadOrders() {
  const sequence = ++requestSequence;
  loading.value = true;
  loadError.value = "";
  try {
    const response = await findOrders({
      page: page.value - 1,
      size: size.value,
      sort: sortBy.value,
      keyword: keyword.value.trim() || undefined,
      status: status.value === "ALL" ? undefined : status.value,
    });
    if (sequence !== requestSequence) return;
    orders.value = response.items;
    totalItems.value = response.totalItems;
  } catch {
    if (sequence !== requestSequence) return;
    loadError.value = t("orders.loadError");
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

watch([page, size, status, sortBy], () => void loadOrders(), { deep: true });
watch(keyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 1;
    void loadOrders();
  }, 300);
});
onMounted(async () => {
  await loadRegionalSettings();
  void loadOrders();
});
onBeforeUnmount(() => { if (debounceTimer) clearTimeout(debounceTimer); });
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <h1 class="text-headline-small font-weight-bold">{{ t("orders.title") }}</h1>
        <p class="text-body-medium text-medium-emphasis mt-1">{{ t("orders.subtitle") }}</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" to="/merchant/orders/create">{{ t("orders.createOrder") }}</v-btn>
    </div>

    <v-card rounded="lg" variant="outlined">
      <v-card-text>
        <v-row align="center">
          <v-col cols="12" md="7">
            <v-text-field v-model="keyword" :placeholder="t('orders.searchPlaceholder')" prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable />
          </v-col>
          <v-col cols="12" md="5">
            <v-select v-model="status" :items="statusOptions" :label="t('orders.filters.status')" variant="outlined" density="compact" hide-details />
          </v-col>
        </v-row>
      </v-card-text>
      <v-alert v-if="loadError" type="error" variant="tonal" density="compact" class="mx-4 mb-4" closable @click:close="loadError = ''">{{ loadError }}</v-alert>
      <v-data-table-server
        v-model:page="page"
        v-model:items-per-page="size"
        v-model:sort-by="sortBy"
        :headers="headers"
        :items="orders"
        :items-length="totalItems"
        :items-per-page-options="[10, 25, 50]"
        :loading="loading"
        hover
        item-value="id"
        @click:row="(_event, { item }) => router.push(`/merchant/orders/${item.id}`)"
      >
        <template #item.orderNumber="{ item }">
          <span class="font-weight-bold">{{ item.orderNumber }}</span>
        </template>
        <template #item.customerName="{ item }">
          <div class="font-weight-medium">{{ item.customerName }}</div>
          <div class="text-body-small text-medium-emphasis">{{ item.customerEmail }}</div>
        </template>
        <template #item.fulfillmentType="{ item }">
          <v-chip size="small" variant="tonal" :prepend-icon="item.fulfillmentType === 'PICKUP' ? 'mdi-storefront-outline' : 'mdi-truck-delivery-outline'">
            {{ t(`orders.fulfillment.${item.fulfillmentType}`) }}
          </v-chip>
        </template>
        <template #item.totalAmount="{ item }"><span class="text-no-wrap">{{ formatMoney(item.totalAmount) }}</span></template>
        <template #item.status="{ item }">
          <v-chip size="small" variant="tonal" :color="statusColor(item.status)">{{ t(`orders.status.${item.status}`) }}</v-chip>
        </template>
        <template #item.createdAt="{ item }"><span class="text-no-wrap">{{ formatDate(item.createdAt) }}</span></template>
        <template #no-data><div class="py-10 text-center text-medium-emphasis">{{ t("orders.noOrders") }}</div></template>
      </v-data-table-server>
    </v-card>
  </v-container>
</template>
