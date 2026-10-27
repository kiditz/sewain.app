<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import type {
  Inventory,
  InventoryCommand,
  InventoryUnitStatus,
} from "~/composables/useInventory";
import type { InventoryUnit, ProductVariantSelection } from "~/types/product";
import { CloseOutlined, ScanOutlined } from "@ant-design/icons-vue";

const props = withDefaults(
  defineProps<{
    unit?: Inventory | null;
  }>(),
  { unit: null },
);

const emit = defineEmits<{
  saved: [unit: Inventory];
}>();

const { t } = useI18n();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const { createInventory, updateInventory } = useInventory();

const isEdit = computed(() => !!props.unit?.id);
const variantDialog = ref(false);

const selectedVariant = ref<{
  productName: string;
  variantName: string;
  sku: string;
  inventoryUnits: InventoryUnit[];
} | null>(null);

const statuses: InventoryUnitStatus[] = ["AVAILABLE", "MAINTENANCE", "DAMAGED"];

const statusOptions = computed(() =>
  statuses.map((value) => ({
    title: t(`inventory.status.${value.toLowerCase()}`),
    value,
  })),
);

const schema = toTypedSchema(
  z.object({
    skuBarcode: z
      .string()
      .trim()
      .min(1, t("inventory.form.validation.barcodeRequired"))
      .max(100, t("inventory.form.validation.barcodeMax")),
    serialNumber: z
      .string()
      .max(150, t("inventory.form.validation.serialNumberMax"))
      .nullable(),
    variantId: z.string().nullable(),
    status: z.enum(["AVAILABLE", "RENTED", "MAINTENANCE", "DAMAGED"]),
    conditionNotes: z.string(),
  }),
);

type FormValues = {
  skuBarcode: string;
  serialNumber: string | null;
  variantId: string | null;
  status: InventoryUnitStatus;
  conditionNotes: string;
};

const emptyValues = (): FormValues => ({
  skuBarcode: "",
  serialNumber: null,
  variantId: null,
  status: "AVAILABLE",
  conditionNotes: "",
});

const { handleSubmit, defineField, errors, isSubmitting, resetForm, meta } =
  useForm<FormValues>({
    validationSchema: schema,
    initialValues: emptyValues(),
  });

const [skuBarcode] = defineField("skuBarcode");
const [serialNumber] = defineField("serialNumber");
const [variantId] = defineField("variantId");
const [status] = defineField("status");
const [conditionNotes] = defineField("conditionNotes");
const change = useObjectChanges()
watch(
  () => meta.value.dirty,
  (dirty) => {
    change.setChanges(dirty)
  },
  { immediate: true },
);
watch(
  () => [props.unit] as const,
  ([unit]) => {
    resetForm({
      values: unit
        ? {
            skuBarcode: unit.skuBarcode,
            serialNumber: unit.serialNumber ?? null,
            variantId: unit.variantId,
            status: unit.status,
            conditionNotes: unit.conditionNotes ?? "",
          }
        : emptyValues(),
    });

    selectedVariant.value = unit?.variantId
      ? {
          productName: unit.productName ?? "",
          variantName: unit.variantName ?? "",
          sku: unit.variantSku ?? "",
          inventoryUnits: [],
        }
      : null;
  },
  { immediate: true },
);

const onVariantSelected = ({ product, variant }: ProductVariantSelection) => {
  variantId.value = variant.id;
  selectedVariant.value = {
    productName: product.name,
    variantName: variant.name,
    sku: variant.sku,
    inventoryUnits: variant.inventoryUnits ?? [],
  };
};

const clearVariant = () => {
  variantId.value = null;
  selectedVariant.value = null;
};
const generateBarcode = () => {
  const variant = selectedVariant.value;

  if (!variant) {
    snackbar.openSnackbar({
      type: "error",
      message: "inventory.form.variantRequired",
    });
    return;
  }

  const prefix = slugify(variant.sku || "unit").toUpperCase();
  const barcodePattern = new RegExp(`^${prefix}-(\\d+)$`);

  const lastSequence = variant.inventoryUnits.reduce((max, unit) => {
    const match = unit.skuBarcode.match(barcodePattern);

    return match ? Math.max(max, Number(match[1])) : max;
  }, 0);

  skuBarcode.value = `${prefix}-${String(lastSequence + 1).padStart(4, "0")}`;
};

