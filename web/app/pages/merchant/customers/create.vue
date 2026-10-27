<script setup lang="ts">
import { toTypedSchema } from "@vee-validate/zod";
import { useForm } from "vee-validate";
import { z } from "zod";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t } = useI18n();
const router = useRouter();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const { createCustomer } = useCustomer();

const schema = toTypedSchema(z.object({
  fullName: z.string().trim().min(1, t("customers.create.validation.fullName")).max(150),
  email: z.string().trim().email(t("customers.create.validation.email")).max(255),
  phone: z.string().trim().max(30).optional(),
  notes: z.string().trim().max(5000).optional(),
}));

const { defineField, errors, handleSubmit, isSubmitting } = useForm({
  validationSchema: schema,
  initialValues: { fullName: "", email: "", phone: "", notes: "" },
});
const [fullName] = defineField("fullName");
const [email] = defineField("email");
const [phone] = defineField("phone");
const [notes] = defineField("notes");

const submit = handleSubmit(async (values) => {
  try {
    await createCustomer({
      fullName: values.fullName,
      email: values.email,
      phone: values.phone || undefined,
      notes: values.notes || undefined,
    });
    snackbar.openSnackbar({ type: "success", message: t("customers.create.successMessage") });
    await router.push("/merchant/customers");
  } catch (error) {
    snackbar.openSnackbar({ type: "error", message: apiErrorMessage(error) });
  }
});
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex align-center ga-3 mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" to="/merchant/customers" :aria-label="t('customers.create.back')" />
      <div>
        <h1 class="text-headline-small font-weight-bold">{{ t("customers.create.title") }}</h1>
        <p class="text-body-medium text-medium-emphasis mt-1">{{ t("customers.create.subtitle") }}</p>
      </div>
    </div>

    <v-row>
      <v-col cols="12" lg="8" xl="7">
        <v-card rounded="lg" variant="outlined">
          <v-card-title class="pa-4">{{ t("customers.create.customerInformation") }}</v-card-title>
          <v-divider />
          <v-card-text class="pa-4">
            <v-form @submit.prevent="submit">
              <v-text-field v-model="fullName" :label="t('customers.create.fullName')" :error-messages="errors.fullName" prepend-inner-icon="mdi-account-outline" variant="outlined" required class="mb-2" />
              <v-text-field v-model="email" :label="t('customers.create.email')" :error-messages="errors.email" prepend-inner-icon="mdi-email-outline" type="email" variant="outlined" required class="mb-2" />
              <v-text-field v-model="phone" :label="t('customers.create.phone')" :error-messages="errors.phone" prepend-inner-icon="mdi-phone-outline" type="tel" variant="outlined" class="mb-2" />
              <v-textarea v-model="notes" :label="t('customers.create.notes')" :placeholder="t('customers.create.notesPlaceholder')" prepend-inner-icon="mdi-text-box-outline" variant="outlined" rows="4" class="mb-2" />
              <v-alert type="info" variant="tonal" class="mb-5">{{ t("customers.create.privacyNotice") }}</v-alert>
              <div class="d-flex flex-wrap justify-end ga-3">
                <v-btn variant="text" to="/merchant/customers">{{ t("customers.create.cancel") }}</v-btn>
                <v-btn color="primary" type="submit" prepend-icon="mdi-content-save-outline" :loading="isSubmitting">{{ t("customers.create.submit") }}</v-btn>
              </div>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4" xl="5">
        <v-card rounded="lg" variant="outlined">
          <v-card-text class="pa-5">
            <v-avatar color="primary-lighten-5" size="56" class="mb-4"><v-icon icon="mdi-account-plus-outline" size="28" color="primary" /></v-avatar>
            <h2 class="text-title-medium font-weight-bold mb-2">{{ t("customers.create.infoTitle") }}</h2>
            <p class="text-body-medium text-medium-emphasis">{{ t("customers.create.infoDescription") }}</p>
            <v-divider class="my-4" />
            <div class="d-flex align-start ga-3 mb-4">
              <v-icon icon="mdi-account-check-outline" color="primary" />
              <div><div class="text-body-medium font-weight-medium">{{ t("customers.create.benefitOneTitle") }}</div><div class="text-body-small text-medium-emphasis">{{ t("customers.create.benefitOneDescription") }}</div></div>
            </div>
            <div class="d-flex align-start ga-3">
              <v-icon icon="mdi-history" color="primary" />
              <div><div class="text-body-medium font-weight-medium">{{ t("customers.create.benefitTwoTitle") }}</div><div class="text-body-small text-medium-emphasis">{{ t("customers.create.benefitTwoDescription") }}</div></div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
