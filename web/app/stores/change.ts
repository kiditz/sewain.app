export const useObjectChanges = defineStore("objectChange", () => {
  const anyChange = ref(false);
  const setChanges = (change: boolean) => {
    anyChange.value = change;
  };
  return { setChanges, anyChange };
});
