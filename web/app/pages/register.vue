<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";

definePageMeta({
  layout: "auth",
});
const { t } = useI18n();
const { apiErrorMessage } = useApiError();
useHead({ title: () => t("register.title") });
const { register } = useAuth();

const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

const serverError = ref<string | undefined>(undefined);
const serverErrorKey = ref(0);

const schema = toTypedSchema(
  z.object({
    fullName: z.string().min(1, t("errors.fullName.notblank")),

    email: z
      .string()
      .min(1, t("errors.email.notblank"))
      .email(t("errors.email.invalid")),

    password: z
      .string()
      .nonempty(t("errors.password.notblank"))
      .regex(passwordRegex, t("errors.password.invalid")),
  }),
);

const { defineField, handleSubmit, errors, isSubmitting, setFieldError, meta } =
  useForm({
    validationSchema: schema,
  });

const [name] = defineField("fullName");
const [email] = defineField("email");
const [password] = defineField("password");

const submit = handleSubmit(async (values) => {
  serverError.value = undefined;

  try {
    await register(values);

    await navigateTo("/login");
  } catch (error: any) {
    const response = error.data;

    if (error.status >= 500) {
      serverError.value = t("errors.server");
      serverErrorKey.value++;
      return;
    }

    for (const [field, code] of Object.entries(response?.errors ?? {})) {
      setFieldError(
        field as "fullName" | "email" | "password",
        t(`errors.${code}`),
      );
    }

    if (response?.message && !response?.errors) {
      serverError.value = t(apiErrorMessage(error));
      serverErrorKey.value++;
    }
  }
});
</script>

<template>
  <div class="d-flex justify-space-between align-center">
    <h3 class="text-title-large my-0">
      {{ t("register.title") }}
    </h3>

    <nuxt-link to="/login" class="text-primary text-decoration-none">
      {{ t("register.hasAccount") }}
    </nuxt-link>
  </div>

  <v-alert
    v-if="serverError"
    :key="serverErrorKey"
    closable
    :text="serverError"
    type="error"
    variant="tonal"
    class="my-5"
  />

  <v-form class="mt-7" @submit.prevent="submit">
    <div class="mb-6">
      <v-label :text="t('register.fullName')" />

      <v-text-field
        v-model="name"
        variant="outlined"
        hide-details="auto"
        :error-messages="errors.fullName"
      />
    </div>

    <div class="mb-6">
      <v-label :text="t('register.email')" />

      <v-text-field
        v-model="email"
        type="email"
        variant="outlined"
        hide-details="auto"
        :error-messages="errors.email"
      />
    </div>

    <div class="mb-6">
      <v-label :text="t('register.password')" />

      <v-text-field
        v-model="password"
        type="password"
        variant="outlined"
        hide-details="auto"
        :error-messages="errors.password"
      />
    </div>

    <v-btn
      type="submit"
      color="primary"
      block
      :loading="isSubmitting"
      class="mt-4"
      variant="flat"
      size="large"
      :disabled="!meta.valid"
    >
      {{ t("register.submit") }}
    </v-btn>
  </v-form>
</template>
