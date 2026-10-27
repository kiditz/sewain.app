<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
definePageMeta({
  layout: "auth",
  middleware: "logged"
});
const { t } = useI18n();
const { apiErrorMessage } = useApiError();
useHead({ title: () => t("login.title") });
const { login } = useAuth();
const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

const serverError = ref<string | undefined>(undefined);
const serverErrorKey = ref(0);
const schema = toTypedSchema(
  z.object({
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

const [email] = defineField("email");
const [password] = defineField("password");
const submit = handleSubmit(async (values) => {
  try {
    await login(values);
    await navigateTo('/merchant')
  } catch (error: any) {
    const response = error.data;
    if (error.status == 401) {
      serverError.value = t(apiErrorMessage(error));
      serverErrorKey.value++;
      return;
    }

    for (const [field, code] of Object.entries(response?.errors ?? {})) {
      setFieldError(field as "email" | "password", t(`errors.${code}`));
    }
  }
});
</script>

<template>
  <div class="d-flex justify-space-between align-center">
    <h3 class="text-title-large my-0">{{ t("login.title") }}</h3>
    <nuxt-link to="/register" class="text-primary text-decoration-none">{{
      t("login.noAccount")
    }}</nuxt-link>
  </div>
  <!-- <v-divider  class="border-opacity-25 my-5"/> -->
  <v-alert
    :key="serverErrorKey"
    v-if="serverError"
    closable
    :text="serverError"
    type="error"
    variant="tonal"
    class="my-5"
  ></v-alert>

  <v-form @submit.prevent="submit" class="mt-7">
    <div class="mb-6">
      <v-label :text="t('login.email')" />
      <v-text-field
        v-model="email"
        variant="outlined"
        hide-details="auto"
        :error-messages="errors.email"
      />
    </div>
    <div class="mb-6">
      <v-label :text="t('login.password')" />
      <v-text-field
        v-model="password"
        hide-details="auto"
        type="password"
        variant="outlined"
        :error-messages="errors.password"
      />
    </div>
    <div class="d-flex align-center mt-4 mb-7 mb-sm-0">
      <div class="ml-auto">
        <router-link to="/login" class="text-grey-darken-1 link-hover"
          >{{ t("login.forgotPassword") }}
        </router-link>
      </div>
    </div>
    <VBtn
      type="submit"
      color="primary"
      block
      :loading="isSubmitting"
      class="mt-4"
      variant="flat"
      size="large"
      :disabled="!meta.valid"
    >
      {{ t("login.submit") }}
    </VBtn>
  </v-form>
</template>
