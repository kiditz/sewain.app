export const useAttributeStore = defineStore("attribute", () => {
  const version = ref(0);
  const refresh = () => {
    version.value++;
  };

  return {
    version,
    refresh,
  };
});