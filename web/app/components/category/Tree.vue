<script setup lang="ts">
import type { Category } from "~/types/category";

const props = defineProps<{
  categories: Category[];
}>();

const categoryTree = computed(() => buildCategoryTree(props.categories));
</script>

<template>
  <v-treeview
    :items="categoryTree"
    item-title="name"
    item-value="id"
    
    open-on-click
  />
</template>
