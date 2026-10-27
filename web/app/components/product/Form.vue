<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import { useForm, useFieldArray } from "vee-validate";
import { DeleteOutlined } from "@ant-design/icons-vue";
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";

import type { Media } from "~/types/media";
import type { RentalPlanTemplate } from "~/types/setting";
import type { Attribute } from "~/types/attribute";
import type {
  DurationUnit,
  InventoryUnitCommand,
  Product,
  ProductCommand,
  ProductStatus,
  ProductVariantCommand,
  RentalPlanCommand,
} from "~/types/product";
import type { Tag } from "~/types/tag";
import type { ProductType } from "~/types/product-type";

const props = defineProps<{
  product?: Product | null;
}>();

const emit = defineEmits<{
  saved: [];
}>();

const openedPanels = ref([0]);

const { t } = useI18n();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const { createProduct, updateProduct } = useProduct();

const durationUnits: DurationUnit[] = ["HOUR", "DAY", "WEEK", "MONTH"];
const attributeMenu = ref(false);

type ProductMode = "single" | "multiple";

// single   : produk tanpa varian (satu varian default, tanpa atribut)
// multiple : produk dengan varian hasil kombinasi atribut
const productMode = ref<ProductMode>("single");
const isSingleProduct = computed(() => productMode.value === "single");
const variantPanels = ref<number[]>([]);

const productModeOptions = computed<
  Array<{
    value: ProductMode;
    icon: string;
    title: string;
    description: string;
  }>
>(() => [
  {
    value: "single",
    icon: "mdi-package-variant-closed",
    title: t("product.form.productMode.single.title"),
    description: t("product.form.productMode.single.description"),
  },
  {
    value: "multiple",
    icon: "mdi-view-grid-outline",
    title: t("product.form.productMode.multiple.title"),
    description: t("product.form.productMode.multiple.description"),
  },
]);

const emptyVariant = (): ProductVariantCommand => ({
  name: "",
  sku: "",
  mediaList: [],
  buyPrice: null,
  depositAmount: 0,
  active: true,
  attributeValueIds: [],
  rentalPlans: [],
  inventoryQuantity: 1,
  inventoryUnits: [],
});

const initialValues: ProductCommand = {
  productTypeId: null,
  tagIds: [],
  name: "",
  slug: "",
  media: "",
  description: "",
  status: "DRAFT",
  // Default: single product, jadi langsung ada satu varian.
  variants: [emptyVariant()],
};

const { schema } = createProductSchema(t);

const {
  handleSubmit,
  defineField,
  errors,
  isSubmitting,
  resetForm,
  values,
  validateField,
} = useForm<ProductCommand>({
  validationSchema: schema,
  initialValues,
});

const [name] = defineField("name");
const [slug] = defineField("slug");
const [media] = defineField("media");
const [description] = defineField("description");
const [status] = defineField("status");
const [productTypeId] = defineField("productTypeId");
const [tagIds] = defineField("tagIds");

const {
  fields: variants,
  update: updateFieldArrayVariant,
  replace: replaceVariant,
  remove: removeVariant,
} = useFieldArray<ProductVariantCommand>("variants");

const requestUrl = useRequestURL();
const { current } = storeToRefs(useMerchantStore());

const permalink = computed(() => {
  const productSlug = slugify(name.value || "");

  return productSlug
    ? `https://${current.value?.slug}.${requestUrl.host}/products/${productSlug}`
    : "";
});

watch(name, (value) => {
  if (!props.product) {
    slug.value = slugify(value || "");
  }
});

// Attributes: data berasal dari ProductAttributeSelector.

const attributeSelector = ref<{
  loadAttributes: () => Promise<void>;
} | null>(null);

const attributes = ref<Attribute[]>([]);
const attributesInitialized = ref(false);

const selectedAttributeIds = ref<string[]>([]);
const selectedValuesByAttribute = ref<Record<string, string[]>>({});

const availableAttributeValues = computed(() =>
  attributes.value.flatMap((attribute) =>
    attribute.values.map((value) => ({
      ...value,
      attributeId: attribute.id,
      attributeName: attribute.name,
    })),
  ),
);

const selectedAttributeGroups = computed(() =>
  selectedAttributeIds.value
    .map((id) => attributes.value.find((attribute) => attribute.id === id))
    .filter((attribute): attribute is Attribute => attribute !== undefined),
);

const syncSelectedAttributes = (product?: Product | null) => {
  if (!product || attributes.value.length === 0) {
    selectedAttributeIds.value = [];
    selectedValuesByAttribute.value = {};
    return;
  }

  const selectedValueIds = new Set(
    product.variants.flatMap((variant) => {
      const mappedVariant = variant as typeof variant & {
        values?: { attributeValueId: string }[];
      };

      return (
        mappedVariant.attributeValueIds ??
        mappedVariant.values?.map((value) => value.attributeValueId) ??
        []
      );
    }),
  );

  const selectedAttributes = attributes.value.filter((attribute) =>
    attribute.values.some((value) => selectedValueIds.has(value.id)),
  );

  selectedAttributeIds.value = selectedAttributes.map(
    (attribute) => attribute.id,
  );

  selectedValuesByAttribute.value = Object.fromEntries(
    selectedAttributes.map((attribute) => [
      attribute.id,
      attribute.values
        .filter((value) => selectedValueIds.has(value.id))
        .map((value) => value.id),
    ]),
  );
};

const onAttributesLoaded = async (loadedAttributes: Attribute[]) => {
  attributes.value = loadedAttributes;

  // Initial load only: populate attribute selections from the product.
  // Do not overwrite the user's current selections on subsequent reloads.
  if (!attributesInitialized.value) {
    attributesInitialized.value = true;
    syncSelectedAttributes(props.product);

    await nextTick();

    if (props.product?.id) {
      saveInitialSnapshot();
    }

    return;
  }

  // Keep selected IDs valid after attribute/value changes.
  const attributeIds = new Set(
    attributes.value.map((attribute) => attribute.id),
  );

  selectedAttributeIds.value = selectedAttributeIds.value.filter((id) =>
    attributeIds.has(id),
  );

  const nextSelections: Record<string, string[]> = {};

  for (const attribute of attributes.value) {
    const selectedIds = new Set(
      selectedValuesByAttribute.value[attribute.id] ?? [],
    );

    nextSelections[attribute.id] = attribute.values
      .filter((value) => selectedIds.has(value.id))
      .map((value) => value.id);
  }

  selectedValuesByAttribute.value = nextSelections;
};

const onAttributeUpdated = (updatedAttribute: Attribute) => {
  const index = attributes.value.findIndex(
    (attribute) => attribute.id === updatedAttribute.id,
  );

  if (index === -1) {
    attributes.value = [...attributes.value, updatedAttribute];
    return;
  }

  attributes.value[index] = updatedAttribute;
};

const { findAttributes } = useAttribute();

const loadProductAttributes = async () => {
  try {
    const loadedAttributes = await findAttributes();
    await onAttributesLoaded(loadedAttributes);
  } catch (error) {
    console.error("Gagal memuat attributes:", error);
  }
};

onMounted(loadProductAttributes)

