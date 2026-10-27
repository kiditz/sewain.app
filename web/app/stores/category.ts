import type { Category } from "~/types/category";

export const useCategoryStore = defineStore("category", () => {
  const categories = ref<Category[]>([]);
  const loading = ref(false);
  const version = ref(0);

  const { findCategories } = useCategory();

  const loadCategories = async () => {
    loading.value = true;

    try {
      categories.value = await findCategories();
    } finally {
      loading.value = false;
    }
  };

  const refresh = async () => {
    version.value++;
    await loadCategories();
  };

  return {
    categories,
    loading,
    version,
    loadCategories,
    refresh,
  };
});
