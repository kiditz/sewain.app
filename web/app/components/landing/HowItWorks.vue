<script setup lang="ts">
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

if (import.meta.client) {
  gsap.registerPlugin(ScrollTrigger);
}

const steps = [
  {
    number: "01",
    icon: "mdi-account-plus-outline",
    title: "landing.steps.accountTitle",
    description: "landing.steps.accountDescription",
  },
  {
    number: "02",
    icon: "mdi-storefront-outline",
    title: "landing.steps.businessTitle",
    description: "landing.steps.businessDescription",
  },
  {
    number: "03",
    icon: "mdi-rocket-launch-outline",
    title: "landing.steps.rentalTitle",
    description: "landing.steps.rentalDescription",
  },
];

const section = ref<HTMLElement>();

onMounted(() => {
  const ctx = gsap.context(() => {
    gsap.from(".how-header > *", {
      scrollTrigger: {
        trigger: ".how-header",
        start: "top 80%",
      },
      y: 40,
      opacity: 0,
      duration: 0.7,
      stagger: 0.15,
    });

    gsap.from(".how-step", {
      scrollTrigger: {
        trigger: ".how-steps",
        start: "top 80%",
      },
      y: 60,
      opacity: 0,
      duration: 0.8,
      stagger: 0.2,
    });

    gsap.from(".how-line", {
      scrollTrigger: {
        trigger: ".how-steps",
        start: "top 75%",
      },
      scaleX: 0,
      transformOrigin: "left center",
      duration: 1,
      delay: 0.4,
    });
  }, section.value);

  onBeforeUnmount(() => ctx.revert());
});
</script>

<template>
  <section id="how-it-works" ref="section" class="how-it-works">
    <v-container>
      <div class="how-header text-center">
        <v-chip color="primary" variant="tonal" class="mb-4">
          {{ $t("ui.how_it_works") }}
        </v-chip>

        <h2 class="text-h3 text-md-h2 font-weight-bold">
          {{ $t("ui.mulai_dalam_beberapa_langkah") }}
        </h2>

        <p class="text-body-1 text-medium-emphasis mx-auto mt-4">
          {{ $t("ui.tidak_perlu_setup_yang_rumit_buat_akun_dan_mulai_kelola_rental") }}
        </p>
      </div>

      <div class="how-steps">
        <div class="how-line" />

        <div v-for="step in steps" :key="step.number" class="how-step">
          <div class="step-number">
            {{ step.number }}
          </div>

          <v-avatar color="primary" size="72" class="step-icon">
            <v-icon :icon="step.icon" size="32" />
          </v-avatar>

          <h3 class="text-title-medium font-weight-bold mt-6">
            {{ $t(step.title) }}
          </h3>

          <p class="text-body-medium text-medium-emphasis mt-3">
            {{ $t(step.description) }}
          </p>
        </div>
      </div>
    </v-container>
  </section>
</template>
