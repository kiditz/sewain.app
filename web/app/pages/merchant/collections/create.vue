<script setup lang="ts">
import type {
  Collection,
  CollectionCommand,
  CollectionMatchCondition,
  CollectionRule,
  CollectionSummary,
  CollectionType,
} from "~/types/collection";
import type { Media } from "~/types/media";
import type { Product, ProductVariantSelection } from "~/types/product";
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";

definePageMeta({
  layout: "merchant",
  middleware: "auth",
});

const { t } = useI18n();
const route = useRoute();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();

const {
  findCollections,
  findCollection,
  findCollectionProducts,
  createCollection,
  updateCollection,
} = useCollection();

const collectionId = typeof route.query.id === "string" ? route.query.id : null;

const isEditing = computed(() => Boolean(collectionId));

const {
  data: existingCollection,
  pending: collectionPending,
  error: collectionError,
} = await useAsyncData<Collection | null>(
  `merchant-collection-${collectionId ?? "new"}`,
  () => (collectionId ? findCollection(collectionId) : Promise.resolve(null)),
  {
    default: () => null,
  },
);

const { data: collectionSummaries } = await useAsyncData<CollectionSummary[]>(
  "merchant-collection-options",
  () => findCollections(),
  {
    default: () => [],
  },
);

const collectionFormSchema = toTypedSchema(
  z
    .object({
      name: z.string().trim().min(1, "Collection name is required."),
      slug: z.string().trim().min(1, "Slug is required."),
      imageUrl: z.string(),
      parentId: z.string().nullable(),
      description: z.string(),
      type: z.enum(["MANUAL", "AUTOMATED"]),
      matchCondition: z.enum(["ALL", "ANY"]),
      rules: z.array(
        z.object({
          attribute: z.enum([
            "TITLE",
            "TYPE",
            "TAG",
            "VARIANT_TITLE",
            "ATTRIBUTE_VALUE",
          ]),
          operator: z.enum([
            "EQUALS",
            "NOT_EQUALS",
            "CONTAINS",
            "NOT_CONTAINS",
            "STARTS_WITH",
            "ENDS_WITH",
          ]),
          values: z.array(z.string()),
        }),
      ),
      productIds: z.array(z.string()),
    })
    .superRefine((values, ctx) => {
      if (values.type === "MANUAL" && values.productIds.length === 0) {
        ctx.addIssue({
          code: "custom",
          path: ["productIds"],
          message: "collections.noProductsSelected",
        });
      }

      if (values.type === "AUTOMATED") {
        if (values.rules.length === 0) {
          ctx.addIssue({
            code: "custom",
            path: ["rules"],
            message: "collections.validation.atLeastOneCondition",
          });
        }

        values.rules.forEach((rule, index) => {
          if (!rule.values.some((value) => value.trim().length > 0)) {
            ctx.addIssue({
              code: "custom",
              path: ["rules", index, "values"],
              message: "collections.validation.conditionValueRequired",
            });
          }
        });
      }
    }),
);

const { values, errors, setFieldValue, resetForm, handleSubmit, defineField } =
  useForm<{
    name: string;
    slug: string;
    imageUrl: string;
    parentId: string | null;
    description: string;
    type: CollectionType;
    matchCondition: CollectionMatchCondition;
    rules: CollectionRule[];
    productIds: string[];
  }>({
    validationSchema: collectionFormSchema,
    initialValues: {
      name: "",
      slug: "",
      imageUrl: "",
      parentId: null,
      description: "",
      type: "MANUAL",
      matchCondition: "ALL",
      rules: [],
      productIds: [],
    },
  });

const [name] = defineField("name");
const [slug] = defineField("slug");
const [description] = defineField("description");
const [parentId] = defineField("parentId");
const [collectionType] = defineField("type");
const [matchCondition] = defineField("matchCondition");

const slugManuallyEdited = ref(false);
const productVariantDialog = ref(false);
const mediaDialog = ref(false);
const selectedProducts = ref<Product[]>([]);
const saving = ref(false);

