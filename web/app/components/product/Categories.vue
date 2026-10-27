<script setup lang="ts">
import type { Category } from "~/types/category";

const props = defineProps<{
  modelValue: string[];
}>();

const emit = defineEmits<{
  "update:modelValue": [value: string[]];
}>();

const categories = ref<Category[]>([]);
const { findCategories } = useCategory();

const categoryTree = computed(() => buildCategoryTree(categories.value));

const selected = computed({
  get: () => props.modelValue,
  set: (value: string[]) => emit("update:modelValue", value),
});

categories.value = await findCategories();
</script>

<template>
  <nuxt-scrollbar style="max-height: 200px">
    <v-treeview
      density="compact"
      hide-actions
      v-model:selected="selected"
      :items="categoryTree"
      item-title="name"
      item-value="id"
      selectable
      select-strategy="trunk"
      open-all
    />
  </nuxt-scrollbar>
</template>
