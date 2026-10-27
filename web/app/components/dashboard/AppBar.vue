<script setup lang="ts">
import { MenuOutlined } from "@ant-design/icons-vue";

const customizer = useCustomizerStore();
</script>

<template>
  <v-app-bar elevation="0">
    <!-- <v-app-bar-nav-icon class="d-md-none" /> -->
    <v-btn
      class="hidden-md-and-down text-secondary mx-3"
      color="darkText"
      icon
      rounded="sm"
      variant="text"
      @click.stop="customizer.setMiniSidebar(!customizer.miniSidebar)"
      size="small"
    >
      <MenuOutlined :style="{ fontSize: '16px' }" />
    </v-btn>
    <!-- <v-btn
      variant="text"
      class="d-none d-md-flex"
      append-icon="mdi-chevron-down"
    >
      <div class="text-left">
        <div class="text-body-medium font-weight-medium">Baby Rent</div>

        <div class="text-body-small text-medium-emphasis">Workspace</div>
      </div>
    </v-btn> -->
    <v-sheet class="d-none d-md-block mr-2">
      <div class="d-flex align-center">
        <merchant-list />
      </div>
    </v-sheet>

    <v-spacer />

    <!-- <v-btn icon variant="text" class="mr-2">
      <v-badge dot color="error">
        <v-icon icon="mdi-bell-outline" />
      </v-badge>
    </v-btn> -->
    <auth-profile/>
    <!-- <v-menu v-model="userMenu">
      <template #activator="{ props }">
        <v-btn v-bind="props" variant="text" class="mr-2" rounded="lg">
          <v-avatar size="36" color="primary">
            <span class="text-body-medium font-weight-medium"> RA </span>
          </v-avatar>

          <div class="text-left ml-3 d-none d-sm-block">
            <div class="text-body-medium font-weight-medium">Rifky</div>

            <div class="text-body-small text-medium-emphasis">Owner</div>
          </div>

          <v-icon
            icon="mdi-chevron-down"
            size="18"
            class="ml-2 d-none d-sm-block"
          />
        </v-btn>
      </template>

      <v-list min-width="220">
        <v-list-item
          prepend-icon="mdi-account-outline"
          title="Profile"
          to="/dashboard/profile"
        />

        <v-list-item
          prepend-icon="mdi-cog-outline"
          title="Settings"
          to="/dashboard/settings"
        />

        <v-divider class="my-2" />

        <v-list-item prepend-icon="mdi-logout" title="Logout" />
      </v-list>
    </v-menu> -->
  </v-app-bar>
</template>
