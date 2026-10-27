<script setup lang="ts">
import {
  BarcodeOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  QrcodeOutlined,
} from "@ant-design/icons-vue";
import JsBarcode from "jsbarcode";
import QrcodeVue from "qrcode.vue";
import type {
  Inventory,
  InventoryUnitStatus,
} from "~/composables/useInventory";

const { t, locale } = useI18n();
const { findInventories, deleteInventory } = useInventory();

definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});

useHead({
  title: t("inventory.title"),
});

const inventory = ref<Inventory[]>([]);
const search = ref("");
const statusFilter = ref<InventoryUnitStatus | "ALL">("ALL");

const page = ref(1);
const itemsPerPage = 10;
const totalItems = ref(0);

const sortBy = ref("createdAt");
const sortDirection = ref<"asc" | "desc">("desc");
const loading = ref(false);
const error = ref(false);

const selectedInventory = ref<Inventory | null>(null);
const detailDialog = ref(false);

const inventoryToDelete = ref<Inventory | null>(null);
const deleteDialog = ref(false);
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
// State baru untuk dialog preview barcode/QR
const barcodeDialog = ref(false);
const activeBarcodeItem = ref<Inventory | null>(null);
const barcodeType = ref<"barcode" | "qrcode">("barcode");

const statusOptions = computed(() => [
  { title: t("inventory.status.all"), value: "ALL" },
  { title: t("inventory.status.available"), value: "AVAILABLE" },
  { title: t("inventory.status.rented"), value: "RENTED" },
  { title: t("inventory.status.maintenance"), value: "MAINTENANCE" },
  { title: t("inventory.status.damaged"), value: "DAMAGED" },
]);

const statusLabels = computed<Record<InventoryUnitStatus, string>>(() => ({
  AVAILABLE: t("inventory.status.available"),
  RENTED: t("inventory.status.rented"),
  MAINTENANCE: t("inventory.status.maintenance"),
  DAMAGED: t("inventory.status.damaged"),
}));

const statusColors: Record<InventoryUnitStatus, string> = {
  AVAILABLE: "success",
  RENTED: "info",
  MAINTENANCE: "warning",
  DAMAGED: "error",
};

const headers = computed(() => [
  { title: t("inventory.table.headers.unit"), key: "serialNumber" },
  {
    title: t("inventory.table.headers.productVariant"),
    key: "productName",
    sortable: false,
  },
  { title: t("inventory.table.headers.barcode"), key: "skuBarcode" },
  {
    title: t("inventory.table.headers.status"),
    key: "status",
    sortable: false,
  },
  {
    title: t("inventory.table.headers.conditionNotes"),
    key: "conditionNotes",
    sortable: false,
  },
  { title: t("inventory.table.headers.lastUpdated"), key: "updatedAt" },
  {
    title: t("inventory.table.headers.actions"),
    key: "actions",
    sortable: false,
    align: "end" as const,
  },
]);

const totalUnits = computed(() => totalItems.value);

const availableUnits = computed(
  () => inventory.value.filter((item) => item.status === "AVAILABLE").length,
);

const rentedUnits = computed(
  () => inventory.value.filter((item) => item.status === "RENTED").length,
);

const attentionUnits = computed(
  () =>
    inventory.value.filter(
      (item) => item.status === "MAINTENANCE" || item.status === "DAMAGED",
    ).length,
);

const openDeleteDialog = (inventory: Inventory) => {
  inventoryToDelete.value = inventory;
  deleteDialog.value = true;
};

