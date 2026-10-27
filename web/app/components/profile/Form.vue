<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
const { t } = useI18n();
const userStore = useUserStore();
const { user } = storeToRefs(userStore);
const snackbar = useSnackbarStore();

const schema = toTypedSchema(
  z.object({
    fullName: z.string().min(1, t("profile.validation.fullNameRequired")),
    email: z.string().email(t("profile.validation.emailInvalid")),
    phone: z.string().min(1, t("profile.validation.phoneRequired")),
  }),
);

const { handleSubmit, defineField, errors, isSubmitting } = useForm({
  validationSchema: schema,
  initialValues: {
    fullName: user.value?.fullName ?? "",
    email: user.value?.email ?? "",
    phone: user.value?.phone ?? "",
  },
});
const { updateProfile } = useProfile();
const [fullName] = defineField("fullName");
const [email] = defineField("email");
const [phone] = defineField("phone");
const onSubmit = handleSubmit(async (values) => {
  const response = await updateProfile(values);
  userStore.setUser(response);
  snackbar.openSnackbar({
    type: "success",
    message: "profile.update.success",
  });
});
</script>
<template>
  <v-card variant="outlined" elevation="0" rounded="lg" :loading="isSubmitting">
    <v-card-item>
      <v-card-title>{{ t("profile.personalInformation") }} </v-card-title>
      <v-card-subtitle>
        {{ t("profile.personalInformationDescription") }}
      </v-card-subtitle>
    </v-card-item>
    <v-divider class="my-0" />

    <v-card-text>
      <v-form @submit.prevent="onSubmit">
        <v-row no-gutters>
          <v-col cols="12">
            <v-label class="mb-2" :text="t('profile.fullName')" />
            <v-text-field
              v-model="fullName"
              variant="outlined"
              density="compact"
              :error-messages="errors.fullName"
            />
          </v-col>

          <v-col cols="12">
            <v-label class="mb-2" :text="t('profile.email')" />
            <v-text-field
              v-model="email"
              type="email"
              variant="outlined"
              density="compact"
              :error-messages="errors.email"
            />
          </v-col>

          <v-col cols="12">
            <v-label class="mb-2" :text="t('profile.phoneNumber')" />
            <v-text-field
              v-model="phone"
              :placeholder="t('ui.phone_example')"
              variant="outlined"
              density="compact"
              :error-messages="errors.phone"
            />
          </v-col>

          <v-col cols="12" class="d-flex justify-end">
            <v-btn type="submit" color="primary" :loading="isSubmitting">
              {{ t("profile.saveChanges") }}
            </v-btn>
          </v-col>
        </v-row>
      </v-form>
    </v-card-text>
  </v-card>
</template>
