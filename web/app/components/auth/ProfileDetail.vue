<script setup lang="ts">
import { EditOutlined, LockOutlined, LogoutOutlined } from "@ant-design/icons-vue";

const { user, initials } = storeToRefs(useUserStore());
const auth = useAuth();

const logout = () => {
  auth.logout();
  return navigateTo("/login", { replace: true });
};
</script>

<template>
  <div class="d-flex align-center pa-2">
    <v-avatar size="36" color="primary" class="mr-3">
      <span class="text-body-medium">{{ initials }}</span>
    </v-avatar>
    <div>
      <div class="text-body-medium font-weight-medium mb-0">
        {{ user?.fullName || "Guest" }}
      </div>
      <p class="text-body-small text-medium-emphasis mb-0">
        {{ user?.email || "" }}
      </p>
    </div>
    <div class="ml-auto">
      <v-btn
        variant="text"
        color="primary"
        rounded="sm"
        icon
        size="large"
        :aria-label="$t('ui.logout')"
        @click="logout"
      >
        <LogoutOutlined />
      </v-btn>
    </div>
  </div>

  <v-list class="py-0" :aria-label="$t('ui.profile_list')">
    <v-list-item to="/profile" color="primary" rounded="0">
      <template #prepend>
        <EditOutlined :style="{ fontSize: '14px' }" class="mr-4" />
      </template>
      <v-list-item-title>{{ $t("ui.edit_profile") }}</v-list-item-title>
    </v-list-item>
    <v-list-item to="/profile/password" color="primary" rounded="0">
      <template #prepend>
        <LockOutlined :style="{ fontSize: '14px' }" class="mr-4" />
      </template>
      <v-list-item-title>{{ $t("common.change_password") }}</v-list-item-title>
    </v-list-item>
    <v-list-item color="primary" rounded="0" @click="logout">
      <template #prepend>
        <LogoutOutlined :style="{ fontSize: '14px' }" class="mr-4" />
      </template>
      <v-list-item-title>{{ $t("ui.logout") }}</v-list-item-title>
    </v-list-item>
  </v-list>
</template>
