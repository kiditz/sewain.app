<script lang="ts" setup>
const { current } = storeToRefs(useMerchantStore());
</script>
<template>
  <v-btn variant="tonal" density="comfortable">
    {{ current?.name }}
  </v-btn>
  <!-- <v-menu>
    <template #activator="{ props }">
      <
    </template>
    <v-sheet rounded="md" width="280" class="mt-2">
      <v-list class="py-0">
        <v-list-item
          class="px-3"
          v-for="merchant in merchants"
          :key="merchant.id"
          :class="{
            'bg-grey-lighten-3': merchant.id === current?.id,
          }"
          :title="merchant.name"
          @click="setCurrent(merchant)"
        />
      </v-list>
      <v-divider />
      <div class="py-3 px-3 d-flex justify-center">
        <nuxt-link to="/merchant/create" class="text-decoration-none">
          <v-btn
            class="ml-1"
            color="primary"
            variant="tonal"
            rounded="lg"
            block
            prepend-icon="mdi-plus"
            text=""
          >
            {{ t("merchant.add.title") }}
          </v-btn>
        </nuxt-link>
      </div>
    </v-sheet>
  </v-menu> -->
</template>
