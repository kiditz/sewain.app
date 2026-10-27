
<script setup lang="ts">
defineProps<{
  modelValue: boolean;
  title: string;
  message: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  confirmed: [];
}>();
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="420"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="lg">
      <v-card-title class="pa-4">
        {{ title }}
      </v-card-title>

      <v-divider />

      <v-card-text>
        {{ message }}
      </v-card-text>

      <v-card-actions>
        <v-spacer />

        <v-btn variant="text" @click="emit('update:modelValue', false)">
          {{ $t("common.cancel") }}
        </v-btn>

        <v-btn color="error" @click="emit('confirmed')">
          {{ $t("common.remove") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