const onAttributeSaved = async (_attribute: Attribute) => {
  attributeMenu.value = false;
  await loadProductAttributes();
  syncSelectedAttributes(props.product);
  
};

watch(
  selectedAttributeIds,
  (ids) => {
    const selected = new Set(ids);
    const nextSelections = { ...selectedValuesByAttribute.value };

    for (const id of Object.keys(nextSelections)) {
      if (!selected.has(id)) {
        delete nextSelections[id];
      }
    }

    for (const id of ids) {
      if (!nextSelections[id]) {
        nextSelections[id] = [];
      }
    }

    selectedValuesByAttribute.value = nextSelections;
  },
  { deep: true },
);

// Variants

const getAttributeCombinationKey = (ids: string[] | undefined) =>
  [...(ids ?? [])].sort().join("|");

const generateVariants = () => {
  const groups = selectedAttributeGroups.value;

  if (groups.length === 0) {
    snackbar.openSnackbar({
      type: "error",
      message: t("product.attributes.requiredBeforeGeneratingVariants"),
    });
    return;
  }

  const selectedOptions = groups.map((attribute) => {
    const selectedIds = new Set(
      selectedValuesByAttribute.value[attribute.id] ?? [],
    );

    return attribute.values.filter((value) => selectedIds.has(value.id));
  });

  if (selectedOptions.some((options) => options.length === 0)) {
    snackbar.openSnackbar({
      type: "error",
      message: t("product.attributes.selectValuesForEveryAttribute"),
    });
    return;
  }

  let combinations: { id: string; value: string }[][] = [[]];

  for (const options of selectedOptions) {
    combinations = combinations.flatMap((combination) =>
      options.map((option) => [
        ...combination,
        { id: option.id, value: option.value },
      ]),
    );
  }

  const existingByCombination = new Map(
    values.variants.map((variant) => [
      getAttributeCombinationKey(variant.attributeValueIds),
      variant,
    ]),
  );

  const retainedIds = new Set<string>();

  const generatedVariants: ProductVariantCommand[] = combinations.map(
    (combination, index) => {
      const attributeValueIds = combination.map((option) => option.id);
      const key = getAttributeCombinationKey(attributeValueIds);
      const existing = existingByCombination.get(key);
      const variantName = combination.map((option) => option.value).join(" / ");

      if (existing) {
        if (existing.id) retainedIds.add(existing.id);

        return {
          ...existing,
          name: variantName,
          attributeValueIds,
          inventoryUnits: existing.inventoryUnits ?? [],
          inventoryQuantity: existing.inventoryUnits?.length ?? 0,
          rentalPlans: existing.rentalPlans ?? [],
        };
      }

      return {
        ...emptyVariant(),
        name: variantName,
        sku: `${slugify(name.value || "product")}-${slugify(variantName)}-${index + 1}`,
        attributeValueIds,
      };
    },
  );

  const retiredVariants = values.variants
    .filter((variant) => variant.id && !retainedIds.has(variant.id))
    .map((variant) => ({
      ...variant,
      active: false,
      inventoryUnits: variant.inventoryUnits ?? [],
      inventoryQuantity: variant.inventoryUnits?.length ?? 0,
      rentalPlans: variant.rentalPlans ?? [],
    }));

  replaceVariant([...generatedVariants, ...retiredVariants]);

  // snackbar.openSnackbar({
  //   type: "success",
  //   message: t("product.variants.generate.success"),
  // });
};

const updateVariant = (
  variantIndex: number,
  update: Partial<ProductVariantCommand>,
) => {
  const currentVariant = values.variants[variantIndex];
  if (!currentVariant) return;

  updateFieldArrayVariant(variantIndex, {
    ...currentVariant,
    ...update,
  });
};

// Product mode (single / multiple)

const hasAttributeValues = (variant: {
  attributeValueIds?: string[];
  values?: { attributeValueId: string }[];
}) => {
  const ids =
    variant.attributeValueIds ??
    variant.values?.map((value) => value.attributeValueId) ??
    [];

  return ids.length > 0;
};

const detectProductMode = (product: Product): ProductMode => {
  const productVariants = product.variants ?? [];

  return productVariants.length <= 1 && !productVariants.some(hasAttributeValues)
    ? "single"
    : "multiple";
};

// Pastikan single product punya tepat satu varian tanpa atribut.
const ensureSingleVariant = () => {
  const [first] = values.variants;

  if (!first) {
    replaceVariant([
      {
        ...emptyVariant(),
        name: name.value || "",
        sku: slug.value || "",
      },
    ]);
    return;
  }

  if (values.variants.length > 1 || first.attributeValueIds.length > 0) {
    replaceVariant([{ ...first, attributeValueIds: [] }]);
  }
};

watch(productMode, (mode) => {
  if (mode === "single") {
    selectedAttributeIds.value = [];
    selectedValuesByAttribute.value = {};
    ensureSingleVariant();
    return;
  }

  // Multiple (produk baru): varian dibuat dari atribut yang dipilih.
  if (!props.product?.id) {
    replaceVariant([]);
  }
});

// Single product: panel varian selalu terbuka dan tidak bisa ditutup.
watch(
  isSingleProduct,
  (single) => {
    variantPanels.value = single ? [0] : [];
  },
  { immediate: true },
);

// Single product: nama & SKU varian mengikuti nama & slug produk
// selama belum diubah manual.
watch(name, (value, oldValue) => {
  if (!isSingleProduct.value) return;

  const variant = values.variants[0];
  if (!variant) return;

  if (!variant.name || variant.name === oldValue) {
    updateVariant(0, { name: value || "" });
  }
});

watch(slug, (value, oldValue) => {
  if (!isSingleProduct.value || props.product?.id) return;

  const variant = values.variants[0];
  if (!variant) return;

  if (!variant.sku || variant.sku === oldValue) {
    updateVariant(0, { sku: value || "" });
  }
});

// Inventory

const generateVariantInventory = (variantIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  const targetQuantity = Math.max(
    0,
    Math.floor(variant.inventoryQuantity || 0),
  );

  const units = [...(variant.inventoryUnits ?? [])];

  if (targetQuantity < units.length) {
    for (
      let i = units.length - 1;
      i >= 0 && units.length > targetQuantity;
      i--
    ) {
      if (!units[i]?.id) {
        units.splice(i, 1);
      }
    }

    if (units.length > targetQuantity) {
      snackbar.openSnackbar({
        type: "error",
        message: t("product.inventory.existingUnitsCannotBeRemoved"),
      });
    }
  }

  const existingBarcodes = new Set(units.map((unit) => unit.skuBarcode));
  const prefix = slugify(variant.sku || "unit").toUpperCase();
  let sequence = 1;

  while (units.length < targetQuantity) {
    const skuBarcode = `${prefix}-${String(sequence).padStart(4, "0")}`;
    sequence++;

    if (existingBarcodes.has(skuBarcode)) continue;

    existingBarcodes.add(skuBarcode);

    units.push({
      skuBarcode,
      conditionNotes: "Good",
      status: "AVAILABLE",
    });
  }

  updateVariant(variantIndex, {
    inventoryUnits: units,
    inventoryQuantity: units.length,
  });
};

