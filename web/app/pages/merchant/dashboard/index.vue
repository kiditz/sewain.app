<script setup lang="ts">
definePageMeta({
  layout: "merchant",
  middleware: ['auth', 'merchant']
});
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div>
        <h1 class="text-headline-medium font-weight-bold">{{ $t("dashboard.title") }}</h1>

        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ $t("ui.overview_bisnis_rental_kamu") }}
        </p>
      </div>

      <v-btn color="primary" prepend-icon="mdi-plus" rounded="lg">
        {{ $t("ui.new_booking") }}
      </v-btn>
    </div>

    <v-row>
      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg">
          <v-card-text>
            <div class="text-body-medium text-medium-emphasis">{{ $t("ui.total_products") }}</div>

            <div class="text-headline-medium font-weight-bold mt-2">128</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg">
          <v-card-text>
            <div class="text-body-medium text-medium-emphasis">{{ $t("ui.active_bookings") }}</div>

            <div class="text-headline-medium font-weight-bold mt-2">24</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg">
          <v-card-text>
            <div class="text-body-medium text-medium-emphasis">{{ $t("menu.customers") }}</div>

            <div class="text-headline-medium font-weight-bold mt-2">86</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg">
          <v-card-text>
            <div class="text-body-medium text-medium-emphasis">{{ $t("finance.revenue") }}</div>

            <div class="text-headline-medium font-weight-bold mt-2">{{ $t("ui.rp_12_4_jt") }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
