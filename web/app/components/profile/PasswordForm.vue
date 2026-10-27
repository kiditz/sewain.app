```vue
<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import { LineOutlined } from "@ant-design/icons-vue";

const { t } = useI18n();

const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const passwordRequirements = [
  "minLength",
  "lowercase",
  "uppercase",
  "number",
  "special",
];

const schema = toTypedSchema(
  z
    .object({
      currentPassword: z
        .string()
        .min(1, t("profile.password.validation.currentRequired")),

      newPassword: z
        .string()
        .min(8, t("profile.password.validation.minLength"))
        .regex(/[a-z]/, t("profile.password.validation.lowercase"))
        .regex(/[A-Z]/, t("profile.password.validation.uppercase"))
        .regex(/[0-9]/, t("profile.password.validation.number"))
        .regex(/[^A-Za-z0-9]/, t("profile.password.validation.special")),

      confirmPassword: z
        .string()
        .min(1, t("profile.password.validation.confirmRequired")),
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      path: ["confirmPassword"],
      message: t("profile.password.validation.confirmNotMatch"),
    }),
);

const { handleSubmit, defineField, errors, isSubmitting, resetForm } = useForm({
  validationSchema: schema,
});

const [currentPassword] = defineField("currentPassword");
const [newPassword] = defineField("newPassword");
const [confirmPassword] = defineField("confirmPassword");

const { changePassword } = useProfile();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const onSubmit = handleSubmit(async (values) => {
  try {
    await changePassword(values);

    resetForm();

    snackbar.openSnackbar({
      type: "success",
      message: "profile.password.update.success",
    });
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
});
</script>

<template>
  <v-card variant="outlined" elevation="0" rounded="lg" :loading="isSubmitting">
    <v-card-item>
      <v-card-title>
        {{ t("profile.password.title") }}
      </v-card-title>

      <v-card-subtitle>
        {{ t("profile.password.description") }}
      </v-card-subtitle>
    </v-card-item>

    <v-divider />

    <v-card-text>
      <v-form @submit.prevent="onSubmit">
        <v-row>
          <!-- Change Password Form -->
          <v-col cols="12" md="7">
            <v-row no-gutters>
              <v-col cols="12">
                <v-label
                  class="mb-2"
                  :text="t('profile.password.currentPassword')"
                />
                <v-text-field
                  v-model="currentPassword"
                  :type="showCurrentPassword ? 'text' : 'password'"
                  :append-inner-icon="
                    showCurrentPassword ? 'mdi-eye-off' : 'mdi-eye'
                  "
                  variant="outlined"
                  density="compact"
                  :error-messages="errors.currentPassword"
                  @click:append-inner="
                    showCurrentPassword = !showCurrentPassword
                  "
                />
              </v-col>

              <v-col cols="12">
                <v-label
                  class="mb-2"
                  :text="t('profile.password.newPassword')"
                />
                <v-text-field
                  v-model="newPassword"
                  :type="showNewPassword ? 'text' : 'password'"
                  :append-inner-icon="
                    showNewPassword ? 'mdi-eye-off' : 'mdi-eye'
                  "
                  variant="outlined"
                  density="compact"
                  :error-messages="errors.newPassword"
                  @click:append-inner="showNewPassword = !showNewPassword"
                />
              </v-col>

              <v-col cols="12">
                <v-label
                  class="mb-2"
                  :text="t('profile.password.confirmPassword')"
                />
                <v-text-field
                  v-model="confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  :append-inner-icon="
                    showConfirmPassword ? 'mdi-eye-off' : 'mdi-eye'
                  "
                  variant="outlined"
                  density="compact"
                  :error-messages="errors.confirmPassword"
                  @click:append-inner="
                    showConfirmPassword = !showConfirmPassword
                  "
                />
              </v-col>
            </v-row>
          </v-col>
          <!-- Password Requirements -->
          <v-col cols="12" md="5">
            <div class="text-title-large font-weight-medium mb-4">
              {{ t("profile.password.requirements.title") }}
            </div>

            <v-list  class="py-0">
              <v-list-item
                v-for="requirement in passwordRequirements"
                :key="requirement"
              >
                <template #prepend>
                  <LineOutlined class="mr-2" />
                </template>

                <v-list-item-title>
                  {{ t(`profile.password.requirements.${requirement}`) }}
                </v-list-item-title>
              </v-list-item>
            </v-list>
          </v-col>
          <v-col cols="12" class="d-flex justify-end">
            <v-btn type="submit" color="primary" :loading="isSubmitting">
              {{ t("profile.password.changePassword") }}
            </v-btn>
          </v-col>
        </v-row>
      </v-form>
    </v-card-text>
  </v-card>
</template>
```