const addInventoryUnit = (variantIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;
  const prefix = slugify(variant.sku || "unit").toUpperCase();
  let sequence = 1;
  const skuBarcode = `${prefix}-${String(sequence).padStart(4, "0")}`;
  updateVariant(variantIndex, {
    inventoryUnits: [
      ...(variant.inventoryUnits ?? []),
      {
        skuBarcode: skuBarcode,
        conditionNotes: "Good",
        status: "AVAILABLE",
      },
    ],
    inventoryQuantity: (variant.inventoryUnits?.length ?? 0) + 1,
  });
};

const removeInventoryUnit = (variantIndex: number, unitIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  updateVariant(variantIndex, {
    inventoryUnits: variant.inventoryUnits?.filter(
      (_, index) => index !== unitIndex,
    ),
    inventoryQuantity: Math.max(0, variant.inventoryQuantity - 1),
  });
};

type InventoryUnitField = "skuBarcode" | "conditionNotes";

const onInventoryUnitChange = async (
  value: string,
  variantIndex: number,
  unitIndex: number,
  field: InventoryUnitField,
) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  const inventoryUnits = [...(variant.inventoryUnits ?? [])];
  const unit = inventoryUnits[unitIndex];
  if (!unit) return;

  inventoryUnits[unitIndex] = {
    ...unit,
    [field]: value,
  };

  updateVariant(variantIndex, { inventoryUnits });

  await validateField(
    `variants[${variantIndex}].inventoryUnits[${unitIndex}].${field}`,
  );
};

// Rental plans

const setting = useSettings();
const settingPlan = await setting.findByKey("RENTAL_PLAN");
const rentalPlanTemplates = settingPlan.value as RentalPlanTemplate[];

const addRentalPlan = (variantIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  updateVariant(variantIndex, {
    rentalPlans: [
      ...(variant.rentalPlans ?? []),
      {
        durationUnit: "DAY",
        durationValue: 1,
        rentalFee: 0,
        active: true,
      },
    ],
  });
};

const removeRentalPlan = (variantIndex: number, planIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  updateVariant(variantIndex, {
    rentalPlans: (variant.rentalPlans ?? []).filter(
      (_, index) => index !== planIndex,
    ),
  });
};

const clearRentalPlans = (variantIndex: number) => {
  updateVariant(variantIndex, { rentalPlans: [] });
};

const generateRentalPlans = (variantIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  if (variant.buyPrice == null || variant.buyPrice <= 0) {
    snackbar.openSnackbar({
      type: "error",
      message: t("product.variant.buyPrice.requiredForRentalPlans"),
    });
    return;
  }

  updateVariant(variantIndex, {
    rentalPlans: rentalPlanTemplates.map((template) => ({
      durationUnit: template.durationUnit,
      durationValue: template.durationValue,
      rentalFee: Math.round((variant.buyPrice! * template.percentage) / 100),
      active: true,
    })),
  });
};

// Money

const { loadRegionalSettings, formatMoney, parseMoney } = setting;
await loadRegionalSettings();

const onVariantMoneyInput = (
  value: string,
  variantIndex: number,
  field: "buyPrice" | "depositAmount",
) => {
  updateVariant(variantIndex, {
    [field]: parseMoney(value) ?? 0,
  });
};

const onRentalFeeInput = (
  value: string,
  variantIndex: number,
  planIndex: number,
) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  const plans = [...(variant.rentalPlans ?? [])];
  const plan = plans[planIndex];
  if (!plan) return;

  plans[planIndex] = {
    ...plan,
    rentalFee: parseMoney(value) ?? 0,
  };

  updateVariant(variantIndex, { rentalPlans: plans });
};

// Media

const uploadProductMedia = ref(false);
const uploadVariantMedia = ref(false);
const activeVariantIndex = ref<number | null>(null);

const onProductMediaSelected = (selected: Media | Media[]) => {
  const item = Array.isArray(selected) ? selected[0] : selected;
  media.value = item?.url ?? "";
};

const openVariantMedia = (index: number) => {
  activeVariantIndex.value = index;
  uploadVariantMedia.value = true;
};

const onVariantMediaSelected = (selected: Media | Media[]) => {
  const index = activeVariantIndex.value;
  if (index === null) return;

  const variant = values.variants[index];
  if (!variant) return;

  const items = Array.isArray(selected) ? selected : [selected];

  updateVariant(index, {
    mediaList: [...(variant.mediaList ?? []), ...items.map((item) => item.url)],
  });
};

const removeVariantMedia = (variantIndex: number, mediaIndex: number) => {
  const variant = values.variants[variantIndex];
  if (!variant) return;

  updateVariant(variantIndex, {
    mediaList: (variant.mediaList ?? []).filter(
      (_, index) => index !== mediaIndex,
    ),
  });
};

// Change tracking

const initialProductSnapshot = ref<string | null>(null);
const initialFormValues = ref<ProductCommand | null>(null);
const initialSelectedAttributeIds = ref<string[]>([]);
const initialSelectedValuesByAttribute = ref<Record<string, string[]>>({});

const onProductTypeDeleted = (id: string) => {
  if (initialFormValues.value?.productTypeId === id) initialFormValues.value.productTypeId = null;
  if (initialProductSnapshot.value) {
    const snapshot = JSON.parse(initialProductSnapshot.value);
    if (snapshot.productTypeId === id) {
      snapshot.productTypeId = null;
      initialProductSnapshot.value = JSON.stringify(snapshot);
    }
  }
};

const onTagDeleted = (id: string) => {
  // Tag deletion is already committed globally; discarding product edits must not restore it.
  if (initialFormValues.value?.tagIds) {
    initialFormValues.value.tagIds = initialFormValues.value.tagIds.filter(value => value !== id);
  }
  if (initialProductSnapshot.value) {
    const snapshot = JSON.parse(initialProductSnapshot.value);
    snapshot.tagIds = (snapshot.tagIds ?? []).filter((value: string) => value !== id);
    initialProductSnapshot.value = JSON.stringify(snapshot);
  }
};

const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value)) as T;

const getProductSnapshot = () =>
  JSON.stringify({
    ...values,
    selectedAttributeIds: [...selectedAttributeIds.value].sort(),
    selectedValuesByAttribute: Object.fromEntries(
      Object.entries(selectedValuesByAttribute.value)
        .map(([id, valueIds]) => [id, [...valueIds].sort()])
        .sort(([a], [b]) => String(a).localeCompare(String(b))),
    ),
  });

const saveInitialSnapshot = () => {
  initialFormValues.value = clone(values);
  initialSelectedAttributeIds.value = [...selectedAttributeIds.value];
  initialSelectedValuesByAttribute.value = clone(
    selectedValuesByAttribute.value,
  );
  initialProductSnapshot.value = getProductSnapshot();
};

const hasChanges = computed(
  () =>
    initialProductSnapshot.value !== null &&
    getProductSnapshot() !== initialProductSnapshot.value,
);

const showUnsavedSnackbar = computed(() => hasChanges.value);

const discardChanges = async () => {
  if (!initialFormValues.value) return;

  resetForm({
    values: clone(initialFormValues.value),
  });

  selectedAttributeIds.value = [...initialSelectedAttributeIds.value];
  selectedValuesByAttribute.value = clone(
    initialSelectedValuesByAttribute.value,
  );

  await nextTick();
};

