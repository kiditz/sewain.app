<script setup lang="ts">
import gsap from 'gsap'

const hero = ref<HTMLElement>()

onMounted(() => {
  const ctx = gsap.context(() => {
    gsap
      .timeline()
      .from('.hero-badge', {
        y: 20,
        opacity: 0,
        duration: 0.6
      })
      .from('.hero-title', {
        y: 60,
        opacity: 0,
        duration: 0.8
      }, '-=0.3')
      .from('.hero-description', {
        y: 30,
        opacity: 0,
        duration: 0.6
      }, '-=0.4')
      .from('.hero-actions', {
        y: 20,
        opacity: 0,
        duration: 0.5
      }, '-=0.3')
      .from('.hero-dashboard', {
        y: 80,
        opacity: 0,
        scale: 0.95,
        duration: 1
      }, '-=0.2')
  }, hero.value)

  onBeforeUnmount(() => ctx.revert())
})
</script>

<template>
  <section
    ref="hero"
    class="hero"
  >
    <div class="hero-background" />

    <v-container>
      <div class="hero-content">
        <div class="hero-badge">
          <v-chip
            color="primary"
            variant="tonal"
          >
            {{ $t("ui.rental_management_made_simple") }}
          </v-chip>
        </div>

        <h1 class="hero-title">
          {{ $t("ui.kelola_rental") }}
          <br />
          <span class="text-primary">
            {{ $t("ui.lebih_mudah") }}
          </span>
        </h1>

        <p class="hero-description">
          {{ $t("ui.kelola_produk_pelanggan_booking_dan_transaksi_rental_dalam_satu") }}
        </p>

        <div class="hero-actions">
          <v-btn
            color="primary"
            size="x-large"
            rounded="lg"
            to="/register"
          >
            {{ $t("ui.get_started") }}
          </v-btn>

          <v-btn
            variant="text"
            size="x-large"
            rounded="lg"
          >
            {{ $t("ui.lihat_demo") }}
          </v-btn>
        </div>

        <!-- <div class="hero-dashboard">
          <v-card rounded="xl">
            <v-img
              src="/dashboard-preview.png"
              aspect-ratio="16/10"
              cover
            />
          </v-card>
        </div> -->
      </div>
    </v-container>
  </section>
</template>
