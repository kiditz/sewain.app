<script setup lang="ts">
import type { PageResponse } from "~/types/pagination";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

type Direction = "INCOME" | "EXPENSE";
type FinanceCategory = "RENTAL_PAYMENT" | "DEPOSIT_RECEIVED" | "RENTAL_REFUND" | "DEPOSIT_REFUND" | "OPERATING_EXPENSE";
type ExpenseCategory = "RENTAL_MAINTENANCE" | "INVENTORY_PURCHASE" | "UTILITIES" | "TRANSPORT" | "MARKETING" | "OTHER";
type PaymentMethod = "CASH" | "BANK_TRANSFER" | "CARD" | "OTHER";
interface FinanceTransaction {
  id: string; sourceType: string; sourceId: string; direction: Direction; category: FinanceCategory;
  description: string; orderId: string | null; orderNumber: string | null; amount: number;
  currency: string; paymentMethod: string | null; expenseCategory: ExpenseCategory | null;
  notes: string | null; occurredAt: string;
}
interface FinanceResponse {
  summary: { cashIn: number; cashOut: number; rentalIncome: number; depositHeld: number };
  cashFlow: { date: string; income: number; expense: number }[];
  transactions: PageResponse<FinanceTransaction>;
}

const { t, locale } = useI18n();
const api = useApi();
const { formatMoney, regional, loadRegionalSettings } = useSettings();
const period = ref("30");
const activeTab = ref("ALL");
const transactionPage = ref(1);
const itemsPerPage = ref(20);
const response = ref<FinanceResponse | null>(null);
const loading = ref(false);
const loadError = ref(false);
const expenseDialog = ref(false);
const savingExpense = ref(false);
const expenseError = ref(false);
const expenseForm = ref({ category: "OTHER" as ExpenseCategory, description: "", amount: null as number | null, date: "", paymentMethod: "CASH" as PaymentMethod, notes: "" });
let requestSequence = 0;

const periods = computed(() => [
  { title: t("finance.period7"), value: "7" },
  { title: t("finance.period30"), value: "30" },
  { title: t("finance.period90"), value: "90" },
]);
const tabs = computed(() => [
  { title: t("finance.allTransactions"), value: "ALL" },
  { title: t("finance.income"), value: "INCOME" },
  { title: t("finance.expense"), value: "EXPENSE" },
]);
const expenseCategories = computed(() => ["RENTAL_MAINTENANCE", "INVENTORY_PURCHASE", "UTILITIES", "TRANSPORT", "MARKETING", "OTHER"].map(value => ({ title: t(`finance.expenseCategories.${value}`), value })));
const paymentMethods = computed(() => ["CASH", "BANK_TRANSFER", "CARD", "OTHER"].map(value => ({ title: t(`finance.paymentMethods.${value}`), value })));
const dateKey = (date: Date) => new Intl.DateTimeFormat("en-CA", { timeZone: regional.value.timezone }).format(date);
const dateFromKey = (value: string) => {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day));
};
const utcDateKey = (date: Date) => [date.getUTCFullYear(), String(date.getUTCMonth() + 1).padStart(2, "0"), String(date.getUTCDate()).padStart(2, "0")].join("-");
const range = computed(() => {
  const today = dateKey(new Date());
  const start = dateFromKey(today);
  start.setUTCDate(start.getUTCDate() - Number(period.value) + 1);
  return { from: utcDateKey(start), to: today };
});
const summary = computed(() => [
  { title: t("finance.cashIn"), amount: response.value?.summary.cashIn ?? 0, icon: "mdi-trending-up", color: "success" },
  { title: t("finance.cashOut"), amount: response.value?.summary.cashOut ?? 0, icon: "mdi-trending-down", color: "error" },
  { title: t("finance.rentalIncome"), amount: response.value?.summary.rentalIncome ?? 0, icon: "mdi-calendar-check-outline", color: "primary" },
  { title: t("finance.depositHeld"), amount: response.value?.summary.depositHeld ?? 0, icon: "mdi-shield-check-outline", color: "warning" },
]);
const transactions = computed(() => response.value?.transactions.items ?? []);
const chartData = computed(() => {
  const map = new Map((response.value?.cashFlow ?? []).map(point => [point.date, point]));
  const points = [];
  const cursor = dateFromKey(range.value.from);
  for (let i = 0; i < Number(period.value); i++) {
    const date = utcDateKey(cursor);
    const item = map.get(date);
    points.push({ date, label: new Intl.DateTimeFormat(locale.value === "id" ? "id-ID" : "en-GB", { day: "numeric", month: "short", timeZone: "UTC" }).format(cursor), income: item?.income ?? 0, expense: item?.expense ?? 0 });
    cursor.setUTCDate(cursor.getUTCDate() + 1);
  }
  // Keep the chart readable for long periods while retaining daily transaction detail in the table.
  return points.length > 30 ? points.filter((_, index) => index % Math.ceil(points.length / 30) === 0 || index === points.length - 1) : points;
});
const chartMax = computed(() => Math.max(1, ...chartData.value.flatMap(point => [point.income, point.expense])));
const transactionHeaders = computed(() => [
  { title: t("finance.date"), key: "occurredAt" },
  { title: t("finance.transaction"), key: "description" },
  { title: t("finance.order"), key: "orderNumber" },
  { title: t("finance.category"), key: "category" },
  { title: t("finance.amount"), key: "amount", align: "end" as const },
]);
const fmtDate = (value: string) => new Intl.DateTimeFormat(locale.value === "id" ? "id-ID" : "en-GB", { day: "numeric", month: "short", year: "numeric", timeZone: regional.value.timezone }).format(new Date(value));
const categoryLabel = (item: FinanceTransaction) => item.expenseCategory ? t(`finance.expenseCategories.${item.expenseCategory}`) : t(`finance.categories.${item.category}`);
const signedAmount = (item: FinanceTransaction) => `${item.direction === "INCOME" ? "+" : "−"}${formatMoney(item.amount)}`;