// Product mapping

type ProductVariantForm = Omit<ProductVariantCommand, "inventoryQuantity"> & {
  id?: string;
  inventoryQuantity: number;
};

type ProductFormValues = Omit<ProductCommand, "variants"> & {
  variants: ProductVariantForm[];
};

type ProductFormSource = {
  tags?: Tag[];
  type?: ProductType | null | undefined;
  name: string;
  slug: string;
  media?: string;
  description?: string;
  status: ProductStatus;
  variants?: Array<{
    id?: string;
    name: string;
    sku: string;
    mediaList?: string[];
    buyPrice?: number | null;
    depositAmount?: number;
    active?: boolean;
    attributeValueIds?: string[];
    values?: Array<{
      attributeValueId: string;
    }>;
    inventoryUnits?: InventoryUnitCommand[];
    rentalPlans?: RentalPlanCommand[];
  }>;
};

const mapProductToForm = (product: ProductFormSource): ProductFormValues => ({
  productTypeId: product.type?.id ?? null,
  tagIds: (product.tags ?? []).map(tag => tag.id),
  name: product.name,
  slug: product.slug,
  media: product.media ?? "",
  description: product.description ?? "",
  status: product.status,
  variants: (product.variants ?? []).map(
    (variant): ProductVariantForm => ({
      id: variant.id,
      name: variant.name,
      sku: variant.sku,
      mediaList: variant.mediaList ?? [],
      buyPrice: variant.buyPrice ?? null,
      depositAmount: variant.depositAmount ?? 0,
      active: variant.active ?? true,
      attributeValueIds:
        variant.attributeValueIds ??
        variant.values?.map((value) => value.attributeValueId) ??
        [],
      inventoryUnits: variant.inventoryUnits ?? [],
      inventoryQuantity: variant.inventoryUnits?.length ?? 0,
      rentalPlans: (variant.rentalPlans ?? []).map((plan) => ({
        durationUnit: plan.durationUnit,
        durationValue: plan.durationValue,
        rentalFee: plan.rentalFee,
        active: plan.active,
      })),
    }),
  ),
});

watch(
  () => props.product,
  async (product) => {
    if (!product?.id) {
      resetForm({ values: clone(initialValues) });
      productMode.value = "single";

      selectedAttributeIds.value = [];
      selectedValuesByAttribute.value = {};

      initialFormValues.value = null;
      initialSelectedAttributeIds.value = [];
      initialSelectedValuesByAttribute.value = {};
      initialProductSnapshot.value = null;

      return;
    }

    resetForm({
      values: mapProductToForm(product),
    });

    productMode.value = detectProductMode(product);

    if (attributesInitialized.value) {
      syncSelectedAttributes(product);
      await nextTick();
      saveInitialSnapshot();
    }
  },
  { immediate: true },
);

// Establish the initial snapshot for a new product.
if (!props.product?.id) {
  await nextTick();
  saveInitialSnapshot();
}

// Submit

const onSubmit = handleSubmit(
  async (formValues) => {
    try {
      if (props.product?.id) {
        const savedProduct = await updateProduct(props.product.id, formValues);

        resetForm({
          values: mapProductToForm(savedProduct),
        });

        if (attributesInitialized.value) {
          syncSelectedAttributes(savedProduct);
        }

        await nextTick();
        saveInitialSnapshot();
      } else {
        await createProduct(formValues);

        resetForm({ values: clone(initialValues) });
        productMode.value = "single";

        selectedAttributeIds.value = [];
        selectedValuesByAttribute.value = {};

        initialFormValues.value = null;
        initialSelectedAttributeIds.value = [];
        initialSelectedValuesByAttribute.value = {};
        initialProductSnapshot.value = null;
      }

      snackbar.openSnackbar({
        type: "success",
        message: props.product?.id
          ? "product.edit.success"
          : "product.add.success",
      });

      emit("saved");
    } catch (error: any) {
      snackbar.openSnackbar({
        type: "error",
        message: apiErrorMessage(error),
      });
    }
  },
  () => {
    snackbar.openSnackbar({
      type: "error",
      message: t("product.validation.checkForm"),
    });
  },
);

// Regenerate variants when selected attribute values change.

watch(
  selectedValuesByAttribute,
  () => {
    if (!attributesInitialized.value) return;

    const groups = selectedAttributeGroups.value;
    if (groups.length === 0) return;

    const hasSelectedValues = groups.some(
      (attribute) =>
        (selectedValuesByAttribute.value[attribute.id]?.length ?? 0) > 0,
    );

    if (hasSelectedValues) {
      generateVariants();
    }
  },
  { deep: true },
);
</script>

