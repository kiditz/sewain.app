<script setup lang="ts">
import { Field, useFieldArray, useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";

import type { SettingCommand } from "~/types/settings";

definePageMeta({
  middleware: ["merchant"],
});

const SETTING_KEY = "RENTAL_PLAN" as const;

const { t } = useI18n();
const { apiErrorMessage } = useApiError();
const { findByKey, update } = useSettings();

const loading = ref(true);
const settingId = ref("");
const errorMessage = ref("");
const successMessage = ref("");

const durationUnits = ["HOUR", "DAY", "WEEK", "MONTH"] as const;

type DurationUnit = (typeof durationUnits)[number];

type RentalPlan = {
  durationUnit: DurationUnit;
  durationValue: number;
  percentage: number;
};

type RentalPlanForm = {
  plan: RentalPlan[];
};

const schema = computed(() =>
  toTypedSchema(
    z.object({
      plan: z
        .array(
          z.object({
            durationUnit: z.enum(durationUnits),
            durationValue: z.coerce
              .number()
              .int(t("rentalplan.integerDuration"))
              .min(1, t("rentalplan.minimumDuration")),
            percentage: z.coerce
              .number()
              .finite(t("rentalplan.invalidPercentage"))
              .min(0, t("rentalplan.negativePercentage")),
          }),
        )
        .min(1, t("rentalplan.minimumPlans"))
        .superRefine((plans, ctx) => {
          const seen = new Set<string>();

          plans.forEach((item, index) => {
            const key = `${item.durationValue}:${item.durationUnit}`;

            if (seen.has(key)) {
              ctx.addIssue({
                code: "custom",
                path: [index, "durationValue"],
                message: t("rentalplan.duplicateDuration"),
              });
            }

            seen.add(key);
          });
        }),
    }),
  ),
);

const { handleSubmit, errors, resetForm, meta, isSubmitting } =
  useForm<RentalPlanForm>({
    validationSchema: schema,
    initialValues: {
      plan: [
        {
          durationUnit: "DAY",
          durationValue: 1,
          percentage: 3,
        },
      ],
    },
  });

const { fields, push, remove } = useFieldArray<RentalPlan>("plan");

const loadRentalPlan = async () => {
  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const setting = await findByKey(SETTING_KEY);

    const value = setting.value as RentalPlan[];

    settingId.value = setting.id;

    // API root array -> wrapper internal form.
    resetForm({
      values: {
        plan: (Array.isArray(value) ? value : []).map((item) => ({
          ...item,
          durationUnit: item.durationUnit ?? "DAY",
        })),
      },
    });
  } catch (error) {
    errorMessage.value = t(apiErrorMessage(error, "rentalplan.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const addPlan = () => {
  push({
    durationUnit: "DAY",
    durationValue: 1,
    percentage: 3,
  });
};

const removePlan = (index: number) => {
  if (fields.value.length > 1) {
    remove(index);
  }
};

const saveRentalPlan = handleSubmit(async (values) => {
  errorMessage.value = "";
  successMessage.value = "";

  const unitOrder: Record<DurationUnit, number> = {
    HOUR: 0,
    DAY: 1,
    WEEK: 2,
    MONTH: 3,
  };

  const plans = values.plan
    .map((item) => ({
      ...item,
      durationValue: Number(item.durationValue),
      percentage: Number(item.percentage),
    }))
    .sort(
      (a, b) =>
        unitOrder[a.durationUnit] - unitOrder[b.durationUnit] ||
        a.durationValue - b.durationValue,
    );

  try {
    const command: SettingCommand = {
      key: SETTING_KEY,
      value: plans,
    };

    await update(settingId.value, command);

    // Keep the form wrapper after saving.
    resetForm({
      values: {
        plan: plans,
      },
    });

    successMessage.value = t("rentalplan.saveSuccess");
  } catch (error) {
    errorMessage.value = t(apiErrorMessage(error, "rentalplan.saveFailed"));
  }
});

onMounted(loadRentalPlan);
</script>

<template>
  <v-container class="py-6" max-width="1200">
    <!-- Page header -->
    <div class="mb-6">
      <div class="text-headline-medium font-weight-bold">
        {{ t("rentalplan.title") }}
      </div>

      <div class="text-body-medium text-medium-emphasis mt-1">
        {{ t("rentalplan.description") }}
      </div>
    </div>

    <!-- Feedback -->
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

    <v-alert
      v-if="successMessage"
      type="success"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="successMessage = ''"
    >
      {{ successMessage }}
    </v-alert>

    <!-- How templates work -->
    <v-card rounded="lg" variant="outlined" class="mb-5">
      <v-card-text class="pa-5">
        <div class="d-flex align-start ga-3">
          <v-icon icon="mdi-information-outline" color="primary" size="24" />

          <div class="flex-grow-1">
            <div class="text-title-medium font-weight-medium mb-2">
              {{ t("rentalplan.templateInfoTitle") }}
            </div>

            <div class="text-body-medium text-medium-emphasis mb-4">
              {{ t("rentalplan.templateInfoDescription") }}
            </div>

            <v-row>
              <v-col cols="12" md="4">
                <div class="d-flex align-start ga-2">
                  <v-icon
                    icon="mdi-calendar-clock-outline"
                    color="primary"
                    size="20"
                  />

                  <div>
                    <div class="text-body-medium font-weight-medium">
                      {{ t("rentalplan.templateDurationTitle") }}
                    </div>
                    <div class="text-body-small text-medium-emphasis">
                      {{ t("rentalplan.templateDurationDescription") }}
                    </div>
                  </div>
                </div>
              </v-col>

              <v-col cols="12" md="4">
                <div class="d-flex align-start ga-2">
                  <v-icon
                    icon="mdi-percent-outline"
                    color="primary"
                    size="20"
                  />

                  <div>
                    <div class="text-body-medium font-weight-medium">
                      {{ t("rentalplan.templatePricingTitle") }}
                    </div>
                    <div class="text-body-small text-medium-emphasis">
                      {{ t("rentalplan.templatePricingDescription") }}
                    </div>
                  </div>
                </div>
              </v-col>

              <v-col cols="12" md="4">
                <div class="d-flex align-start ga-2">
                  <v-icon
                    icon="mdi-package-variant-closed"
                    color="primary"
                    size="20"
                  />

                  <div>
                    <div class="text-body-medium font-weight-medium">
                      {{ t("rentalplan.templateProductTitle") }}
                    </div>
                    <div class="text-body-small text-medium-emphasis">
                      {{ t("rentalplan.templateProductDescription") }}
                    </div>
                  </div>
                </div>
              </v-col>
            </v-row>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- Rental plan configuration -->
    <v-card variant="outlined" flat>
      <div
        class="d-flex flex-wrap align-center justify-space-between ga-3 pa-5"
      >
        <div>
          <div class="text-title-medium">
            {{ t("rentalplan.listTitle") }}
          </div>

          <div class="text-body-small text-medium-emphasis mt-1">
            {{ t("rentalplan.listDescription") }}
          </div>
        </div>

        <v-btn
          color="primary"
          variant="outlined"
          prepend-icon="mdi-plus"
          :disabled="isSubmitting"
          @click="addPlan"
        >
          {{ t("rentalplan.addDuration") }}
        </v-btn>
      </div>

      <v-divider />

      <v-card-text v-if="loading" class="pa-5">
        <v-skeleton-loader type="list-item-three-line" />
        <v-skeleton-loader type="list-item-three-line" />
      </v-card-text>

      <v-card-text v-else class="pa-5">
        <v-form @submit.prevent="saveRentalPlan">
          <div v-for="(field, index) in fields" :key="field.key" class="mb-4">
            <v-card rounded="lg" border flat>
              <v-card-text class="pa-4">
                <div class="d-flex align-center ga-3 mb-4">
                  <v-avatar color="primary" variant="tonal" size="36">
                    {{ index + 1 }}
                  </v-avatar>

                  <div class="flex-grow-1">
                    <div class="text-title-medium">
                      {{ t("rentalplan.plan", { number: index + 1 }) }}
                    </div>

                    <div class="text-body-small text-medium-emphasis">
                      {{ t("rentalplan.planHint") }}
                    </div>
                  </div>

                  <v-btn
                    icon="mdi-delete-outline"
                    variant="text"
                    color="error"
                    :aria-label="
                      t('rentalplan.removePlan', { number: index + 1 })
                    "
                    :disabled="isSubmitting || fields.length <= 1"
                    @click="removePlan(index)"
                  />
                </div>

                <v-row>
                  <v-col cols="12" sm="4">
                    <Field
                      :name="`plan[${index}].durationValue`"
                      v-slot="{ value, handleChange, errors: fieldErrors }"
                    >
                      <v-text-field
                        :model-value="value"
                        :label="t('rentalplan.duration')"
                        type="number"
                        min="1"
                        step="1"
                        variant="outlined"
                        :error-messages="fieldErrors"
                        :disabled="isSubmitting"
                        @update:model-value="handleChange"
                      />
                    </Field>
                  </v-col>

                  <v-col cols="12" sm="4">
                    <Field
                      :name="`plan[${index}].durationUnit`"
                      v-slot="{ value, handleChange, errors: fieldErrors }"
                    >
                      <v-select
                        :model-value="value"
                        :label="t('rentalplan.durationUnit')"
                        :items="
                          durationUnits.map((unit) => ({
                            title: t(`rentalplan.durationUnitOptions.${unit}`),
                            value: unit,
                          }))
                        "
                        variant="outlined"
                        :error-messages="fieldErrors"
                        :disabled="isSubmitting"
                        @update:model-value="handleChange"
                      />
                    </Field>
                  </v-col>

                  <v-col cols="12" sm="4">
                    <Field
                      :name="`plan[${index}].percentage`"
                      v-slot="{ value, handleChange, errors: fieldErrors }"
                    >
                      <v-text-field
                        :model-value="value"
                        :label="t('rentalplan.percentage')"
                        type="number"
                        min="0"
                        step="any"
                        suffix="%"
                        variant="outlined"
                        :error-messages="fieldErrors"
                        :disabled="isSubmitting"
                        @update:model-value="handleChange"
                      />
                    </Field>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </div>

          <v-alert
            v-if="Object.keys(errors).length > 0"
            type="error"
            variant="tonal"
            density="compact"
            class="mb-4"
          >
            {{
              Object.values(errors).find((error) => typeof error === "string")
            }}
          </v-alert>

          <v-divider class="mb-5" />

          <div class="d-flex flex-wrap align-center justify-space-between ga-3">
            <div class="text-body-small text-medium-emphasis">
              {{ t("rentalplan.validationHint") }}
            </div>

            <div class="d-flex justify-end ga-2">
              <v-btn
                variant="text"
                :disabled="loading || isSubmitting"
                @click="loadRentalPlan"
              >
                {{ t("rentalplan.reset") }}
              </v-btn>

              <v-btn
                type="submit"
                color="primary"
                prepend-icon="mdi-content-save-outline"
                :loading="isSubmitting"
                :disabled="loading || !meta.dirty"
              >
                {{ t("rentalplan.save") }}
              </v-btn>
            </div>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
    <v-alert class="mt-3" type="info" variant="tonal" density="compact">
      {{ t("rentalplan.percentageHint") }}
    </v-alert>
  </v-container>
</template>
