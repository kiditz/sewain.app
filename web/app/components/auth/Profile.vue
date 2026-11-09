<script setup lang="ts">
const { findProfile } = useProfile();
const account = await findProfile();
const store = useUserStore();
store.setUser(account);
const { user, initials } = storeToRefs(store);
</script>
<template>
  <v-menu offset="8, 0" v-if="user" :close-on-content-click="false">
    <template #activator="{ props }">
      <v-btn class="profileBtn mr-2 flex-shrink-0" variant="text" rounded="lg" v-bind="props">
        <v-avatar size="32" color="primary">
          <span class="text-body-medium"> {{ initials }} </span>
        </v-avatar>
        <div class="profile-identity text-left ml-3 d-none d-sm-block">
          <div class="text-body-medium font-weight-medium">
            {{ user.fullName }}
          </div>
          <div class="text-body-small text-medium-emphasis">
            {{ user.email }}
          </div>
        </div>

        <v-icon
          icon="mdi-chevron-down"
          size="24"
          class="ml-2 d-none d-sm-block"
        />
      </v-btn>
    </template>
    <v-sheet rounded="md" width="290" style="max-width: calc(100vw - 24px)">
      <auth-profile-detail />
    </v-sheet>
  </v-menu>
</template>

<style scoped>
.profile-identity { max-width: clamp(100px, 15vw, 220px); min-width: 0; }
.profile-identity > div { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
