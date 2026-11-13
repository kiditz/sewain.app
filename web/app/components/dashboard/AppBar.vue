<script setup lang="ts">
import { useDisplay } from "vuetify";
import { MenuOutlined } from "@ant-design/icons-vue";

const shell = useDashboardShellStore();
const { mdAndDown } = useDisplay();
const { t } = useI18n();
function toggleNavigation() {
  if (mdAndDown.value) shell.toggleDrawer();
  else shell.toggleCollapsed();
}
</script>

<template>
  <v-app-bar elevation="0">

    <v-btn
      class="text-secondary mx-2 flex-shrink-0"
      :aria-label="t('ui.menu')"
      aria-controls="dashboard-navigation"
      :aria-expanded="mdAndDown ? shell.drawerOpen : !shell.collapsed"
      color="darkText"
      icon
      rounded="sm"
      variant="text"
      @click.stop="toggleNavigation"
      size="small"
    >
      <MenuOutlined :style="{ fontSize: '16px' }" />
    </v-btn>

    <v-sheet class="dashboard-store-name mr-2">
      <div class="d-flex align-center">
        <merchant-list />
      </div>
    </v-sheet>

    <v-spacer />

    <auth-profile/>

  </v-app-bar>
</template>

<style scoped>
.dashboard-store-name { min-width: 0; max-width: min(40vw, 240px); overflow: hidden; }
.dashboard-store-name :deep(.v-btn) { max-width: 100%; }
.dashboard-store-name :deep(.v-btn__content) { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