<template>
  <media-dialog
    v-model="uploadProductMedia"
    @selected="onProductMediaSelected"
  />

  <media-dialog
    v-model="uploadVariantMedia"
    multiple
    @selected="onVariantMediaSelected"
  />

  <v-snackbar
    v-model="showUnsavedSnackbar"
    location="top center"
    :timeout="-1"
    color="primary"
    variant="flat"
    rounded="lg"
  >
    <span class="font-weight-bold">
      {{ t("product.unsavedChanges.title") }}
    </span>

    <template #actions>
      <v-btn
        variant="outlined"
        :disabled="isSubmitting"
        @click="discardChanges"
      >
        {{ t("common.discardChange") }}
      </v-btn>

      <v-btn
        class="ml-2"
        variant="tonal"
        :loading="isSubmitting"
        :disabled="isSubmitting"
        @click="onSubmit"
      >
        {{ t("common.saveChange") }}
      </v-btn>
    </template>
  </v-snackbar>

  <v-form class="product-form" @submit.prevent="onSubmit">
    <v-row align="start">
      <!-- Main content -->
      <v-col cols="12" md="9">
        <!-- Product mode -->
        <v-card variant="outlined" class="mb-4" rounded="lg">
          <v-card-item>
            <template #title>
              {{ t("product.form.productMode.title") }}
            </template>
            <template #subtitle>
              {{
                product?.id
                  ? t("product.form.productMode.locked")
                  : t("product.form.productMode.description")
              }}
            </template>
          </v-card-item>

          <v-divider />

          <v-card-text class="pa-4 pa-md-6">
            <v-row density="compact" role="radiogroup">
              <v-col
                v-for="option in productModeOptions"
                :key="option.value"
                cols="12"
                sm="6"
              >
                <v-card
                  variant="outlined"
                  class="mode-card"
                  :class="{ 'mode-card--active': productMode === option.value }"
                  :disabled="!!product?.id"
                  role="radio"
                  :aria-checked="productMode === option.value"
                  @click="productMode = option.value"
                >
                  <div class="d-flex align-start ga-3">
                    <v-avatar
                      color="primary"
                      variant="tonal"
                      size="40"
                      rounded="lg"
                    >
                      <v-icon :icon="option.icon" />
                    </v-avatar>

                    <div class="min-width-0 flex-grow-1">
                      <div class="text-body-1 font-weight-medium">
                        {{ option.title }}
                      </div>
                      <div class="text-body-small text-medium-emphasis mt-1">
                        {{ option.description }}
                      </div>
                    </div>

                    <v-icon
                      v-if="productMode === option.value"
                      icon="mdi-check-circle"
                      color="primary"
                    />
                  </div>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <!-- Product information -->
        <v-card variant="outlined" rounded="lg">
          <v-card-item>
            <template #title>
              {{ t("product.form.information") }}
            </template>
            <template #subtitle>
              {{
                product ? t("product.edit.subtitle") : t("product.add.subtitle")
              }}
            </template>
          </v-card-item>

          <v-divider />

          <v-card-text class="pa-4 pa-md-6">
            <div class="form-field">
              <v-label class="field-label" :text="t('product.name')" />
              <v-text-field
                v-model="name"
                :placeholder="t('product.namePlaceholder')"
                variant="outlined"
                density="compact"
                :error-messages="errors.name"
                hide-details="auto"
              />
            </div>

            <div class="form-field">
              <v-label class="field-label" :text="t('product.slug')" />
              <v-text-field
                v-model="slug"
                density="compact"
                variant="outlined"
                :error-messages="errors.slug"
                hide-details="auto"
              />

              <div v-if="permalink" class="permalink-preview">
                <v-icon icon="mdi-link-variant" size="small" />
                <span>{{ permalink }}</span>
              </div>
            </div>

            <div class="form-field">
              <v-label class="field-label" :text="t('product.description')" />

              <div class="editor-container">
                <QuillEditor
                  :content="description"
                  content-type="html"
                  theme="snow"
                  toolbar="essential"
                  @update:content="description = $event"
                />
              </div>
            </div>

            <div class="form-field mb-0">
              <div class="d-flex align-center justify-space-between ga-3 mb-3">
                <v-label :text="t('product.image.title')" />
                <v-btn
                  variant="outlined"
                  prepend-icon="mdi-image-plus"
                  @click="uploadProductMedia = true"
                >
                  {{ t("common.select") }}
                </v-btn>
              </div>

              <div v-if="media">
                <v-img
                  :src="media"
                  aspect-ratio="1"
                  max-width="240"
                  cover
                  class="rounded-lg"
                />

                <v-btn
                  class="image-remove-button"
                  icon="mdi-close"
                  size="small"
                  variant="elevated"
                  color="surface"
                  :aria-label="t('common.remove')"
                  @click="media = ''"
                />
              </div>

              <button
                v-else
                type="button"
                class="media-empty-state"
                @click="uploadProductMedia = true"
              >
                <v-icon icon="mdi-image-plus" size="36" />
                <span class="text-body-medium font-weight-medium mt-3">
                  {{ t("product.form.productImage") }}
                </span>
                <span class="text-body-small text-medium-emphasis mt-1">
                  {{ t("common.select") }}
                </span>
              </button>
            </div>
          </v-card-text>
        </v-card>

        <!-- Attributes (hanya untuk multiple product) -->
        <v-card
          v-if="!isSingleProduct"
          variant="outlined"
          class="form-card mt-4"
        >
          <v-card-item>
            <template #title>
              {{ t("product.attributes.title") }}
            </template>

            <template #subtitle>
              {{ t("product.attributes.selectExisting") }}
            </template>

           
          </v-card-item>

          <v-divider />

          <v-card-text class="pa-4 pa-md-6">
            <ProductAttributeSelector
              ref="attributeSelector"
              v-model:attribute-ids="selectedAttributeIds"
              v-model:values-by-attribute="selectedValuesByAttribute"
              @attributes-loaded="onAttributesLoaded"
              @attribute-updated="onAttributeUpdated"
            />
          </v-card-text>
        </v-card>

        <!-- Variants -->
        <div class="section-heading mt-6 mb-3">
          <div>
            <h2 class="text-title-medium font-weight-bold">
              {{
                isSingleProduct
                  ? t("product.form.singleProductTitle")
                  : t("product.variants.title")
              }}
            </h2>
            <p class="text-body-medium text-medium-emphasis mt-1">
              {{
                isSingleProduct
                  ? t("product.form.singleProductDescription")
                  : t("product.form.variantsDescription")
              }}
            </p>
          </div>

          <v-chip v-if="!isSingleProduct" size="small" variant="tonal">
            {{ t("product.form.variantsCount", { count: variants.length }) }}
          </v-chip>
        </div>

        <v-card
          v-if="variants.length === 0"
          variant="outlined"
          class="empty-variants pa-6 pa-md-10 text-center"
        >
          <v-icon
            icon="mdi-format-list-bulleted"
            size="42"
            color="medium-emphasis"
          />
          <div class="text-title-medium font-weight-medium mt-3">
            {{ t("product.form.noVariants") }}
          </div>
          <p class="text-body-medium text-medium-emphasis mt-1">
            {{ t("product.form.noVariantsDescription") }}
          </p>
        </v-card>

        <v-expansion-panels
          v-else
          v-model="variantPanels"
          class="variant-panels"
          :class="{ 'variant-panels--single': isSingleProduct }"
          variant="accordion"
          multiple
        >
          <v-expansion-panel
            v-for="(variant, index) in variants"
            :key="variant.key"
            class="variant-panel"
          >
            <v-expansion-panel-title>
              <div class="d-flex align-center ga-3 flex-grow-1 min-width-0">
                <v-avatar
                  color="primary"
                  variant="tonal"
                  size="40"
                  rounded="lg"
                >
                  <v-icon icon="mdi-package-variant" />
                </v-avatar>

                <div class="min-width-0 flex-grow-1">
                  <div class="text-body-1 font-weight-medium text-truncate">
                    {{
                      variant.value.name ||
                      `${t("product.variants.item")} ${index + 1}`
                    }}
                  </div>
                  <div
                    class="text-body-small text-medium-emphasis text-truncate"
                  >
                    {{ variant.value.sku || t("product.variant.skuMissing") }}
                  </div>
                </div>

                <v-chip
                  v-if="!variant.value.active"
                  size="small"
                  color="warning"
                  variant="tonal"
                  class="mr-2"
                >
                  {{ t("product.form.inactiveVariant") }}
                </v-chip>
              </div>
            </v-expansion-panel-title>

            <v-divider />

            <v-expansion-panel-text>
              <div class="">
                <!-- Variant information -->
                <v-expansion-panels
                  variant="default"
                  :model-value="openedPanels"
                >
                  <v-expansion-panel>
                    <v-expansion-panel-title>
                      <v-icon icon="mdi-information-outline" class="mr-3" />
                      <div class="min-width-0">
                        <div class="text-title-medium font-weight-medium">
                          {{ t("product.form.variantInformation") }}
                        </div>
                        <div class="text-body-small text-medium-emphasis">
                          {{ t("product.form.variantInformationDescription") }}
                        </div>
                      </div>
                    </v-expansion-panel-title>

                    <v-expansion-panel-text>
                      <v-row>
                        <v-col v-if="!isSingleProduct" cols="12" md="6">
                          <v-label
                            class="field-label"
                            :text="t('product.variant.name.label')"
                          />
                          <v-text-field
                            v-model="variant.value.name"
                            variant="outlined"
                            density="compact"
                            :error-messages="errors[`variants[${index}].name`]"
                            hide-details="auto"
                          />
                        </v-col>

                        <v-col cols="12" :md="isSingleProduct ? 12 : 6">
                          <v-label
                            class="field-label"
                            :text="t('product.variant.sku.label')"
                          />
                          <v-text-field
                            v-model="variant.value.sku"
                            variant="outlined"
                            density="compact"
                            :error-messages="errors[`variants[${index}].sku`]"
                            hide-details="auto"
                          />
                        </v-col>

                        <v-col cols="12" md="6">
                          <v-label
                            class="field-label"
                            :text="t('product.buyPrice')"
                          />
                          <v-text-field
                            :model-value="formatMoney(variant.value.buyPrice)"
                            inputmode="numeric"
                            variant="outlined"
                            density="compact"
                            hide-details
                            @update:model-value="
                              onVariantMoneyInput(
                                $event ?? '',
                                index,
                                'buyPrice',
                              )
                            "
                          />
                        </v-col>

                        <v-col cols="12" md="6">
                          <v-label
                            class="field-label"
                            :text="t('product.depositAmount')"
                          />
                          <v-text-field
                            :model-value="
                              formatMoney(variant.value.depositAmount)
                            "
                            inputmode="numeric"
                            variant="outlined"
                            density="compact"
                            hide-details
                            @update:model-value="
                              onVariantMoneyInput(
                                $event ?? '',
                                index,
                                'depositAmount',
                              )
                            "
                          />
                        </v-col>

                        <v-col cols="12">
                          <v-switch
                            v-model="variant.value.active"
                            color="primary"
                            :label="t('product.variant.active')"
                            hide-details
                          />
                        </v-col>

                        <v-col v-if="!isSingleProduct" cols="12">
                          <v-label class="field-label">
                            {{ t("product.variant.attributes") }}
                          </v-label>

                          <div
                            v-if="variant.value.attributeValueIds.length"
                            class="d-flex flex-wrap ga-2"
                          >
                            <v-chip
                              v-for="id in variant.value.attributeValueIds"
                              :key="id"
                              size="small"
                              variant="tonal"
                            >
                              {{
                                availableAttributeValues.find(
                                  (item) => item.id === id,
                                )?.value ?? id
                              }}
                            </v-chip>
                          </div>

                          <div
                            v-else
                            class="text-body-medium text-medium-emphasis"
                          >
                            {{ t("product.form.noAttributes") }}
                          </div>
                        </v-col>

                        <v-col cols="12">
                          <div
                            class="d-flex align-center justify-space-between flex-wrap ga-3 mb-3"
                          >
                            <v-label :text="t('product.variant.media')" />

                            <v-btn
                              variant="outlined"
                              prepend-icon="mdi-image-plus"
                              @click="openVariantMedia(index)"
                            >
                              {{ t("common.add") }}
                            </v-btn>
                          </div>

                          <v-row v-if="variant.value.mediaList?.length">
                            <v-col
                              v-for="(image, imageIndex) in variant.value
                                .mediaList"
                              :key="`${variant.key}-${imageIndex}`"
                              cols="6"
                              sm="4"
                              md="3"
                            >
                              <div class="variant-image">
                                <v-img
                                  :src="image"
                                  aspect-ratio="1"
                                  cover
                                  class="rounded-lg"
                                />

                                <v-btn
                                  icon="mdi-close"
                                  size="x-small"
                                  color="surface"
                                  variant="elevated"
                                  class="variant-image-remove"
                                  :aria-label="t('common.remove')"
                                  @click="removeVariantMedia(index, imageIndex)"
                                />
                              </div>
                            </v-col>
                          </v-row>

                          <div
                            v-else
                            class="text-body-medium text-medium-emphasis"
                          >
                            {{ t("product.form.noVariantImages") }}
                          </div>
                        </v-col>
                      </v-row>
                    </v-expansion-panel-text>
                  </v-expansion-panel>
                  <!-- Rental plans -->
                  <v-expansion-panel>
                    <v-expansion-panel-title>
                      <v-icon icon="mdi-calendar-clock" class="mr-3" />

                      <div
                        class="d-flex align-center justify-space-between flex-grow-1 ga-3 min-width-0"
                      >
                        <div class="min-width-0">
                          <div class="text-title-medium font-weight-medium">
                            {{ t("product.form.rentalPlanTitle") }}
                          </div>
                          <div class="text-body-small text-medium-emphasis">
                            {{ t("product.form.rentalPlanDescription") }}
                          </div>
                        </div>

                        <v-chip size="small" color="primary" variant="tonal">
                          {{
                            t("product.form.plansCount", {
                              count: variant.value.rentalPlans.length,
                            })
                          }}
                        </v-chip>
                      </div>
                    </v-expansion-panel-title>

                    <v-expansion-panel-text>
                      <div class="d-flex justify-end flex-wrap ga-2 mb-4">
                        <v-btn
                          size="small"
                          variant="outlined"
                          prepend-icon="mdi-plus"
                          @click.stop="addRentalPlan(index)"
                        >
                          {{ t("common.add") }}
                        </v-btn>

                        <v-btn
                          size="small"
                          variant="outlined"
                          prepend-icon="mdi-auto-fix"
                          :disabled="variant.value.rentalPlans.length > 0"
                          @click.stop="generateRentalPlans(index)"
                        >
                          {{ t("common.generate") }}
                        </v-btn>

                        <v-btn
                          size="small"
                          variant="text"
                          color="error"
                          prepend-icon="mdi-delete-outline"
                          :disabled="variant.value.rentalPlans.length === 0"
                          @click.stop="clearRentalPlans(index)"
                        >
                          {{ t("common.clear") }}
                        </v-btn>
                      </div>

                      <div
                        v-if="variant.value.rentalPlans.length"
                        class="d-flex flex-column ga-3"
                      >
                        <v-card
                          v-for="(plan, planIndex) in variant.value.rentalPlans"
                          :key="`${variant.key}-plan-${planIndex}`"
                          variant="outlined"
                          class="rental-plan-card"
                        >
                          <div
                            class="d-flex align-center justify-space-between ga-3 mb-4"
                          >
                            <div class="d-flex align-center ga-3">
                              <v-avatar
                                color="primary"
                                variant="tonal"
                                size="36"
                                rounded="lg"
                              >
                                <v-icon icon="mdi-clock-outline" />
                              </v-avatar>

                              <div>
                                <div
                                  class="text-body-medium font-weight-medium"
                                >
                                  {{ t("rentalplan.title") }}
                                  {{ planIndex + 1 }}
                                </div>
                                <div
                                  class="text-body-small text-medium-emphasis"
                                >
                                  {{
                                    t(
                                      `rentalplan.durationUnitOptions.${plan.durationUnit}`,
                                    )
                                  }}
                                </div>
                              </div>
                            </div>

                            <div class="d-flex align-center ga-2">
                              <v-switch
                                v-model="plan.active"
                                color="primary"
                                hide-details
                                density="compact"
                                :aria-label="t('product.variant.active')"
                              />

                              <v-btn
                                icon
                                size="small"
                                color="error"
                                variant="text"
                                :aria-label="t('common.remove')"
                                @click="removeRentalPlan(index, planIndex)"
                              >
                                <DeleteOutlined />
                              </v-btn>
                            </div>
                          </div>

                          <v-row>
                            <v-col cols="12" sm="3">
                              <v-text-field
                                v-model.number="plan.durationValue"
                                type="number"
                                density="compact"
                                min="1"
                                :label="t('rentalplan.durationValue')"
                                variant="outlined"
                                hide-details="auto"
                              />
                            </v-col>

                            <v-col cols="12" sm="3">
                              <v-select
                                v-model="plan.durationUnit"
                                :items="durationUnits"
                                :label="t('rentalplan.durationUnit')"
                                :item-title="
                                  (unit: DurationUnit) =>
                                    t(`rentalplan.durationUnitOptions.${unit}`)
                                "
                                variant="outlined"
                                density="compact"
                                hide-details
                              />
                            </v-col>

                            <v-col cols="12" sm="6">
                              <v-text-field
                                :model-value="formatMoney(plan.rentalFee)"
                                inputmode="numeric"
                                :label="t('rentalplan.rentalFee')"
                                variant="outlined"
                                density="compact"
                                hide-details
                                @update:model-value="
                                  onRentalFeeInput(
                                    $event ?? '',
                                    index,
                                    planIndex,
                                  )
                                "
                              />
                            </v-col>
                          </v-row>
                        </v-card>
                      </div>

                      <v-card
                        v-else
                        variant="outlined"
                        class="empty-state pa-6 text-center"
                      >
                        <v-avatar
                          color="primary"
                          variant="tonal"
                          size="52"
                          class="mb-3"
                        >
                          <v-icon icon="mdi-calendar-clock" size="26" />
                        </v-avatar>

                        <div class="text-body-medium font-weight-medium">
                          {{ t("product.form.emptyRentalPlans") }}
                        </div>

                        <p
                          class="text-body-small text-medium-emphasis mt-1 mb-4"
                        >
                          {{ t("product.form.emptyRentalPlansDescription") }}
                        </p>

                        <div class="d-flex justify-center flex-wrap ga-2">
                          <v-btn
                            color="primary"
                            variant="tonal"
                            prepend-icon="mdi-plus"
                            @click="addRentalPlan(index)"
                          >
                            {{ t("common.add") }}
                          </v-btn>

                          <v-btn
                            variant="outlined"
                            prepend-icon="mdi-auto-fix"
                            :disabled="
                              variant.value.buyPrice == null ||
                              variant.value.buyPrice <= 0
                            "
                            @click="generateRentalPlans(index)"
                          >
                            {{ t("common.generate") }}
                          </v-btn>
                        </div>
                      </v-card>
                    </v-expansion-panel-text>
                  </v-expansion-panel>
                  <!-- Inventory units -->
                  <v-expansion-panel>
                    <v-expansion-panel-title>
                      <v-icon icon="mdi-package-variant-closed" class="mr-3" />

                      <div
                        class="d-flex align-center justify-space-between flex-grow-1 ga-3 min-width-0"
                      >
                        <div class="min-width-0">
                          <div class="text-title-medium font-weight-medium">
                            {{ t("product.form.inventoryUnits") }}
                          </div>
                          <div class="text-body-small text-medium-emphasis">
                            {{ t("product.form.inventoryDescription") }}
                          </div>
                        </div>

                        <v-chip size="small" color="primary" variant="tonal">
                          {{
                            t("product.form.unitsCount", {
                              count: variant.value.inventoryUnits?.length ?? 0,
                            })
                          }}
                        </v-chip>
                      </div>
                    </v-expansion-panel-title>

                    <v-expansion-panel-text>
                      <v-card variant="outlined" class="mb-4">
                        <div
                          class="d-flex flex-column flex-sm-row align-sm-center ga-3 pa-3"
                        >
                          <div class="flex-grow-1">
                            <v-text-field
                              v-model.number="variant.value.inventoryQuantity"
                              type="number"
                              min="0"
                              step="1"
                              :label="t('product.form.inventoryQuantity')"
                              variant="outlined"
                              :hint="
                                t('product.form.inventoryQuantityHint')
                              "
                              hide-details
                              density="compact"
                              
                            />
                          </div>

                          <div class="d-flex justify-end ga-2">
                            <v-btn
                              variant="outlined"
                              prepend-icon="mdi-plus"
                              @click="addInventoryUnit(index)"
                            >
                              {{ t("product.form.addUnit") }}
                            </v-btn>

                            <v-btn
                              color="primary"
                              prepend-icon="mdi-auto-fix"
                              :disabled="!variant.value.sku.trim()"
                              @click="generateVariantInventory(index)"
                            >
                              {{ t("product.form.generateUnits") }}
                            </v-btn>
                          </div>
                        </div>
                      </v-card>

                      <div
                        v-if="variant.value.inventoryUnits?.length"
                        class="inventory-table-scroll"
                      >
                        <v-table hover>
                          <thead>
                            <tr>
                              <th>{{ t("product.form.barcode") }}</th>
                              <th>{{ t("product.form.serialNumber") }}</th>
                              <th>{{ t("product.form.status") }}</th>
                              <th>{{ t("product.form.condition") }}</th>
                              <th class="text-end">
                                {{ t("product.form.action") }}
                              </th>
                            </tr>
                          </thead>

                          <tbody>
                            <tr
                              v-for="(unit, unitIndex) in variant.value
                                .inventoryUnits"
                              :key="unit.id ?? `${variant.key}-${unitIndex}`"
                            >
                              <td>
                                <v-text-field
                                  :model-value="unit.skuBarcode"
                                  variant="outlined"
                                  density="compact"
                                  :placeholder="t('product.form.enterBarcode')"
                                  :error-messages="
                                    errors[
                                      `variants[${index}].inventoryUnits[${unitIndex}].skuBarcode`
                                    ]
                                  "
                                  hide-details="auto"
                                  @update:model-value="
                                    onInventoryUnitChange(
                                      $event ?? '',
                                      index,
                                      unitIndex,
                                      'skuBarcode',
                                    )
                                  "
                                />
                              </td>

                              <td>
                                <span
                                  class="text-body-medium text-medium-emphasis"
                                >
                                  {{
                                    unit.serialNumber ||
                                    t("product.form.generatedOnSave")
                                  }}
                                </span>
                              </td>

                              <td>
                                <v-chip
                                  size="small"
                                  :color="
                                    unit.status === 'AVAILABLE'
                                      ? 'success'
                                      : 'default'
                                  "
                                  variant="tonal"
                                >
                                  {{
                                    t(
                                      `product.inventory.status.${unit.status ?? "AVAILABLE"}`,
                                    )
                                  }}
                                </v-chip>
                              </td>

                              <td>
                                <v-text-field
                                  :model-value="unit.conditionNotes ?? ''"
                                  variant="outlined"
                                  density="compact"
                                  :placeholder="
                                    t('product.form.conditionNotes')
                                  "
                                  :error-messages="
                                    errors[
                                      `variants[${index}].inventoryUnits[${unitIndex}].conditionNotes`
                                    ]
                                  "
                                  hide-details="auto"
                                  @update:model-value="
                                    onInventoryUnitChange(
                                      $event ?? '',
                                      index,
                                      unitIndex,
                                      'conditionNotes',
                                    )
                                  "
                                />
                              </td>

                              <td class="text-end">
                                <v-btn
                                  icon="mdi-delete-outline"
                                  size="small"
                                  color="error"
                                  variant="text"
                                  :disabled="!!unit.id"
                                  :aria-label="t('common.remove')"
                                  @click="removeInventoryUnit(index, unitIndex)"
                                />
                              </td>
                            </tr>
                          </tbody>
                        </v-table>
                      </div>

                      <v-card
                        v-else
                        variant="outlined"
                        class="empty-state pa-6 text-center"
                      >
                        <v-avatar
                          color="primary"
                          variant="tonal"
                          size="52"
                          class="mb-3"
                        >
                          <v-icon icon="mdi-package-variant-closed" size="26" />
                        </v-avatar>

                        <div class="text-body-medium font-weight-medium">
                          {{ t("product.form.emptyInventoryTitle") }}
                        </div>

                        <p class="text-body-small text-medium-emphasis mt-1">
                          {{ t("product.form.emptyInventory") }}
                        </p>
                      </v-card>
                    </v-expansion-panel-text>
                  </v-expansion-panel>
                </v-expansion-panels>

                <div v-if="!isSingleProduct" class="d-flex justify-end pt-2">
                  <v-btn
                    color="error"
                    variant="text"
                    prepend-icon="mdi-delete-outline"
                    :disabled="variants.length === 1"
                    @click="removeVariant(index)"
                  >
                    {{ t("product.variants.remove") }}
                  </v-btn>
                </div>
              </div>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-col>

      <!-- Sidebar -->
      <v-col cols="12" md="3">
        <div class="product-sidebar">
          <v-card variant="outlined" class="form-card">
            <v-card-item>
              <template #title>
                {{ t("product.form.publishing") }}
              </template>
              <template #subtitle>
                {{ t("product.form.publishingDescription") }}
              </template>
            </v-card-item>

            <v-divider />

            <v-card-text class="pa-4">
              <div class="form-field">
                <v-label class="field-label" :text="t('product.status')" />

                <product-status v-model="status" />
              </div>

              <ProductTypeAutoComplete :hide-delete="false" v-model="productTypeId" @deleted="onProductTypeDeleted" />
              <ProductTagAutoComplete v-model="tagIds" :initial-items="product?.tags ?? []" :disabled="isSubmitting" class="mt-4" @deleted="onTagDeleted" />
            </v-card-text>
          </v-card>

          <v-card variant="outlined" class="mt-4">
            <v-card-item>
              <template #title>
                {{ t("product.form.summary") }}
              </template>
            </v-card-item>

            <v-divider />

            <v-card-text class="pa-4">
              <template v-if="!isSingleProduct">
                <div class="summary-row">
                  <span class="text-body-medium text-medium-emphasis">
                    {{ t("product.form.variantsCountLabel") }}
                  </span>
                  <span class="text-body-medium font-weight-medium">
                    {{ variants.length }}
                  </span>
                </div>

                <v-divider class="my-3" />
              </template>

              <div class="summary-row">
                <span class="text-body-medium text-medium-emphasis">
                  {{ t("rentalplan.title") }}
                </span>
                <span class="text-body-medium font-weight-medium">
                  {{
                    values.variants.reduce(
                      (total, variant) =>
                        total + (variant.rentalPlans?.length ?? 0),
                      0,
                    )
                  }}
                </span>
              </div>

              <v-divider class="my-3" />

              <div class="summary-row">
                <span class="text-body-medium text-medium-emphasis">
                  {{ t("product.form.inventoryUnits") }}
                </span>
                <span class="text-body-medium font-weight-medium">
                  {{
                    values.variants.reduce(
                      (total, variant) =>
                        total + (variant.inventoryUnits?.length ?? 0),
                      0,
                    )
                  }}
                </span>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-col>
    </v-row>
  </v-form>
