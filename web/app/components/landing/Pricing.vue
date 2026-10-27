<script setup lang="ts">
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

if (import.meta.client) {
  gsap.registerPlugin(ScrollTrigger);
}

const plans = [
  {
    name: "Free",
    description: "landing.plans.freeDescription",
    price: "0",
    features: [
      "landing.plans.workspaceOne",
      "landing.plans.productsTen",
      "landing.plans.basicBooking",
      "landing.plans.basicCustomers",
    ],
    popular: false,
  },
  {
    name: "Business",
    description: "landing.plans.businessDescription",
    price: "99",
    features: [
      "landing.plans.workspaceOne",
      "landing.plans.productsUnlimited",
      "landing.plans.bookingManagement",
      "landing.plans.customerManagement",
      "landing.plans.dashboardStats",
      "landing.plans.prioritySupport",
    ],
    popular: true,
  },
  {
    name: "Pro",
    description: "landing.plans.proDescription",
    price: "199",
    features: [
      "landing.plans.workspaceFive",
      "landing.plans.productsUnlimited",
      "landing.plans.advancedBooking",
      "landing.plans.advancedStats",
      "landing.plans.multipleUsers",
      "landing.plans.prioritySupport",
    ],
    popular: false,
  },
];

const section = ref<HTMLElement>();

onMounted(() => {
  const ctx = gsap.context(() => {
    gsap.from(".pricing-header > *", {
      scrollTrigger: {
        trigger: ".pricing-header",
        start: "top 80%",
      },
      y: 40,
      opacity: 0,
      duration: 0.7,
      stagger: 0.15,
    });

    gsap.from(".pricing-card", {
      scrollTrigger: {
        trigger: ".pricing-grid",
        start: "top 80%",
      },
      y: 60,
      opacity: 0,
      duration: 0.8,
      stagger: 0.15,
    });
  }, section.value);

  onBeforeUnmount(() => ctx.revert());
});
</script>

<template>
  <section id="pricing" ref="section" class="pricing-section">
    <v-container>
      <div class="pricing-header text-center">
        <v-chip color="primary" variant="tonal" class="mb-4"> {{ $t("ui.pricing") }} </v-chip>

        <h2 class="text-h3 text-md-h2 font-weight-bold">
          {{ $t("ui.pilih_paket_yang_sesuai") }}
        </h2>

        <p class="text-body-1 text-medium-emphasis mx-auto mt-4">
          {{ $t("ui.mulai_gratis_dan_upgrade_ketika_bisnis_kamu_berkembang") }}
        </p>
      </div>

      <div class="pricing-grid">
        <v-card
          v-for="plan in plans"
          :key="plan.name"
          class="pricing-card"
          :class="{ 'pricing-card-popular': plan.popular }"
          rounded="xl"
          variant="flat"
        >
          <div v-if="plan.popular" class="pricing-popular">{{ $t("ui.paling_populer") }}</div>

          <v-card-text class="pa-7">
            <div class="text-overline text-primary font-weight-bold">
              {{ plan.name }}
            </div>

            <p class="text-body-medium text-medium-emphasis mt-3">
                    {{ $t(plan.description) }}
            </p>

            <div class="pricing-price">
              <span class="pricing-currency">Rp</span>
              <span class="pricing-amount">{{ plan.price }}</span>
              <span class="pricing-period">{{ $t("ui.rb_bulan") }}</span>
            </div>

            <v-btn
              block
              size="large"
              :color="plan.popular ? 'primary' : undefined"
              :variant="plan.popular ? 'flat' : 'outlined'"
              rounded="lg"
              class="mt-6"
              to="/register"
            >
              {{ $t("ui.get_started") }}
            </v-btn>

            <v-divider class="my-7" />

            <div class="pricing-features">
              <div
                v-for="feature in plan.features"
                :key="feature"
                class="pricing-feature"
              >
                <v-icon icon="mdi-check-circle" color="primary" size="18" />

                <span class="text-body-medium">
                      {{ $t(feature) }}
                </span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </v-container>
  </section>
</template>