function slugify(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

watch(
  () => values.name,
  (name) => {
    if (!slugManuallyEdited.value) {
      setFieldValue("slug", slugify(name ?? ""));
    }
  },
);

watch(
  existingCollection,
  (collection) => {
    if (!collection) return;

    slugManuallyEdited.value = true;

    resetForm({
      values: {
        name: collection.name,
        slug: collection.slug,
        imageUrl: collection.imageUrl ?? "",
        parentId: collection.parentId ?? null,
        description: collection.description ?? "",
        type: collection.type,
        matchCondition: collection.matchCondition,
        rules: collection.rules.map((rule) => ({
          ...rule,
          values: rule.values.length ? [...rule.values] : [""],
        })),
        productIds: [...collection.productIds],
      },
    });
  },
  { immediate: true },
);

const parentOptions = computed(() =>
  (collectionSummaries.value ?? [])
    .filter((collection) => collection.id !== collectionId)
    .map((collection) => ({
      title: collection.name,
      value: collection.id,
    })),
);

/**
 * Existing manual collection products are loaded through the collection API.
 */
const {
  data: collectionProducts,
  pending: collectionProductsPending,
  error: collectionProductsError,
} = await useAsyncData<Product[]>(
  `merchant-collection-products-${collectionId ?? "new"}`,
  async () => {
    if (!collectionId || existingCollection.value?.type !== "MANUAL") {
      return [];
    }

    const products: Product[] = [];
    const size = 100;
    let page = 0;
    let totalItems = 0;

    do {
      const response = await findCollectionProducts(collectionId, {
        page,
        size,
      });

      products.push(...response.items);
      totalItems = response.totalItems;
      page++;

      if (response.items.length === 0) break;
    } while (products.length < totalItems);

    return products;
  },
  {
    default: () => [],
  },
);

/**
 * Restore product display order from collection.productIds.
 */
watch(
  collectionProducts,
  (products) => {
    const productsById = new Map(
      products.map((product) => [product.id, product]),
    );

    selectedProducts.value = (values.productIds ?? []).flatMap((id) => {
      const product = productsById.get(id);
      return product ? [product] : [];
    });
  },
  { immediate: true },
);

const addProduct = (selection: ProductVariantSelection) => {
  const product = selection.product;
  const currentProductIds = values.productIds ?? [];
  if (currentProductIds.includes(product.id)) return;

  setFieldValue("productIds", [...currentProductIds, product.id]);
  selectedProducts.value = [...selectedProducts.value, product];
};

const removeProduct = (productId: string) => {
  setFieldValue(
    "productIds",
    (values.productIds ?? []).filter((id) => id !== productId),
  );
  selectedProducts.value = selectedProducts.value.filter(
    (product) => product.id !== productId,
  );
};

const selectImage = (selection: Media | Media[]) => {
  const selected = Array.isArray(selection) ? selection[0] : selection;
  if (selected) setFieldValue("imageUrl", selected.url);
};

const removeImage = () => setFieldValue("imageUrl", "");

const attributeOptions = computed(() => [
  {
    title: t("collections.condition.productTitle"),
    value: "TITLE",
  },
  {
    title: t("collections.condition.productType"),
    value: "TYPE",
  },
  {
    title: t("collections.condition.tag"),
    value: "TAG",
  },
  {
    title: t("collections.condition.variantTitle"),
    value: "VARIANT_TITLE",
  },
  {
    title: t("collections.condition.attributeValue"),
    value: "ATTRIBUTE_VALUE",
  },
]);

const operatorOptions = computed(() => [
  {
    title: t("collections.condition.equals"),
    value: "EQUALS",
  },
  {
    title: t("collections.condition.notEquals"),
    value: "NOT_EQUALS",
  },
  {
    title: t("collections.condition.contains"),
    value: "CONTAINS",
  },
  {
    title: t("collections.condition.notContains"),
    value: "NOT_CONTAINS",
  },
  {
    title: t("collections.condition.startsWith"),
    value: "STARTS_WITH",
  },
  {
    title: t("collections.condition.endsWith"),
    value: "ENDS_WITH",
  },
]);

const setRuleAttribute = (
  ruleIndex: number,
  attribute: CollectionRule["attribute"],
) => {
  setFieldValue(
    "rules",
    values.rules.map((rule, index) =>
      index === ruleIndex ? { ...rule, attribute } : rule,
    ),
  );
};

const setRuleOperator = (
  ruleIndex: number,
  operator: CollectionRule["operator"],
) => {
  setFieldValue(
    "rules",
    values.rules.map((rule, index) =>
      index === ruleIndex ? { ...rule, operator } : rule,
    ),
  );
};

const setRuleValues = (ruleIndex: number, nextValues: string[]) => {
  setFieldValue(
    "rules",
    values.rules.map((rule, index) =>
      index === ruleIndex ? { ...rule, values: nextValues } : rule,
    ),
  );
};

const addRule = () => {
  setFieldValue("rules", [
    ...values.rules,
    { attribute: "TYPE", operator: "EQUALS", values: [""] },
  ]);
};

const removeRule = (index: number) => {
  setFieldValue(
    "rules",
    values.rules.filter((_, ruleIndex) => ruleIndex !== index),
  );
};

const setRuleValue = (
  ruleIndex: number,
  valueIndex: number,
  value: string | null,
) => {
  const rule = values.rules[ruleIndex];
  if (!rule) return;

  const name = value?.trim() ?? "";
  const duplicate =
    name.length > 0 &&
    rule.values.some(
      (existing, index) =>
        index !== valueIndex &&
        existing.trim().toLocaleLowerCase() === name.toLocaleLowerCase(),
    );
  if (duplicate) return;

  const rules = values.rules.map((item, index) =>
    index === ruleIndex
      ? {
          ...item,
          values: item.values.map((existing, index) =>
            index === valueIndex ? name : existing,
          ),
        }
      : item,
  );
  setFieldValue("rules", rules);
};

const addRuleValue = (ruleIndex: number) => {
  setFieldValue(
    "rules",
    values.rules.map((rule, index) =>
      index === ruleIndex ? { ...rule, values: [...rule.values, ""] } : rule,
    ),
  );
};

const removeRuleValue = (ruleIndex: number, valueIndex: number) => {
  setFieldValue(
    "rules",
    values.rules.map((rule, index) => {
      if (index !== ruleIndex) return rule;
      const nextValues = rule.values.filter((_, index) => index !== valueIndex);
      return { ...rule, values: nextValues.length ? nextValues : [""] };
    }),
  );
};

const save = handleSubmit(async (formValues) => {
  if (
    saving.value ||
    (isEditing.value && (!existingCollection.value || collectionPending.value))
  ) {
    return;
  }

  saving.value = true;

  const command: CollectionCommand = {
    name: formValues.name.trim(),
    slug: formValues.slug.trim(),
    imageUrl: formValues.imageUrl.trim() || null,
    parentId: formValues.parentId,
    description: formValues.description.trim(),
    type: formValues.type,
    matchCondition:
      formValues.type === "AUTOMATED" ? formValues.matchCondition : "ALL",
    ...(formValues.type === "AUTOMATED"
      ? {
          rules: formValues.rules.map((rule) => ({
            ...rule,
            values: rule.values.map((value) => value.trim()).filter(Boolean),
          })),
        }
      : { productIds: [...(formValues.productIds ?? [])] }),
  };

  try {
    if (collectionId) {
      await updateCollection(collectionId, command);
    } else {
      await createCollection(command);
    }

    snackbar.openSnackbar({
      type: "success",
      message: collectionId
        ? "collections.edit.success"
        : "collections.add.success",
    });

    clearNuxtData("merchant-collections");
    clearNuxtData("merchant-collection-options");
    await navigateTo("/merchant/collections", { replace: true });
  } catch (error: unknown) {
    const apiError = error as { data?: { message?: string } };
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(apiError),
    });
  } finally {
    saving.value = false;
  }
});

