import { defineStore } from "pinia";
import { config } from "~/types/config";

export const useCustomizerStore = defineStore("customizer", () => {
  const drawer = ref(config.drawer);
  const miniSidebar = ref(config.mini_sidebar);
  const actTheme = ref(config.actTheme);
  const fontTheme = ref(config.fontTheme);

  const toggleDrawer = () => {
    drawer.value = !drawer.value;
  };

  const setMiniSidebar = (value: boolean) => {
    miniSidebar.value = value;
  };

  const setTheme = (value: string) => {
    actTheme.value = value;
  };

  const setFont = (value: string) => {
    fontTheme.value = value;
  };

  return {
    drawer,
    miniSidebar,
    actTheme,
    fontTheme,

    toggleDrawer,
    setMiniSidebar,
    setTheme,
    setFont,
  };
});