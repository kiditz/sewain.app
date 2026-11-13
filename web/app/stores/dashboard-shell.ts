import { defineStore } from 'pinia';

/** Navigation state only; responsive behavior is managed by the dashboard components. */
export const useDashboardShellStore = defineStore('dashboard-shell', () => {
  const drawerOpen = ref(true);
  const collapsed = ref(false);
  function toggleDrawer() { drawerOpen.value = !drawerOpen.value; }
  function toggleCollapsed() { collapsed.value = !collapsed.value; }
  return { drawerOpen, collapsed, toggleDrawer, toggleCollapsed };
});