const collectionTypeOptions = computed<
  Array<{
    value: CollectionType;
    icon: string;
    title: string;
    description: string;
  }>
>(() => [
  {
    value: "MANUAL",
    icon: "mdi-gesture-tap",
    title: t("collections.type.manual"),
    description: t("collections.type.manualDescription"),
  },
  {
    value: "AUTOMATED",
    icon: "mdi-robot",
    title: t("collections.type.automated"),
    description: t("collections.type.automatedDescription"),
  },
]);
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex align-center ga-3 mb-6">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        to="/merchant/collections"
        :aria-label="t('common.cancel')"
      />

      <div>
        <div class="text-headline-small font-weight-bold">
          {{
            isEditing
              ? t("collections.actions.edit")
              : t("collections.actions.create")
          }}
        </div>

        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("collections.createSubtitle") }}
        </p>
      </div>
    </div>

    <v-progress-linear
      v-if="collectionPending"
      indeterminate
      color="primary"
      class="mb-4"
    />

    <v-alert v-if="collectionError" type="error" variant="tonal" class="mb-4">
      {{ collectionError.message || t("collections.errors.loadFailed") }}
    </v-alert>

    <v-alert
      v-if="collectionProductsError"
      type="error"
      variant="tonal"
      class="mb-4"
    >
      {{
        collectionProductsError.message ||
        t("collections.errors.loadProductsFailed")
      }}
    </v-alert>

    <v-form @submit.prevent="save">
      <v-row>
        <v-col cols="12" md="8">
          <!-- Details -->
          <v-card rounded="lg" variant="outlined" class="mb-4">
            <v-card-item>
              <template #title>
                {{ t("collections.sections.details") }}
              </template>
            </v-card-item>
            <v-divider />
            <v-card-text>
              <v-label class="mb-1 text-body-medium">
                {{ t("collections.fields.title") }}
              </v-label>
              <v-text-field
                v-model="name"
                :placeholder="t('collections.placeholders.title')"
                variant="outlined"
                density="compact"
                :error-messages="errors.name ? [errors.name] : []"
                required
              />

              <v-label class="mb-1 text-body-medium">
                {{ t("collections.fields.slug") }}
              </v-label>
              <v-text-field
                v-model="slug"
                prepend-inner-icon="mdi-link-variant"
                variant="outlined"
                density="compact"
                :error-messages="errors.slug ? [errors.slug] : []"
                required
                @update:model-value="slugManuallyEdited = true"
              />

              <v-label class="mb-1 text-body-medium">
                {{ t("collections.fields.description") }}
              </v-label>
              <v-textarea
                v-model="description"
                :placeholder="t('collections.placeholders.description')"
                variant="outlined"
                density="compact"
                rows="3"
                auto-grow
                hide-details
              />
            </v-card-text>
          </v-card>

          <!-- Image and parent collection -->
          <v-card rounded="lg" variant="outlined" class="mb-4">
            <v-card-item>
              <template #title>
                {{ t("collections.sections.organization") }}
              </template>
            </v-card-item>
            <v-divider />
            <v-card-text>
              <div class="d-flex flex-wrap align-start ga-4">
                <v-card v-if="values.imageUrl" width="180" rounded="lg" flat>
                  <v-img :src="values.imageUrl" height="135" cover />

                  <v-card-actions class="pa-2">
                    <v-btn
                      block
                      variant="text"
                      color="error"
                      prepend-icon="mdi-delete-outline"
                      @click="removeImage"
                    >
                      {{ t("common.clear") }}
                    </v-btn>
                  </v-card-actions>
                </v-card>

                <v-btn
                  color="primary"
                  variant="outlined"
                  prepend-icon="mdi-image-multiple-outline"
                  height="40"
                  @click="mediaDialog = true"
                >
                  {{
                    values.imageUrl
                      ? t("collections.actions.changeImage")
                      : t("collections.actions.selectImage")
                  }}
                </v-btn>
              </div>

              <v-label class="mt-5 mb-1 text-body-medium">
                {{ t("collections.fields.parentCollection") }}
              </v-label>
              <v-select
                v-model="parentId"
                :items="parentOptions"
                item-title="title"
                item-value="value"
                variant="outlined"
                density="compact"
                clearable
                hide-details
              />
            </v-card-text>
          </v-card>

          <!-- Collection type -->
          <v-card variant="outlined" rounded="lg" class="mb-4">
            <v-card-item>
              <template #title>
                {{ t("collections.sections.collectionType") }}
              </template>
            </v-card-item>
            <v-divider />

            <v-card-text>
              <v-row density="compact" role="radiogroup">
                <v-col
                  cols="12"
                  sm="6"
                  v-for="option in collectionTypeOptions"
                  :key="option.value"
                >
                  <v-card
                    variant="outlined"
                    class="collection-card"
                    :class="{
                      'collection-card--active':
                        collectionType === option.value,
                    }"
                    role="radio"
                    :aria-checked="collectionType === option.value"
                    @click="collectionType = option.value"
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
                        v-if="collectionType === option.value"
                        icon="mdi-check-circle"
                        color="primary"
                      />
                    </div>
                  </v-card>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <!-- Manual products -->
          <v-card
            v-if="values.type === 'MANUAL'"
            rounded="lg"
            variant="outlined"
          >
            <v-card-item>
              <template #title>
                {{ t("collections.sections.products") }}
              </template>
              <template #append>
                <v-btn
                  color="primary"
                  prepend-icon="mdi-plus"
                  variant="outlined"
                  @click="productVariantDialog = true"
                >
                  {{ t("collections.actions.addProduct") }}
                </v-btn>
              </template>
            </v-card-item>
            <v-divider />

            <v-card-text>
              <div
                v-if="collectionProductsPending"
                class="d-flex justify-center py-8"
              >
                <v-progress-circular indeterminate />
              </div>

              <v-list
                v-else-if="selectedProducts.length"
                lines="two"
                class="pa-0"
              >
                <v-list-item
                  v-for="product in selectedProducts"
                  :key="product.id"
                  :title="product.name"
                  :subtitle="product.slug"
                  rounded="lg"
                >
                  <template #prepend>
                    <v-avatar size="48" rounded="lg" color="surface-variant">
                      <v-img
                        v-if="
                          product.media || product.variants?.[0]?.mediaList?.[0]
                        "
                        :src="
                          product.media || product.variants?.[0]?.mediaList?.[0]
                        "
                        :alt="product.name"
                        cover
                      />

                      <v-icon v-else> mdi-package-variant </v-icon>
                    </v-avatar>
                  </template>

                  <template #append>
                    <v-btn
                      icon="mdi-close"
                      variant="text"
                      color="error"
                      size="small"
                      :aria-label="t('collections.actions.removeProduct')"
                      @click="removeProduct(product.id)"
                    />
                  </template>
                </v-list-item>
              </v-list>

              <div v-else class="py-10 text-center text-medium-emphasis">
                <v-icon
                  icon="mdi-package-variant-closed"
                  size="40"
                  class="mb-2"
                />

                <div class="text-body-medium">
                  {{ t("collections.noProductsSelected") }}
                </div>

                <v-btn
                  color="primary"
                  variant="text"
                  prepend-icon="mdi-plus"
                  class="mt-2"
                  @click="productVariantDialog = true"
                >
                  {{ t("collections.actions.addProduct") }}
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <!-- Automated rules -->
          <v-card v-else rounded="lg" variant="outlined">
            <v-card-item>
              <template #title>
                {{ t("collections.sections.conditions") }}
              </template>
            </v-card-item>
            <v-divider />
            <v-card-text>
              <!-- <p class="text-body-medium text-medium-emphasis mb-4">
                {{ t("collections.conditionsHint") }}
              </p> -->

              <v-btn-toggle
                v-model="matchCondition"
                mandatory
                color="primary"
                variant="outlined"
                divided
                density="compact"
                class="mb-4"
              >
                <v-btn value="ALL">
                  {{ t("collections.match.all") }}
                </v-btn>

                <v-btn value="ANY">
                  {{ t("collections.match.any") }}
                </v-btn>
              </v-btn-toggle>

              <v-alert variant="tonal" type="info" class="mb-4">
                {{
                  values.matchCondition === "ALL"
                    ? t("collections.match.allDescription")
                    : t("collections.match.anyDescription")
                }}
              </v-alert>

              <div
                v-for="(rule, index) in values.rules"
                :key="rule.id ?? index"
                class="rounded-lg pa-3 mb-3"
              >
                <div class="d-flex align-center justify-space-between mb-3">
                  <div class="text-title-small text-body-medium">
                    {{ t("collections.rule", { number: index + 1 }) }}
                  </div>

                  <v-btn
                    icon="mdi-close"
                    variant="text"
                    color="error"
                    size="small"
                    :aria-label="t('collections.actions.removeCondition')"
                    :disabled="saving"
                    @click="removeRule(index)"
                  />
                </div>

                <v-row density="compact">
                  <v-col cols="12" sm="6">
                    <v-label class="mb-1 text-body-medium">
                      {{ t("collections.condition.field") }}
                    </v-label>
                    <v-select
                      :model-value="rule.attribute"
                      :items="attributeOptions"
                      @update:model-value="setRuleAttribute(index, $event)"
                      item-title="title"
                      item-value="value"
                      variant="outlined"
                      density="compact"
                      hide-details
                    />
                  </v-col>

                  <v-col cols="12" sm="6">
                    <v-label class="mb-1 text-body-medium">
                      {{ t("collections.condition.operator") }}
                    </v-label>
                    <v-select
                      :model-value="rule.operator"
                      :items="operatorOptions"
                      @update:model-value="setRuleOperator(index, $event)"
                      item-title="title"
                      item-value="value"
                      variant="outlined"
                      density="compact"
                      hide-details
                    />
                  </v-col>

                  <v-col cols="12">
                    <v-label class="mb-1 text-body-medium">
                      {{ t("collections.condition.value") }}
                    </v-label>
                    <template v-if="rule.attribute === 'TYPE'">
                      <div
                        v-for="(value, valueIndex) in rule.values"
                        :key="`${index}-${valueIndex}`"
                        class="d-flex align-start ga-2 mb-3"
                      >
                        <ProductTypeAutoComplete
                          :model-value="value || null"
                          model-key="name"
                          :disabled="saving"
                          class="flex-grow-1"
                          @update:model-value="
                            setRuleValue(index, valueIndex, $event)
                          "
                        />

                        <v-btn
                          icon="mdi-close"
                          variant="text"
                          color="error"
                          size="small"
                          :aria-label="t('collections.actions.removeCondition')"
                          :disabled="saving"
                          @click="removeRuleValue(index, valueIndex)"
                        />
                      </div>

                      <v-btn
                        prepend-icon="mdi-plus"
                        variant="text"
                        size="small"
                        :disabled="saving"
                        @click="addRuleValue(index)"
                      >
                        {{ t("collections.actions.addAnotherProductType") }}
                      </v-btn>
                    </template>

                    <v-combobox
                      v-else
                      :model-value="rule.values"
                      @update:model-value="setRuleValues(index, $event)"
                      variant="outlined"
                      density="compact"
                      multiple
                      chips
                      closable-chips
                      clearable
                      hide-details="auto"
                      :disabled="saving"
                    />
                  </v-col>
                </v-row>
              </div>

              <div v-if="errors.rules" class="text-error text-body-small mb-3">
                {{ errors.rules }}
              </div>

              <v-btn
                prepend-icon="mdi-plus"
                variant="tonal"
                :disabled="saving"
                @click="addRule"
              >
                {{ t("collections.actions.addCondition") }}
              </v-btn>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Summary -->
        <v-col cols="12" md="4">
          <v-card rounded="lg" variant="outlined" flat>
            <v-card-item>
              <template #title>
                {{ t("collections.sections.summary") }}
              </template>
            </v-card-item>
            <v-divider />
            <v-card-text>
              <div class="text-body-medium text-medium-emphasis mb-1">
                {{ t("collections.fields.title") }}
              </div>

              <div class="text-title-medium text-body-medium mb-4">
                {{ values.name || "—" }}
              </div>

              <div class="text-body-medium text-medium-emphasis mb-1">
                {{ t("collections.fields.type") }}
              </div>

              <v-chip
                :color="values.type === 'AUTOMATED' ? 'primary' : undefined"
                variant="tonal"
                class="mb-4"
              >
                {{ t(`collections.type.${values.type.toLowerCase()}`) }}
              </v-chip>

              <v-divider class="mb-4" />

              <div class="text-body-medium text-medium-emphasis mb-1">
                {{ t("collections.fields.products") }}
              </div>

              <div class="text-headline-small font-weight-bold">
                {{
                  values.type === "MANUAL"
                    ? (values.productIds?.length ?? 0)
                    : t("collections.automaticProducts")
                }}
              </div>

              <p class="text-body-small text-medium-emphasis mt-2">
                {{
                  values.type === "MANUAL"
                    ? t("collections.summary.manual")
                    : t("collections.summary.automated")
                }}
              </p>

              <template v-if="values.type === 'AUTOMATED'">
                <v-divider class="my-4" />

                <div class="text-body-medium text-medium-emphasis mb-1">
                  {{ t("collections.fields.ruleCount") }}
                </div>

                <div class="text-title-large font-weight-bold">
                  {{ values.rules.length }}
                </div>
              </template>
            </v-card-text>

            <v-card-actions class="pa-4">
              <v-btn
                block
                color="primary"
                type="submit"
                :loading="saving"
                :disabled="
                  saving ||
                  collectionPending ||
                  (isEditing && !existingCollection)
                "
              >
                {{
                  isEditing
                    ? t("collections.actions.save")
                    : t("collections.actions.create")
                }}
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </v-form>
  </v-container>

  <ProductVariantDialog v-model="productVariantDialog" @selected="addProduct" />

  <MediaDialog v-model="mediaDialog" @selected="selectImage" />
</template>
<style scoped>
.field-label {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
}

.collection-card {
  height: 100%;
  padding: 16px;

  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.collection-card:hover {
  border-color: rgb(var(--v-theme-primary));
}

.collection-card--active {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.04);
  box-shadow: 0 0 0 1px rgb(var(--v-theme-primary));
}
</style>
