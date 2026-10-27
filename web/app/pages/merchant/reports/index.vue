<script setup lang="ts">
import type { MerchantReportResponse } from "~/types/merchant-report";

definePageMeta({
  layout: "merchant",
  middleware: ["auth", "merchant"],
});

const { t, locale } = useI18n();
const { formatMoney, formatDate } = useSettings();
const { getReport } = useMerchantReport();
const period = ref("30");
const reportTab = ref("rental");
const periods = computed(() => [
  { title: t("reports.period7"), value: "7" },
  { title: t("reports.period30"), value: "30" },
  { title: t("reports.period90"), value: "90" },
]);
const reportTabs = computed(() => [
  { title: t("reports.tabRental"), value: "rental" },
  { title: t("reports.tabProducts"), value: "products" },
  { title: t("reports.tabOrders"), value: "orders" },
]);
const { data: report, pending, error, refresh } = await useAsyncData<MerchantReportResponse>(
  () => `merchant-reports-${period.value}`,
  () => getReport(Number(period.value)),
  { watch: [period] },
);

const summary = computed(() => report.value?.summary);
const changeText = (value?: number, suffix = "%") => {
  if (value === undefined || value === null) return "—";
  const sign = value > 0 ? "+" : "";
  return `${sign}${new Intl.NumberFormat(locale.value, { maximumFractionDigits: 1 }).format(value)}${suffix}`;
};
const metrics = computed(() => {
  const current = summary.value;
  return [
    {
      title: t("reports.metricOrders"),
      value: current?.totalOrders ?? 0,
      change: changeText(current?.ordersChangePercent),
      changeColor: (current?.ordersChangePercent ?? 0) === 0 ? "text-medium-emphasis" : (current?.ordersChangePercent ?? 0) > 0 ? "text-success" : "text-error",
      description: t("reports.previousPeriod"),
      icon: "mdi-clipboard-list-outline",
      color: "primary",
    },
    {
      title: t("reports.metricRevenue"),
      amount: current?.rentalRevenue ?? 0,
      change: changeText(current?.revenueChangePercent),
      changeColor: (current?.revenueChangePercent ?? 0) === 0 ? "text-medium-emphasis" : (current?.revenueChangePercent ?? 0) > 0 ? "text-success" : "text-error",
      description: t("reports.netRentalsPrevious"),
      icon: "mdi-cash-multiple",
      color: "success",
    },
    {
      title: t("reports.metricAverage"),
      amount: current?.averageOrderValue ?? 0,
      change: changeText(current?.averageOrderValueChangePercent),
      changeColor: (current?.averageOrderValueChangePercent ?? 0) === 0 ? "text-medium-emphasis" : (current?.averageOrderValueChangePercent ?? 0) > 0 ? "text-success" : "text-error",
      description: t("reports.previousPeriod"),
      icon: "mdi-chart-box-outline",
      color: "info",
    },
    {
      title: t("reports.metricCancellation"),
      value: `${changeText(current?.cancellationRate, "%")}`,
      change: changeText(current?.cancellationRateChangePoints, t("reports.points")),
      changeColor: (current?.cancellationRateChangePoints ?? 0) === 0 ? "text-medium-emphasis" : (current?.cancellationRateChangePoints ?? 0) < 0 ? "text-success" : "text-error",
      description: t("reports.cancellationChange"),
      icon: "mdi-calendar-remove-outline",
      color: "warning",
    },
  ];
});
const revenueTrend = computed(() => report.value?.revenueTrend ?? []);
const orderStatuses = computed(() => {
  const total = summary.value?.totalOrders ?? 0;
  const colors: Record<string, string> = {
    COMPLETED: "success",
    ACTIVE_RENTAL: "info",
    PENDING_PAYMENT: "warning",
    CONFIRMED: "primary",
    PROCESSING: "primary",
    READY: "info",
    CANCELLED: "error",
    EXPIRED: "default",
  };
  return (report.value?.orderStatuses ?? []).map((item) => ({
    ...item,
    label: t(`orders.status.${item.status}`),
    color: colors[item.status] ?? "default",
    percent: total ? Number(((item.count / total) * 100).toFixed(1)) : 0,
  }));
});
const topProducts = computed(() => report.value?.topProducts ?? []);
const dailyReports = computed(() => report.value?.dailyReports ?? []);
const maxRevenue = computed(() => Math.max(0, ...revenueTrend.value.map((item) => item.revenue)) || 1);
const revenueChange = computed(() => summary.value?.revenueChangePercent ?? 0);