const onSubmit = handleSubmit(async (values) => {
  const command: InventoryCommand = {
    skuBarcode: values.skuBarcode.trim(),
    serialNumber: values.serialNumber?.trim() || null,
    variantId: values.variantId,
    status: values.status,
    conditionNotes: values.conditionNotes.trim(),
  };

  try {
    const saved = props.unit?.id
      ? await updateInventory(props.unit.id, command)
      : await createInventory(command);

    snackbar.openSnackbar({
      type: "success",
      message: props.unit?.id
        ? "inventory.form.updateSuccess"
        : "inventory.form.createSuccess",
    });
    emit("saved", saved);
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
});
</script>
<template>
  <product-variant-dialog
    v-model="variantDialog"
    :selected-variant-id="variantId"
    @selected="onVariantSelected"
  />
  <v-card rounded="lg" variant="outlined">
    <v-card-title class="d-flex align-center pa-5 ga-2">
      <v-avatar color="primary" variant="tonal" rounded="lg">
        <scan-outlined />
      </v-avatar>
      <div class="text-title-medium">
        {{ isEdit ? t("inventory.edit.title") : t("inventory.add.title") }}
      </div>
    </v-card-title>
    <v-divider />
    <v-card-text class="pa-5">
      <v-form id="inventory-form" @submit.prevent="onSubmit">
        <div class="form-field">
          <v-label class="field-label" :text="t('inventory.form.variant')" />
          <v-card
            variant="outlined"
            density="compact"
            rounded="lg"
            class="pa-3"
          >
            <div class="d-flex align-center ga-3">
              <v-avatar color="primary" variant="tonal" rounded="lg" size="40">
                <v-icon icon="mdi-package-variant" />
              </v-avatar>
              <div class="min-width-0 flex-grow-1">
                <template v-if="selectedVariant">
                  <div class="font-weight-medium text-truncate">
                    {{ selectedVariant.productName }}
                  </div>
                  <div
                    class="text-body-small text-medium-emphasis text-truncate"
                  >
                    {{ selectedVariant.variantName }}
                    <template v-if="selectedVariant.sku">
                      · {{ selectedVariant.sku }}
                    </template>
                  </div>
                </template>
                <div v-else class="text-medium-emphasis">
                  {{ t("inventory.form.variantPlaceholder") }}
                </div>
              </div>
              <v-btn variant="tonal" @click="variantDialog = true">
                {{
                  selectedVariant
                    ? t("inventory.form.changeVariant")
                    : t("inventory.form.selectVariant")
                }}
              </v-btn>
              <v-btn
                v-if="selectedVariant"
                icon="mdi-close"
                size="small"
                variant="text"
                :aria-label="t('inventory.form.clearVariant')"
                @click="clearVariant"
              >
                <close-outlined />
              </v-btn>
            </div>
          </v-card>
          <div class="text-body-small text-medium-emphasis mt-1">
            {{ t("inventory.form.variantHint") }}
          </div>
        </div>
        <div class="form-field">
          <v-label class="field-label" :text="t('inventory.form.barcode')" />
          <v-text-field
            v-model="skuBarcode"
            variant="outlined"
            density="compact"
            :error-messages="errors.skuBarcode"
            hide-details="auto"
          >
            <template #append-inner>
              <v-btn
                size="x-small"
                variant="text"
                color="primary"
                prepend-icon="mdi-auto-fix"
                @click="generateBarcode"
              >
                {{ t("inventory.form.barcodeGenerate") }}
              </v-btn>
            </template>
          </v-text-field>
        </div>
        <div class="form-field">
          <v-label
            class="field-label"
            :text="t('inventory.form.serialNumber')"
          />
          <v-text-field
            v-model="serialNumber"
            variant="outlined"
            density="compact"
            :placeholder="t('inventory.form.serialNumberPlaceholder')"
            :hint="t('inventory.form.serialNumberHint')"
            persistent-hint
            :error-messages="errors.serialNumber"
          />
        </div>

        <div class="form-field">
          <v-label class="field-label" :text="t('inventory.form.status')" />
          <v-select
            v-model="status"
            :items="statusOptions"
            variant="outlined"
            density="compact"
            :error-messages="errors.status"
            hide-details="auto"
          />
        </div>
        <div class="form-field mb-0">
          <v-label
            class="field-label"
            :text="t('inventory.form.conditionNotes')"
          />
          <v-textarea
            v-model="conditionNotes"
            variant="outlined"
            density="compact"
            rows="3"
            auto-grow
            hide-details="auto"
          />
        </div>
      </v-form>
    </v-card-text>
    <v-divider />
    <v-card-actions class="pa-4">
      <v-spacer />
      <v-btn
        color="primary"
        variant="flat"
        type="submit"
        form="inventory-form"
        :loading="isSubmitting"
      >
        {{ t("common.saveChange") }}
      </v-btn>
    </v-card-actions>
  </v-card>
</template>
<style scoped>
.form-field {
  margin-bottom: 20px;
}
.field-label {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
}
.min-width-0 {
  min-width: 0;
}
</style>
