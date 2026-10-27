<script setup lang="ts">
import DashboardAppBar from "~/components/dashboard/AppBar.vue";
import DashboardNavigation from "~/components/dashboard/Navigation.vue";

</script>

<template>
  <v-app>
    <dashboard-navigation />
    <dashboard-app-bar />
    
    <v-main class="page-wrapper">
      <v-container fluid class="pa-0">
        <snackbar/>    
        <slot />
      </v-container>
    </v-main>
  </v-app>
</template>