</template>

<style scoped>


.form-card {
  overflow: hidden;
  background: rgb(var(--v-theme-surface));
}

.form-field {
  margin-bottom: 24px;
}

.mode-card {
  height: 100%;
  padding: 16px;
  
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.mode-card:hover {
  border-color: rgb(var(--v-theme-primary));
}

.mode-card--active {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.04);
  box-shadow: 0 0 0 1px rgb(var(--v-theme-primary));
}

/* Single product: sembunyikan header panel varian, konten langsung tampil. */
.variant-panels--single :deep(.variant-panel > .v-expansion-panel-title),
.variant-panels--single :deep(.variant-panel > .v-divider) {
  display: none;
}

.field-label {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
}

.permalink-preview {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
  color: rgb(var(--v-theme-primary));
  font-size: 0.75rem;
  overflow-wrap: anywhere;
}

.editor-container {
  overflow: hidden;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}

:deep(.ql-toolbar.ql-snow) {
  border: 0;
  border-bottom: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

:deep(.ql-container.ql-snow) {
  border: 0;
}

:deep(.ql-container),
:deep(.ql-editor) {
  min-height: 180px;
}

:deep(.ql-editor) {
  font-family: inherit;
  font-size: 0.875rem;
}

.image-remove-button {
  position: absolute;
  top: 8px;
  right: 8px;
}

.media-empty-state {
  display: flex;
  width: 100%;
  max-width: 240px;
  min-height: 180px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border: 1px dashed rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 12px;
  background: rgba(var(--v-theme-on-surface), 0.02);
  color: rgb(var(--v-theme-on-surface-variant));
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.media-empty-state:hover {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.04);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.empty-variants,
.empty-state {
  border-style: dashed;
  border-radius: 12px;
}

.variant-panels {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.variant-panel {
  overflow: hidden;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 12px !important;
  background: rgb(var(--v-theme-surface));
}

:deep(.variant-panel > .v-expansion-panel-title) {
  min-height: 76px;
  padding: 16px 20px;
}

:deep(
  .variant-panel > .v-expansion-panel-text > .v-expansion-panel-text__wrapper
) {
  padding: 16px;
}

.variant-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.variant-section .v-expansion-panel) {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 10px !important;
}

:deep(.variant-section .v-expansion-panel-title) {
  min-height: 68px;
  padding: 14px 16px;
}

:deep(.variant-section .v-expansion-panel-text__wrapper) {
  padding: 16px;
}

.rental-plan-card {
  padding: 16px;
  border-radius: 10px;
}

.inventory-generator {
  border-radius: 10px;
}

.inventory-generate-button {
  min-height: 44px;
}

.inventory-table-scroll {
  max-width: 100%;
  overflow-x: auto;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 10px;
}

.inventory-table-scroll :deep(table) {
  min-width: 820px;
}

.inventory-table-scroll :deep(th) {
  white-space: nowrap;
  font-size: 0.75rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface-variant));
}

.inventory-table-scroll :deep(td) {
  padding-top: 12px;
  padding-bottom: 12px;
}

.variant-image {
  position: relative;
}

.variant-image-remove {
  position: absolute;
  top: 6px;
  right: 6px;
}

.product-sidebar {
  position: sticky;
  top: 24px;
}

.summary-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.min-width-0 {
  min-width: 0;
}

@media (max-width: 959px) {
  .product-sidebar {
    position: static;
  }
}

@media (max-width: 600px) {
  .section-heading {
    align-items: flex-start;
  }

  .rental-plan-card {
    padding: 12px;
  }
}
</style>