async function loadFinance() {
  const sequence = ++requestSequence;
  loading.value = true;
  loadError.value = false;
  try {
    const data = await api<FinanceResponse>("merchant/finance", { query: { ...range.value, direction: activeTab.value, page: transactionPage.value - 1, size: itemsPerPage.value } });
    if (sequence === requestSequence) response.value = data;
  } catch {
    if (sequence === requestSequence) loadError.value = true;
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

function openExpenseDialog() {
  expenseError.value = false;
  expenseForm.value = { category: "OTHER", description: "", amount: null, date: dateKey(new Date()), paymentMethod: "CASH", notes: "" };
  expenseDialog.value = true;
}

async function saveExpense() {
  if (!expenseForm.value.description.trim() || !expenseForm.value.amount || expenseForm.value.amount <= 0 || !expenseForm.value.date) return;
  savingExpense.value = true;
  expenseError.value = false;
  try {
    await api("merchant/finance/expenses", { method: "POST", body: expenseForm.value });
    expenseDialog.value = false;
    await loadFinance();
  } catch {
    expenseError.value = true;
  } finally {
    savingExpense.value = false;
  }
}

async function exportTransactions() {
  const allTransactions: FinanceTransaction[] = [];
  const pageSize = 100;
  const totalItems = response.value?.transactions.totalItems ?? 0;
  for (let page = 0; page * pageSize < totalItems; page++) {
    const result = await api<FinanceResponse>("merchant/finance", { query: { ...range.value, direction: activeTab.value, page, size: pageSize } });
    allTransactions.push(...result.transactions.items);
  }
  const headers = [t("finance.date"), t("finance.transaction"), t("finance.order"), t("finance.category"), t("finance.direction"), t("finance.amount")];
  const rows = allTransactions.map(item => [fmtDate(item.occurredAt), item.description, item.orderNumber ?? "", categoryLabel(item), t(`finance.${item.direction.toLowerCase()}`), item.amount]);
  const quote = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`;
  const blob = new Blob(["\uFEFF" + [headers, ...rows].map(row => row.map(quote).join(",")).join("\r\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a"); link.href = url; link.download = "finance-transactions.csv"; link.click(); URL.revokeObjectURL(url);
}

watch([period, activeTab], () => { transactionPage.value = 1; void loadFinance(); });
watch([transactionPage, itemsPerPage], () => { void loadFinance(); });
onMounted(async () => { await loadRegionalSettings(); await loadFinance(); });
</script>

<template>
  <v-container fluid class="pa-0 finance-page">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div><h1 class="text-title-large font-weight-bold">{{ t("finance.title") }}</h1><p class="text-body-medium text-medium-emphasis mt-1">{{ t("finance.subtitle") }}</p></div>
      <div class="d-flex flex-wrap align-center ga-3">
        <v-select v-model="period" :items="periods" item-title="title" item-value="value" variant="outlined" hide-details style="min-width: 190px" :aria-label="t('finance.period')" />
        <v-btn variant="outlined" prepend-icon="mdi-download" :disabled="!transactions.length" @click="exportTransactions">{{ t("finance.export") }}</v-btn>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="openExpenseDialog">{{ t("finance.addExpense") }}</v-btn>
      </div>
    </div>
    <v-alert v-if="loadError" type="error" variant="tonal" class="mb-4">{{ t("finance.loadError") }}</v-alert>
    <v-row class="mb-2">
      <v-col v-for="item in summary" :key="item.title" cols="12" sm="6" xl="3"><v-card rounded="lg" border flat class="h-100"><v-card-text class="pa-5"><div class="d-flex align-start justify-space-between mb-4"><span class="text-body-medium text-medium-emphasis">{{ item.title }}</span><v-avatar :color="item.color" variant="tonal" rounded="lg" size="40"><v-icon :icon="item.icon" /></v-avatar></div><div class="text-title-large font-weight-bold">{{ loading && !response ? "—" : formatMoney(item.amount) }}</div></v-card-text></v-card></v-col>
    </v-row>
    <v-card rounded="lg" border flat class="mb-6"><v-card-text class="pa-5"><div class="d-flex flex-wrap align-start justify-space-between ga-3 mb-5"><div><h2 class="text-title-medium font-weight-bold">{{ t("finance.cashFlow") }}</h2><p class="text-body-medium text-medium-emphasis mt-1">{{ t("finance.cashFlowSubtitle") }}</p></div><div class="d-flex align-center ga-4"><span><i class="legend-dot bg-primary" /> {{ t("finance.income") }}</span><span><i class="legend-dot bg-error" /> {{ t("finance.expense") }}</span></div></div>
      <div class="cashflow-chart"><div class="chart-content"><div class="chart-grid"><i v-for="n in 5" :key="n" class="chart-grid-line" /></div><div class="chart-bars"><div v-for="point in chartData" :key="point.date" class="chart-column"><div class="chart-bar-group"><i class="chart-bar bg-primary" :style="{ height: `${point.income / chartMax * 100}%` }" :title="`${t('finance.income')}: ${formatMoney(point.income)}`" /><i class="chart-bar bg-error" :style="{ height: `${point.expense / chartMax * 100}%` }" :title="`${t('finance.expense')}: ${formatMoney(point.expense)}`" /></div><span class="chart-x-label">{{ point.label }}</span></div></div></div></div>
    </v-card-text></v-card>
    <v-card rounded="lg" border flat><v-card-text class="pa-5 pb-0"><div class="mb-4"><h2 class="text-title-medium font-weight-bold">{{ t("finance.transactions") }}</h2><p class="text-body-medium text-medium-emphasis mt-1">{{ t("finance.transactionsSubtitle") }}</p></div><v-tabs v-model="activeTab" color="primary"><v-tab v-for="tab in tabs" :key="tab.value" :value="tab.value">{{ tab.title }}</v-tab></v-tabs></v-card-text><v-divider />
      <v-data-table-server v-model:page="transactionPage" v-model:items-per-page="itemsPerPage" :headers="transactionHeaders" :items="transactions" :items-length="response?.transactions.totalItems ?? 0" item-value="id" :loading="loading" hover>
        <template #[`item.occurredAt`]='{ item }'><span>{{ fmtDate(item.occurredAt) }}</span></template>
        <template #[`item.description`]='{ item }'><div class="font-weight-medium">{{ item.description }}</div><div v-if="item.notes" class="text-caption text-medium-emphasis">{{ item.notes }}</div></template>
        <template #[`item.orderNumber`]='{ item }'><NuxtLink v-if="item.orderId" :to="`/merchant/orders/${item.orderId}`" class="text-primary text-decoration-none">{{ item.orderNumber ? `#${item.orderNumber}` : t('finance.viewOrder') }}</NuxtLink><span v-else class="text-medium-emphasis">—</span></template>
        <template #[`item.category`]='{ item }'><v-chip size="small" variant="tonal">{{ categoryLabel(item) }}</v-chip></template>
        <template #[`item.amount`]='{ item }'><span class="font-weight-bold text-no-wrap" :class="item.direction === 'INCOME' ? 'text-success' : 'text-error'">{{ signedAmount(item) }}</span></template>
        <template #no-data><div class="py-8 text-center text-medium-emphasis">{{ t("finance.noTransactions") }}</div></template>
      </v-data-table-server>
    </v-card>
    <v-dialog v-model="expenseDialog" max-width="560"><v-card><v-card-title class="pa-5">{{ t("finance.addExpense") }}</v-card-title><v-card-text class="px-5"><v-alert v-if="expenseError" type="error" variant="tonal" class="mb-4">{{ t("finance.saveExpenseError") }}</v-alert><v-select v-model="expenseForm.category" :items="expenseCategories" :label="t('finance.category')" variant="outlined" /><v-text-field v-model="expenseForm.description" :label="t('finance.expenseDescription')" variant="outlined" /><v-text-field v-model.number="expenseForm.amount" :label="t('finance.amount')" type="number" min="0.01" variant="outlined" /><v-text-field v-model="expenseForm.date" :label="t('finance.date')" type="date" variant="outlined" /><v-select v-model="expenseForm.paymentMethod" :items="paymentMethods" :label="t('finance.paymentMethod')" variant="outlined" /><v-textarea v-model="expenseForm.notes" :label="t('finance.notes')" variant="outlined" rows="2" /></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="expenseDialog = false">{{ t("common.cancel") }}</v-btn><v-btn color="primary" :loading="savingExpense" :disabled="!expenseForm.description.trim() || !expenseForm.amount || expenseForm.amount <= 0" @click="saveExpense">{{ t("finance.saveExpense") }}</v-btn></v-card-actions></v-card></v-dialog>
  </v-container>
</template>

<style scoped>
.finance-page { padding-bottom: 32px; }
.legend-dot { display: inline-block; width: 9px; height: 9px; border-radius: 50%; margin-right: 6px; }
.cashflow-chart { display: flex; height: 250px; }
.chart-content { position: relative; flex: 1; min-width: 0; }
.chart-grid { position: absolute; inset: 0 0 30px; display: flex; flex-direction: column; justify-content: space-between; }
.chart-grid-line { border-top: 1px dashed rgba(var(--v-border-color), .8); }
.chart-bars { position: absolute; inset: 0; display: flex; justify-content: space-around; gap: 8px; }
.chart-column { position: relative; display: flex; flex: 1; min-width: 0; align-items: stretch; justify-content: flex-end; }
.chart-bar-group { width: 100%; height: calc(100% - 30px); display: flex; align-items: flex-end; justify-content: center; gap: 3px; }
.chart-bar { display: block; width: min(12px, 35%); min-height: 2px; border-radius: 3px 3px 0 0; }
.chart-x-label { position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); color: rgba(var(--v-theme-on-surface), .6); font-size: 10px; white-space: nowrap; }
</style>
