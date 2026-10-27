<script setup lang="ts">
import { storeToRefs } from "pinia";

const snackbarStore = useSnackbarStore();
const { t } = useI18n();
const { timeout, message, show, color, header, type } =
  storeToRefs(snackbarStore);

const { hideSnackbar } = snackbarStore;

const icon = computed(() => {
  switch (type.value) {
    case "success":
      return "mdi-check-circle";
    case "error":
      return "mdi-alert-circle";
    case "warning":
      return "mdi-alert";
    default:
      return "mdi-information";
  }
});
const snackbarTitle = computed(() => {
  return header.value || t(`snackbar.${type.value}.title`);
});
const close = () => {
  snackbarStore.hideSnackbar();
};
</script>

<template>
  <v-snackbar
    :model-value="show"
    location="top right"
    :timeout="timeout"
    :color="color"
    variant="flat"
    @update:model-value="(show) => !show && close()"
  >
    <div class="d-flex align-center">
      <v-icon :icon="icon" color="white" class="mr-3" />

      <div>
        <div v-if="header" class="font-weight-medium">
          {{ snackbarTitle }}
        </div>

        <div>
          {{ t(message) }}
        </div>
      </div>
    </div>

    <template #actions>
      <v-btn color="white" variant="tonal" @click="hideSnackbar"> {{ $t("inventory.details.close") }} </v-btn>
    </template>
  </v-snackbar>
</template>