const confirmDeleteInventory = async () => {
  if (!inventoryToDelete.value) {
    return;
  }

  try {
    await deleteInventory(inventoryToDelete.value.id);

    deleteDialog.value = false;
    inventoryToDelete.value = null;
    await fetchInventory();
    snackbar.openSnackbar({
      type: "success",
      message: "inventory.delete.success",
    });
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
};

const fetchInventory = async () => {
  loading.value = true;
  error.value = false;

  try {
    const keyword = search.value.trim();

    const result = await findInventories({
      page: page.value - 1,
      size: itemsPerPage,
      sortBy: sortBy.value,
      sortDirection: sortDirection.value,
      ...(keyword
        ? {
            skuBarcode: keyword,
            serialNumber: keyword,
            "variant.name": keyword,
            "variant.sku": keyword,
            "variant.product.name": keyword,
          }
        : {}),
      ...(statusFilter.value !== "ALL" ? { status: statusFilter.value } : {}),
    });

    inventory.value = result.items;
    totalItems.value = result.totalItems;
  } catch {
    inventory.value = [];
    totalItems.value = 0;
    error.value = true;
  } finally {
    loading.value = false;
  }
};

async function handleTableOptions(options: {
  page: number;
  itemsPerPage: number;
  sortBy: { key: string; order?: "asc" | "desc" }[];
}) {
  const nextSort = options.sortBy[0];

  const nextSortBy = nextSort?.key || "createdAt";
  const nextSortDirection = nextSort?.order ?? "desc";

  const sortChanged =
    sortBy.value !== nextSortBy || sortDirection.value !== nextSortDirection;

  sortBy.value = nextSortBy;
  sortDirection.value = nextSortDirection;

  if (sortChanged && page.value === options.page) {
    await fetchInventory();
    return;
  }

  page.value = options.page;

  await fetchInventory();
}

let searchTimeout: ReturnType<typeof setTimeout> | undefined;

watch(search, () => {
  if (searchTimeout) clearTimeout(searchTimeout);

  searchTimeout = setTimeout(() => {
    if (page.value !== 1) {
      page.value = 1;
    }

    fetchInventory();
  }, 300);
});

watch(statusFilter, () => {
  if (page.value !== 1) {
    page.value = 1;
  }

  fetchInventory();
});

onMounted(fetchInventory);

onBeforeUnmount(() => {
  if (searchTimeout) clearTimeout(searchTimeout);
});

function openDetails(item: Inventory) {
  selectedInventory.value = item;
  detailDialog.value = true;
}

function openBarcodeModal(item: Inventory, type: "barcode" | "qrcode") {
  activeBarcodeItem.value = item;
  barcodeType.value = type;
  barcodeDialog.value = true;

  // Render barcode pakai jsbarcode jika tipenya barcode
  if (type === "barcode") {
    nextTick(() => {
      try {
        JsBarcode("#barcode-canvas", item.skuBarcode, {
          format: "CODE128",
          width: 1,
          height: 60,
          displayValue: true, // menampilkan teks string di bawah garis
          fontSize: 14,
          margin: 5,
        });
      } catch (e) {
        console.error("Gagal merender barcode:", e);
      }
    });
  }
}

function printLabel() {
  window.print();
}
function resetFilters() {
  search.value = "";
  statusFilter.value = "ALL";
  page.value = 1;
  sortBy.value = "createdAt";
  sortDirection.value = "desc";
  fetchInventory();
}

function formatDate(value: string | null | undefined) {
  if (!value) return "-";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "-";

  return new Intl.DateTimeFormat(locale.value || "id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <h1 class="text-title-large font-weight-bold">
          {{ t("inventory.title") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("inventory.subtitle") }}
        </p>
      </div>

      <div class="d-flex align-center justify-end ga-2">
        <v-btn
          color="primary"
          prepend-icon="mdi-plus"
          to="/merchant/inventory/create"
        >
          {{ t("inventory.add.title") }}
        </v-btn>
        <v-btn
          prepend-icon="mdi-refresh"
          variant="outlined"
          color="primary"
          @click="resetFilters"
        >
          {{ t("inventory.resetFilter") }}
        </v-btn>
      </div>
    </div>

    <!-- Summary -->
    <v-row class="mb-2">
      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg" border flat class="pa-4 h-100">
          <div class="d-flex align-center justify-space-between">
            <span class="text-body-medium text-medium-emphasis">
              {{ t("inventory.summary.totalUnits") }}
            </span>
            <v-avatar color="primary" variant="tonal" rounded="lg" size="40">
              <v-icon icon="mdi-package-variant-closed" />
            </v-avatar>
          </div>
          <div class="text-headline-medium font-weight-bold mt-3">
            {{ totalUnits }}
          </div>
          <div class="text-body-small text-medium-emphasis mt-1">
            {{ t("inventory.summary.allUnitsSubtitle") }}
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg" border flat class="pa-4 h-100">
          <div class="d-flex align-center justify-space-between">
            <span class="text-body-medium text-medium-emphasis">
              {{ t("inventory.summary.available") }}
            </span>
            <v-avatar color="success" variant="tonal" rounded="lg" size="40">
              <v-icon icon="mdi-check-circle-outline" />
            </v-avatar>
          </div>
          <div class="text-headline-medium font-weight-bold mt-3">
            {{ availableUnits }}
          </div>
          <div class="text-body-small text-medium-emphasis mt-1">
            {{ t("inventory.summary.onThisPage") }}
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg" border flat class="pa-4 h-100">
          <div class="d-flex align-center justify-space-between">
            <span class="text-body-medium text-medium-emphasis">
              {{ t("inventory.summary.rented") }}
            </span>
            <v-avatar color="info" variant="tonal" rounded="lg" size="40">
              <v-icon icon="mdi-calendar-clock-outline" />
            </v-avatar>
          </div>
          <div class="text-headline-medium font-weight-bold mt-3">
            {{ rentedUnits }}
          </div>
          <div class="text-body-small text-medium-emphasis mt-1">
            {{ t("inventory.summary.onThisPage") }}
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg" border flat class="pa-4 h-100">
          <div class="d-flex align-center justify-space-between">
            <span class="text-body-medium text-medium-emphasis">
              {{ t("inventory.summary.needAttention") }}
            </span>
            <v-avatar color="error" variant="tonal" rounded="lg" size="40">
              <v-icon icon="mdi-alert-circle-outline" />
            </v-avatar>
          </div>
          <div class="text-headline-medium font-weight-bold mt-3">
            {{ attentionUnits }}
          </div>
          <div class="text-body-small text-medium-emphasis mt-1">
            {{ t("inventory.summary.maintenanceOrDamagedSubtitle") }}
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- Inventory table -->
    <v-card rounded="lg" border flat>
      <div class="pa-4 pa-md-5">
        <div class="mb-4">
          <h2 class="text-title-medium font-weight-bold">
            {{ t("inventory.table.title") }}
          </h2>
          <p class="text-body-medium text-medium-emphasis mt-1">
            {{ t("inventory.table.unitsFound", { count: totalItems }) }}
          </p>
        </div>

        <v-row>
          <v-col cols="12" md="8">
            <v-text-field
              v-model="search"
              density="compact"
              :label="t('inventory.table.searchLabel')"
              :placeholder="t('inventory.table.searchPlaceholder')"
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
              hide-details
              clearable
            />
          </v-col>

          <v-col cols="12" md="4">
            <v-select
              v-model="statusFilter"
              :items="statusOptions"
              density="compact"
              :label="t('inventory.table.statusLabel')"
              variant="outlined"
              hide-details
            />
          </v-col>
        </v-row>
      </div>

      <v-divider />

      <v-data-table-server
        v-model:page="page"
        :headers="headers"
        :items="inventory"
        :items-length="totalItems"
        :items-per-page="itemsPerPage"
        :loading="loading"
        item-value="id"
        @update:options="handleTableOptions"
      >
        <template #item.serialNumber="{ item }">
          <div class="font-weight-medium text-no-wrap">
            {{ item.serialNumber || "-" }}
          </div>
        </template>

        <template #item.productName="{ item }">
          <div class="font-weight-medium">
            {{ item.productName || "-" }}
          </div>
          <div class="text-body-medium text-medium-emphasis">
            {{ item.variantName || "-" }}
          </div>
        </template>

        <!-- Modifikasi kolom Barcode agar interaktif (bisa diklik untuk preview QR/Barcode) -->
        <template #item.skuBarcode="{ item }">
          <div class="d-flex align-center ga-2">
            <span class="text-body-medium font-weight-medium">{{
              item.skuBarcode
            }}</span>
          </div>
        </template>

        <template #item.status="{ item }">
          <v-chip
            size="small"
            :color="statusColors[item.status]"
            variant="tonal"
          >
            {{ statusLabels[item.status] }}
          </v-chip>
        </template>

        <template #item.conditionNotes="{ item }">
          <span class="text-body-medium">
            {{ item.conditionNotes || "-" }}
          </span>
        </template>

        <template #item.updatedAt="{ item }">
          {{ formatDate(item.updatedAt) }}
        </template>

        <template #item.actions="{ item }">
          <div class="d-flex justify-end">
            <v-btn
              icon
              variant="text"
              size="small"
              :aria-label="t('inventory.details.viewAria')"
              @click="openDetails(item)"
            >
              <EyeOutlined />
            </v-btn>
            <v-btn
              icon
              variant="text"
              size="small"
              color="primary"
              :aria-label="t('inventory.edit.title')"
              :to="`/merchant/inventory/${item.id}`"
            >
              <EditOutlined />
            </v-btn>
            <v-btn
              :disabled="item.variantId != null"
              icon
              variant="text"
              size="small"
              color="error"
              :aria-label="t('inventory.details.viewAria')"
              @click="openDeleteDialog(item)"
            >
              <DeleteOutlined />
            </v-btn>
            <v-btn
              icon
              variant="text"
              size="small"
              :title="t('inventory.barcode.viewLinear')"
              :aria-label="t('inventory.barcode.viewLinear')"
              @click="openBarcodeModal(item, 'barcode')"
            >
              <BarcodeOutlined />
            </v-btn>

            <v-btn
              icon
              variant="text"
              size="small"
              :title="t('inventory.barcode.viewQr')"
              :aria-label="t('inventory.barcode.viewQr')"
              @click="openBarcodeModal(item, 'qrcode')"
            >
              <QrcodeOutlined />
            </v-btn>
          </div>
        </template>

        <template #no-data>
          <div class="py-8 text-center">
            <v-icon
              icon="mdi-package-variant-closed"
              size="40"
              color="medium-emphasis"
              class="mb-2"
            />
            <div class="text-title-medium font-weight-medium">
              {{ t("inventory.table.noData.title") }}
            </div>
            <div class="text-body-medium text-medium-emphasis mt-1">
              {{ t("inventory.table.noData.subtitle") }}
            </div>
            <v-btn
              class="mt-3"
              variant="text"
              color="primary"
              @click="resetFilters"
            >
              {{ t("inventory.resetFilter") }}
            </v-btn>
          </div>
        </template>

       
      </v-data-table-server>
    </v-card>

    <!-- Inventory detail modal -->
    <v-dialog v-model="detailDialog" max-width="560">
      <v-card v-if="selectedInventory" rounded="lg">
        <v-card-title class="d-flex align-center justify-space-between pa-5">
          <div>
            <div class="text-title-medium">
              {{ t("inventory.details.title") }}
            </div>
            <div class="text-body-medium text-medium-emphasis mt-1">
              {{
                selectedInventory.serialNumber ||
                t("inventory.details.noSerialNumber")
              }}
            </div>
          </div>

          <v-btn
            icon="mdi-close"
            variant="text"
            :aria-label="t('inventory.details.closeAria')"
            @click="detailDialog = false"
          />
        </v-card-title>

        <v-divider />

        <v-card-text class="pa-5">
          <div class="text-overline text-medium-emphasis">
            {{ t("inventory.details.product") }}
          </div>
          <div class="text-title-medium font-weight-bold">
            {{ selectedInventory.productName || "-" }}
          </div>
          <div class="text-body-medium text-medium-emphasis">
            {{ selectedInventory.variantName || "-" }}
          </div>

          <v-divider class="my-4" />

          <v-row>
            <v-col cols="12" sm="6">
              <div class="text-body-small text-medium-emphasis">
                {{ t("inventory.details.serialNumber") }}
              </div>
              <div class="text-body-medium font-weight-medium">
                {{ selectedInventory.serialNumber || "-" }}
              </div>
            </v-col>

            <v-col cols="12" sm="6">
              <div class="text-body-small text-medium-emphasis">
                {{ t("inventory.details.barcode") }}
              </div>
              <div class="text-body-medium font-weight-medium">
                {{ selectedInventory.skuBarcode }}
              </div>
            </v-col>

            <v-col cols="12" sm="6" class="mt-4">
              <div class="text-body-small text-medium-emphasis">
                {{ t("inventory.details.createdAt") }}
              </div>
              <div class="text-body-medium font-weight-medium">
                {{ formatDate(selectedInventory.createdAt) }}
              </div>
            </v-col>

            <v-col cols="12" sm="6" class="mt-4">
              <div class="text-body-small text-medium-emphasis">
                {{ t("inventory.details.updatedAt") }}
              </div>
              <div class="text-body-medium font-weight-medium">
                {{ formatDate(selectedInventory.updatedAt) }}
              </div>
            </v-col>

            <v-col cols="12" class="mt-4">
              <div class="text-body-small text-medium-emphasis mb-2">
                {{ t("inventory.details.status") }}
              </div>
              <v-chip
                :color="statusColors[selectedInventory.status]"
                variant="tonal"
                size="small"
              >
                {{ statusLabels[selectedInventory.status] }}
              </v-chip>
            </v-col>

            <v-col cols="12" class="mt-4">
              <div class="text-body-small text-medium-emphasis mb-2">
                {{ t("inventory.details.conditionNotes") }}
              </div>
              <div class="text-body-medium" style="white-space: pre-wrap">
                {{
                  selectedInventory.conditionNotes ||
                  t("inventory.details.noNotes")
                }}
              </div>
            </v-col>
          </v-row>
        </v-card-text>

        <v-divider />

        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn variant="tonal" @click="detailDialog = false">
            {{ t("inventory.details.close") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Modal Preview Barcode / QR Code & Cetak Label -->
    <v-dialog v-model="barcodeDialog" max-width="700">
      <v-card v-if="activeBarcodeItem" rounded="lg" class="text-center pa-4">
        <v-card-title class="text-title-medium justify-space-between d-flex">
          <span>{{
            barcodeType === "barcode" ? "Linear Barcode" : "QR Code"
          }}</span>
          <v-btn
            icon="mdi-close"
            variant="text"
            size="small"
            @click="barcodeDialog = false"
          />
        </v-card-title>

        <v-card-text>
          <!-- Area yang akan dicetak (printableRef) -->
          <div id="printable-label">
            <div class="my-2 font-weight-bold text-subtitle-1">
              {{ activeBarcodeItem.productName }}
            </div>
            <div class="text-body-2 text-medium-emphasis mb-3">
              {{ activeBarcodeItem.variantName }}
              <span v-if="activeBarcodeItem.serialNumber"
                >| SN: {{ activeBarcodeItem.serialNumber }}</span
              >
            </div>

            <div
              class="bg-white pa-4 rounded-lg d-flex flex-column align-center justify-center border"
            >
              <!-- Jika tipe Barcode, render via Canvas JsBarcode -->
              <div v-show="barcodeType === 'barcode'">
                <canvas id="barcode-canvas"></canvas>
              </div>

              <!-- Jika tipe QR Code, render via qrcode.vue -->
              <div
                v-show="barcodeType === 'qrcode'"
                class="d-flex flex-column align-center"
              >
                <qrcode-vue
                  :value="activeBarcodeItem.skuBarcode"
                  :size="140"
                  level="M"
                />
                <div class="font-weight-mono text-body-2 font-weight-bold mt-2">
                  {{ activeBarcodeItem.skuBarcode }}
                </div>
              </div>
            </div>
          </div>
        </v-card-text>

        <v-card-actions class="justify-space-between pa-2">
          <v-btn
            variant="outlined"
            color="primary"
            block
            prepend-icon="mdi-printer"
            @click="printLabel"
          >
            {{ t("common.print") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
  <ConfirmDeleteDialog
    v-model="deleteDialog"
    :title="$t('inventory.delete.confirm.title')"
    :message="$t('inventory.delete.confirm.message')"
    @confirmed="confirmDeleteInventory"
  />
</template>
<style>
@media print {
  /* Paksa orientasi cetak menjadi portrait (tegak) */
  @page {
    size: portrait;
    margin: 0;
  }

  /* Sembunyikan seluruh elemen halaman bawaan dashboard */
  body * {
    visibility: hidden;
  }

  /* Tampilkan hanya area label yang ingin dicetak */
  #printable-label,
  #printable-label * {
    visibility: visible;
  }

  /* Posisikan label pas di tengah atau pojok atas tanpa rotasi */
  #printable-label {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    transform: none !important;
  }
}
</style>
