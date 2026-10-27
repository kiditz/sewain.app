<script setup lang="ts">
import { useForm, useFieldArray } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import type { Attribute, AttributeCommand } from "~/types/attribute";

type AttributeFormValues = {
  name: string;
  description: string | null;
  values: {
    id?: string;
    value: string;
  }[];
};

const props = defineProps<{
  attribute?: Attribute | null;
}>();

const emit = defineEmits<{
  saved: [attribute: Attribute];
}>();

const { t } = useI18n();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const store = useAttributeStore();
const { createAttribute, updateAttribute, findAttribute } = useAttribute();

const schema = toTypedSchema(
  z.object({
    name: z
      .string()
      .min(1, t("attribute.validation.nameRequired"))
      .max(100, t("attribute.validation.nameMax")),

    description: z.string().nullable(),

    values: z
      .array(
        z.object({
          id: z.string().uuid().optional(),
          value: z
            .string()
            .min(1, t("attribute.validation.valueRequired"))
            .max(100, t("attribute.validation.valueMax")),
        }),
      )
      .min(1, t("attribute.validation.valuesRequired")),
  }),
);

const { handleSubmit, defineField, errors, isSubmitting, resetForm } =
  useForm<AttributeFormValues>({
    validationSchema: schema,

    initialValues: {
      name: "",
      description: null,
      values: [
        {
          value: "",
        },
      ],
    },
  });

const { fields, push, remove } =
  useFieldArray<AttributeFormValues["values"][number]>("values");

const [name] = defineField("name");
const [description] = defineField("description");

watch(
  () => props.attribute,
  (attribute) => {
    resetForm({
      values: attribute
        ? {
            name: attribute.name,
            description: attribute.description,
            values: attribute.values.map((value) => ({
              id: value.id,
              value: value.value,
            })),
          }
        : {
            name: "",
            description: null,
            values: [
              {
                value: "",
              },
            ],
          },
    });
  },
  {
    immediate: true,
  },
);

const onSubmit = handleSubmit(async (values) => {
  try {
    console.log("SUBMIT VALUES", values);

    const command: AttributeCommand = {
      name: values.name,
      slug: slugify(values.name),
      description: values.description,
      values: values.values.map((item) => ({
        id: item.id,
        value: item.value,
        slug: slugify(item.value),
      })),
    };

    console.log("COMMAND", command);
    let attributeId: string;

    if (props.attribute) {
      await updateAttribute(props.attribute.id, command);
      attributeId = props.attribute.id;

      snackbar.openSnackbar({
        type: "success",
        message: "attribute.update.success",
      });
    } else {
      const created = await createAttribute(command);
      attributeId = created.id;

      snackbar.openSnackbar({
        type: "success",
        message: "attribute.create.success",
      });
    }
    store.refresh();
    const attribute = await findAttribute(attributeId);

    emit("saved", attribute);

    if (!props.attribute) {
      resetForm({
        values: {
          name: "",
          description: null,
          values: [
            {
              value: "",
            },
          ],
        },
      });
    }
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
});

const addValue = () => {
  push({
    value: "",
  });
};

const removeValue = (index: number) => {
  if (fields.value.length === 1) {
    return;
  }

  remove(index);
};
</script>

<template>
  <v-form @submit.prevent="onSubmit">
    <v-row no-gutters>
      <v-col cols="12">
        <v-label :text="t('attribute.name')" class="mb-2" />

        <v-text-field
          v-model="name"
          variant="outlined"
          density="compact"
          :error-messages="errors.name"
          :placeholder="t('attribute.namePlaceholder')"
        />
      </v-col>

      <v-col cols="12">
        <v-label :text="t('attribute.description')" class="mb-2" />

        <v-textarea
          v-model="description"
          variant="outlined"
          density="compact"
          :error-messages="errors.description"
          :placeholder="t('attribute.descriptionPlaceholder')"
          rows="2"
        />
      </v-col>

      <v-col cols="12">
        <div class="d-flex align-center justify-space-between mb-2">
          <v-label :text="t('attribute.values')" />

          <v-btn
            variant="tonal"
            size="small"
            prepend-icon="mdi-plus"
            @click="addValue"
          >
            {{ t("common.add") }}
          </v-btn>
        </div>

        <div
          v-for="(field, index) in fields"
          :key="field.key"
          class="d-flex align-start ga-2 mb-2"
        >
          <v-text-field
            v-model="field.value.value"
            variant="outlined"
            :placeholder="t('attribute.valuePlaceholder')"
            :error-messages="errors[`values[${index}].value`]"
            hide-details="auto"
          />

          <v-btn
            icon="mdi-delete-outline"
            variant="text"
            color="error"
            size="small"
            :disabled="fields.length === 1"
            class="mt-2"
            @click="removeValue(index)"
          />
        </div>

        <div v-if="errors.values" class="text-error text-body-small mt-1">
          {{ errors.values }}
        </div>
      </v-col>

      <v-col cols="12" class="d-flex justify-end">
        <v-btn type="submit" color="primary" :loading="isSubmitting">
          {{ props.attribute ? t("common.update") : t("common.save") }}
        </v-btn>
      </v-col>
    </v-row>
  </v-form>
</template>
