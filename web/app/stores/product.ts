export const useProductStore = defineStore("product", () => {
  const version = ref(0);
  const refresh = () => {
    version.value++;
  };

  return {
    version,
    refresh,
  };
});