const productHeaders = computed(() => [
  { title: t("reports.product"), key: "name" },
  { title: t("reports.category"), key: "category" },
  { title: t("reports.totalRentals"), key: "rentals" },
  { title: t("reports.revenue"), key: "revenue", align: "end" as const },
  { title: t("reports.utilization"), key: "utilization" },
]);
const dailyHeaders = computed(() => [
  { title: t("reports.date"), key: "date" },
  { title: t("reports.orders"), key: "orders" },
  { title: t("reports.completedColumn"), key: "completed" },
  { title: t("reports.cancelledColumn"), key: "cancelled" },
  { title: t("reports.netRentalRevenue"), key: "revenue", align: "end" as const },
]);

const exportCsv = () => {
  let headers: string[];
  let rows: (string | number)[][];
  if (reportTab.value === "products") {
    headers = [t("reports.product"), t("reports.category"), t("reports.totalRentals"), t("reports.revenue"), `${t("reports.utilization")} (%)`];
    rows = topProducts.value.map((item) => [item.name, item.category, item.rentals, item.revenue, item.utilization]);
  } else if (reportTab.value === "orders") {
    headers = [t("reports.date"), t("reports.orders"), t("reports.completedColumn"), t("reports.cancelledColumn"), t("reports.netRentalRevenue")];
    rows = dailyReports.value.map((item) => [item.date, item.orders, item.completed, item.cancelled, item.revenue]);
  } else {
    headers = [t("reports.date"), t("reports.netRentalRevenue"), t("reports.orderCount")];
    rows = revenueTrend.value.map((item) => [item.date, item.revenue, item.orders]);
  }
  const escapeCsv = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`;
  const csv = [headers, ...rows].map((row) => row.map(escapeCsv).join(",")).join("\r\n");
  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `sewain-report-${reportTab.value}-${period.value}d.csv`;
  link.click();
  URL.revokeObjectURL(url);
};
</script>

<template>
  <v-container fluid class="pa-0 reports-page">
    <div
      class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6"
    >
      <div>
        <h1 class="text-title-large font-weight-bold">
          {{ $t("menu.reports") }}
        </h1>

        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ $t("ui.analisis_performa_rental_dan_perkembangan_bisnismu") }}
        </p>
      </div>

      <div class="d-flex align-center flex-wrap ga-3">
        <v-select
          v-model="period"
          :items="periods"
          item-title="title"
          item-value="value"
          variant="outlined"
          
          hide-details
          style="min-width: 180px"
          :aria-label="$t('finance.period')"
        />

        <v-btn
          variant="outlined"
          prepend-icon="mdi-download"
          :disabled="pending || !!error || !report"
          @click="exportCsv"
        >
          {{ $t("ui.export_csv") }}
        </v-btn>
      </div>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ $t("ui.laporan_gagal_dimuat") }}
      <v-btn variant="text" size="small" @click="refresh">{{ $t("checkout.retry") }}</v-btn>
    </v-alert>
    <v-progress-linear v-else-if="pending" indeterminate color="primary" class="mb-4" />

    <v-row v-if="report" class="mb-2">
      <v-col
        v-for="metric in metrics"
        :key="metric.title"
        cols="12"
        sm="6"
        xl="3"
      >
        <v-card rounded="lg" border flat class="h-100">
          <v-card-text class="pa-5">
            <div class="d-flex align-start justify-space-between mb-4">
              <span class="text-body-medium text-medium-emphasis">
                {{ metric.title }}
              </span>

              <v-avatar
                :color="metric.color"
                variant="tonal"
                rounded="lg"
                size="40"
              >
                <v-icon :icon="metric.icon" />
              </v-avatar>
            </div>

            <div class="text-title-large font-weight-bold mb-2">
              {{
                'amount' in metric
                  ? formatMoney(metric.amount)
                  : metric.value
              }}
            </div>

            <div class="text-body-small">
              <span :class="[metric.changeColor, 'font-weight-bold']">
                {{ metric.change }}
              </span>

              <span class="text-medium-emphasis ml-1">
                {{ metric.description }}
              </span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row v-if="report" class="mb-2">
      <v-col cols="12" xl="8">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-text class="pa-5">
            <div
              class="d-flex flex-wrap align-start justify-space-between ga-3 mb-6"
            >
              <div>
                <h2 class="text-title-medium font-weight-bold">
                  {{ $t("ui.tren_pendapatan") }}
                </h2>

                <p class="text-body-medium text-medium-emphasis mt-1">
                  {{ $t("ui.pendapatan_rental_selama_periode_terpilih") }}
                </p>
              </div>

              <v-chip :color="revenueChange >= 0 ? 'success' : 'warning'" variant="tonal" size="small">
                <v-icon start :icon="revenueChange >= 0 ? 'mdi-trending-up' : 'mdi-trending-down'" />
                {{ changeText(revenueChange) }} {{ $t("ui.dari_periode_sebelumnya") }}
              </v-chip>
            </div>

            <div class="revenue-chart">
              <div class="revenue-y-axis">
                <span>Rp {{ (maxRevenue / 1000000).toFixed(1) }} {{ $t("ui.jt") }}</span>
                <span>Rp {{ (maxRevenue * 0.75 / 1000000).toFixed(1) }} {{ $t("ui.jt") }}</span>
                <span>Rp {{ (maxRevenue * 0.5 / 1000000).toFixed(1) }} {{ $t("ui.jt") }}</span>
                <span>Rp {{ (maxRevenue * 0.25 / 1000000).toFixed(1) }} {{ $t("ui.jt") }}</span>
                <span>{{ $t("ui.rp_0") }}</span>
              </div>

              <div class="revenue-chart-content">
                <div class="revenue-grid">
                  <div v-for="n in 5" :key="n" class="revenue-grid-line" />
                </div>

                <div class="revenue-bars">
                  <div
                    v-for="item in revenueTrend"
                    :key="item.date"
                    class="revenue-column"
                  >
                    <div
                      class="revenue-bar bg-primary"
                      :style="{
                        height: `${(item.revenue / maxRevenue) * 100}%`,
                      }"
                      :title="`${formatMoney(item.revenue)} · ${item.orders} pesanan`"
                    />

                    <span class="revenue-x-label">
                      {{ formatDate(item.date) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <v-divider class="my-5" />

            <v-row>
              <v-col cols="6">
                <div class="text-body-medium text-medium-emphasis mb-1">
                  {{ $t("ui.total_pendapatan") }}
                </div>

                <div class="text-title-medium font-weight-bold">
                  {{ formatMoney(summary?.rentalRevenue ?? 0) }}
                </div>
              </v-col>

              <v-col cols="6">
                <div class="text-body-medium text-medium-emphasis mb-1">
                  {{ $t("orders.stats.total") }}
                </div>

                <div class="text-title-medium font-weight-bold">
                  {{ summary?.totalOrders ?? 0 }} {{ $t("ui.pesanan") }}
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" xl="4">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-text class="pa-5">
            <div class="mb-5">
              <h2 class="text-title-medium font-weight-bold">
                {{ $t("calendar.statusLegend") }}
              </h2>

              <p class="text-body-medium text-medium-emphasis mt-1">
                {{ $t("ui.distribusi_pesanan_berdasarkan_status") }}
              </p>
            </div>

            <div class="d-flex align-center ga-3 mb-6">
              <div class="text-headline-medium font-weight-bold">
                {{ summary?.totalOrders ?? 0 }}
              </div>

              <div class="text-body-medium text-medium-emphasis">
                {{ $t("ui.total_pesanan") }}
              </div>
            </div>

            <div
              v-for="status in orderStatus"
              :key="status.status"
              class="mb-5"
            >
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-body-medium">
                  {{ status.label }}
                </span>

                <span class="text-body-medium font-weight-bold">
                  {{ status.count }}
                </span>
              </div>

              <v-progress-linear
                :model-value="status.percent"
                :color="status.color"
                height="7"
                rounded
              />

              <div class="text-body-small text-medium-emphasis mt-1">
                {{ status.percent.toLocaleString(locale) }}{{ $t("ui.dari_total") }}
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card rounded="lg" border flat class="mb-5">
      <v-card-text class="pa-5">
        <div class="mb-5">
          <h2 class="text-title-medium font-weight-bold">
            {{ $t("ui.performa_produk") }}
          </h2>

          <p class="text-body-medium text-medium-emphasis mt-1">
            {{ $t("ui.produk_dengan_aktivitas_rental_tertinggi_pada_periode_terpilih") }}
          </p>
        </div>

        <v-data-table
          :headers="productHeaders"
          :items="topProducts"
          item-value="id"
          :items-per-page="5"
          
          hover
          class="report-table"
        >
          <template #[`item.name`]="{ item }">
            <div class="font-weight-medium">
              {{ item.name }}
            </div>
          </template>

          <template #[`item.rentals`]="{ item }">
            {{ item.rentals }} {{ $t("ui.kali") }}
          </template>

          <template #[`item.revenue`]="{ item }">
            <span class="font-weight-bold">
              {{ formatMoney(item.revenue) }}
            </span>
          </template>

          <template #[`item.utilization`]="{ item }">
            <div class="d-flex align-center ga-3 utilization-cell">
              <v-progress-linear
                :model-value="item.utilization"
                color="primary"
                height="6"
                rounded
              />

              <span class="text-body-medium text-no-wrap">
                {{ item.utilization }}%
              </span>
            </div>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <v-card rounded="lg" border flat>
      <v-card-text class="pa-5 pb-0">
        <div class="mb-4">
          <h2 class="text-title-medium font-weight-bold">
            {{ $t("ui.detail_laporan") }}
          </h2>

          <p class="text-body-medium text-medium-emphasis mt-1">
            {{ $t("ui.rincian_performa_berdasarkan_kategori_laporan") }}
          </p>
        </div>

        <v-tabs v-model="reportTab" color="primary">
          <v-tab
            v-for="tab in reportTabs"
            :key="tab.value"
            :value="tab.value"
          >
            {{ tab.title }}
          </v-tab>
        </v-tabs>
      </v-card-text>

      <v-divider />

      <v-card-text v-if="reportTab === 'rental'" class="pa-5">
        <v-row>
          <v-col cols="12" sm="4">
            <div class="text-body-medium text-medium-emphasis mb-1">
              {{ $t("ui.produk_disewakan") }}
            </div>

            <div class="text-title-large font-weight-bold">{{ summary?.productsRented ?? 0 }}</div>
          </v-col>

          <v-col cols="12" sm="4">
            <div class="text-body-medium text-medium-emphasis mb-1">
              {{ $t("ui.rata_rata_durasi_rental") }}
            </div>

            <div class="text-title-large font-weight-bold">{{ (summary?.averageRentalDays ?? 0).toLocaleString(locale, { maximumFractionDigits: 1 }) }} {{ $t("orders.create.days") }}</div>
          </v-col>

          <v-col cols="12" sm="4">
            <div class="text-body-medium text-medium-emphasis mb-1">
              {{ $t("ui.pelanggan_unik") }}
            </div>

            <div class="text-title-large font-weight-bold">{{ summary?.uniqueCustomers ?? 0 }}</div>
          </v-col>
        </v-row>
      </v-card-text>

      <v-data-table
        v-else-if="reportTab === 'orders'"
        :headers="dailyHeaders"
        :items="dailyReports"
        item-value="date"
        :items-per-page="5"
        
        class="report-table"
      >
        <template #[`item.date`]="{ item }">
          {{ formatDate(item.date) }}
        </template>

        <template #[`item.revenue`]="{ item }">
          <span class="font-weight-bold">
            {{ formatMoney(item.revenue) }}
          </span>
        </template>
      </v-data-table>

      <v-data-table
        v-else
        :headers="productHeaders"
        :items="topProducts"
        item-value="id"
        :items-per-page="5"
        
        class="report-table"
      >
        <template #[`item.revenue`]="{ item }">
          <span class="font-weight-bold">
            {{ formatMoney(item.revenue) }}
          </span>
        </template>

        <template #[`item.utilization`]="{ item }">
          {{ item.utilization }}%
        </template>
      </v-data-table>
    </v-card>
  </v-container>
</template>

<style scoped>
.reports-page {
  padding-bottom: 32px;
}

.revenue-chart {
  display: flex;
  height: 270px;
  padding-top: 12px;
}

.revenue-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  padding: 0 12px 34px 0;
  color: rgba(var(--v-theme-on-surface), 0.6);
  font-size: 11px;
  white-space: nowrap;
}

.revenue-chart-content {
  position: relative;
  flex: 1;
  min-width: 0;
}

.revenue-grid {
  position: absolute;
  inset: 0 0 34px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.revenue-grid-line {
  width: 100%;
  border-top: 1px dashed rgba(var(--v-border-color), 0.8);
}

.revenue-bars {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: stretch;
  justify-content: space-around;
  gap: 12px;
}

.revenue-column {
  position: relative;
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: flex-end;
  justify-content: center;
}

.revenue-bar {
  width: min(34px, 70%);
  min-height: 3px;
  border-radius: 5px 5px 0 0;
  transition: height 0.2s ease;
}

.revenue-x-label {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(var(--v-theme-on-surface), 0.6);
  font-size: 10px;
  white-space: nowrap;
}

.utilization-cell {
  min-width: 130px;
}

.utilization-cell :deep(.v-progress-linear) {
  max-width: 100px;
}

.report-table :deep(th) {
  white-space: nowrap;
  font-weight: 600 !important;
}

.report-table :deep(td) {
  height: 62px !important;
}

@media (max-width: 600px) {
  .revenue-chart {
    height: 230px;
  }

  .revenue-bars {
    gap: 5px;
  }

  .revenue-x-label {
    font-size: 9px;
  }
}
</style>
