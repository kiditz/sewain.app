<script setup lang="ts">
import { FileImageOutlined } from "@ant-design/icons-vue";

defineProps<{
  modelValue: string | null | undefined;
}>();
const emit = defineEmits<{
  open: [];
}>();
</script>
<template>
  <div class="d-flex align-center ga-4">
    <v-avatar v-if="modelValue" size="70" rounded>
      <v-img :src="modelValue" cover />
    </v-avatar>

    <v-avatar v-else size="70" rounded color="grey-lighten-3">
      <FileImageOutlined />
    </v-avatar>
    <v-btn class="ms-auto" variant="outlined" @click="emit('open')">
      <FileImageOutlined class="mr-2" />
      {{ $t("ui.choose_image") }}
    </v-btn>
  </div>
</template>
