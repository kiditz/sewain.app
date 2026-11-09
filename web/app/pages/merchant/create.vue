<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";

definePageMeta({
  layout: "auth",
  middleware: ["auth"],
});

const { addMerchant } = useMerchant();
const { t } = useI18n();
const { apiErrorMessage } = useApiError();

const serverError = ref<string | undefined>();


const slugManuallyEdited = ref(false);

const schema = toTypedSchema(
  z.object({
    name: z.string().min(1, t("errors.merchant.name.notblank")),
    slug: z
      .string()
      .min(1, t("errors.slug.notblank"))
      .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, t("errors.merchant.slug.invalid")),
  }),
);

const { defineField, handleSubmit, errors, isSubmitting, setFieldError, meta } =
  useForm({
    validationSchema: schema,
  });

const [name] = defineField("name");
const [slug] = defineField("slug");
watch(name, (value) => {
  if (!slugManuallyEdited.value) {
    slug.value = slugify(value ?? "");
  }
});
const submit = handleSubmit(async (values) => {
  serverError.value = undefined;

  try {
    const merchant = await addMerchant(values);
    useMerchantStore().setCurrent(merchant);
    await navigateTo("/merchant/setup");
  } catch (error: any) {
    const response = error.data;

    if (error.status >= 500) {
      serverError.value = t("errors.server");
      return;
    }

    for (const [field, code] of Object.entries(response?.errors ?? {})) {
      setFieldError(field as "name" | "slug", t(`errors.${code}`));
    }

    if (response?.message && !response?.errors) {
      serverError.value = t(apiErrorMessage(error));
    }
  }
});
</script>

<template>
  <div>
    <h3 class="text-title-large mb-2">
      {{ t("merchant.create.title") }}
    </h3>

    <p class="text-body-medium text-medium-emphasis mb-7">
      {{ t("merchant.create.description") }}
    </p>

    <v-alert
      v-if="serverError"
      closable
      :text="serverError"
      type="error"
      variant="tonal"
      class="mb-5"
    />

    <v-form @submit.prevent="submit" class="mt-6">
      <div class="mb-6">
        <v-label :text="t('merchant.name')" />

        <v-text-field
          v-model="name"
          density="compact"
          variant="outlined"
          hide-details="auto"
          :error-messages="errors.name"
        />
      </div>

      <div class="mb-6">
        <v-label :text="t('merchant.slug')" />

        <v-text-field
          v-model="slug"
          density="compact"
          variant="outlined"
          hide-details="auto"
          :error-messages="errors.slug"
          :hint="$t('ui.contoh_baby_rent')"
          persistent-hint
          @input="slugManuallyEdited = true"
        />
      </div>

      <v-btn
        type="submit"
        color="primary"
        block
        variant="flat"
        size="large"
        :loading="isSubmitting"
        :disabled="!meta.valid"
      >
        {{ t("merchant.create.submit") }}
      </v-btn>
    </v-form>
  </div>
</template>
