<script setup lang="ts">
import { UserOutlined, LockOutlined } from "@ant-design/icons-vue";
const store = useUserStore();
const { user, initials } = storeToRefs(store);
const { t } = useI18n();
</script>
<template>
  <v-card variant="outlined" elevation="0" rounded="lg">
    <v-card-item class="d-flex flex-column align-center text-center py-8">
      <v-avatar color="primary" size="120" class="mb-4">
        <v-img v-if="user?.photoUrl" :src="user.photoUrl" cover />
        <span v-else class="text-headline-medium font-weight-medium">
          {{ initials }}
        </span>
      </v-avatar>
      <div class="text-title-large font-weight-medium">
        {{ user?.fullName }}
      </div>
      <div class="text-body-medium text-medium-emphasis mt-1">
        {{ user?.email }}
      </div>
    </v-card-item>
    <v-list color="secondary" >
      <v-list-item to="/profile" color="primary">
        <template #prepend>
          <UserOutlined />
        </template>
        <v-list-item-title class="ml-2">
          {{ t("profile.personalInformation") }}
        </v-list-item-title>
      </v-list-item>

      <v-list-item to="/profile/password" color="primary">
        <template #prepend>
          <LockOutlined />
        </template>

        <v-list-item-title class="ml-2"> {{ t("profile.changePassword") }} </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-card>
</template>
