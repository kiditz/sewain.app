<script setup lang="ts">
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

if (import.meta.client) {
  gsap.registerPlugin(ScrollTrigger);
}

const features = [
  {
    icon: "mdi-package-variant-closed",
    title: "landing.features.productTitle",
    description: "landing.features.productDescription",
  },
  {
    icon: "mdi-calendar-check",
    title: "landing.features.bookingTitle",
    description: "landing.features.bookingDescription",
  },
  {
    icon: "mdi-account-group-outline",
    title: "landing.features.customerTitle",
    description: "landing.features.customerDescription",
  },
  {
    icon: "mdi-chart-line",
    title: "landing.features.businessTitle",
    description: "landing.features.businessDescription",
  },
  {
    icon: "mdi-cash-multiple",
    title: "landing.features.transactionTitle",
    description: "landing.features.transactionDescription",
  },
  {
    icon: "mdi-cellphone-link",
    title: "landing.features.accessTitle",
    description: "landing.features.accessDescription",
  },
];

const section = ref<HTMLElement>();

onMounted(() => {
  const ctx = gsap.context(() => {
    gsap.from(".features-header > *", {
      scrollTrigger: {
        trigger: ".features-header",
        start: "top 80%",
      },
      y: 40,
      opacity: 0,
      duration: 0.7,
      stagger: 0.15,
    });

    gsap.from(".feature-card", {
      scrollTrigger: {
        trigger: ".features-grid",
        start: "top 80%",
      },
      y: 50,
      opacity: 0,
      duration: 0.7,
      stagger: 0.1,
    });
  }, section.value);

  onBeforeUnmount(() => ctx.revert());
});
</script>

<template>
  <section id="features" ref="section" class="features">
    <v-container>
      <div class="features-header text-center">
        <v-chip color="primary" variant="tonal" class="mb-4"> {{ $t("ui.features") }} </v-chip>

        <h2 class="text-h3 text-md-h2 font-weight-bold">
          {{ $t("ui.semua_yang_kamu_butuhkan") }}
        </h2>

        <p class="text-body-1 text-medium-emphasis mx-auto mt-4">
          {{ $t("ui.kelola_operasional_rental_tanpa_harus_berpindah_pindah_aplikasi") }}
        </p>
      </div>

      <div class="features-grid">
        <v-card
          v-for="feature in features"
          :key="feature.title"
          class="feature-card"
          rounded="xl"
          variant="flat"
        >
          <v-card-text class="pa-6">
            <v-avatar color="primary" variant="tonal" size="52" class="mb-5">
              <v-icon :icon="feature.icon" />
            </v-avatar>

            <h3 class="text-title-medium font-weight-bold">
                {{ $t(feature.title) }}
            </h3>

            <p class="text-body-medium text-medium-emphasis mt-3 mb-0">
                {{ $t(feature.description) }}
            </p>
          </v-card-text>
        </v-card>
      </div>
    </v-container>
  </section>
</template>
