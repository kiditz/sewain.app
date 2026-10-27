<script setup lang="ts">
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

if (import.meta.client) {
  gsap.registerPlugin(ScrollTrigger)
}

const section = ref<HTMLElement>()

onMounted(() => {
  const ctx = gsap.context(() => {
    gsap.from('.cta-content > *', {
      scrollTrigger: {
        trigger: '.cta-content',
        start: 'top 80%',
      },
      y: 40,
      opacity: 0,
      duration: 0.7,
      stagger: 0.12,
    })

    gsap.to('.cta-orb', {
      x: 40,
      y: -30,
      duration: 4,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
    })
  }, section.value)

  onBeforeUnmount(() => ctx.revert())
})
</script>

<template>
  <section
    ref="section"
    class="cta-section"
  >
    <v-container>
      <div class="cta-wrapper">
        <div class="cta-orb cta-orb-left" />
        <div class="cta-orb cta-orb-right" />

        <div class="cta-content text-center">
          <v-chip
            variant="tonal"
            class="mb-5"
          >
            {{ $t("ui.get_started") }}
          </v-chip>

          <h2 class="text-h3 text-md-h2 font-weight-bold">
            {{ $t("ui.siap_membuat_bisnis_rental") }}
            <span>{{ $t("ui.lebih_mudah") }}</span>
          </h2>

          <p class="text-body-1 mt-5">
            {{ $t("ui.mulai_kelola_produk_booking_dan_pelanggan_dalam_satu_platform") }}
          </p>

          <div class="cta-actions">
            <v-btn
              size="x-large"
              color="primary"
              rounded="lg"
              to="/register"
            >
              {{ $t("ui.buat_akun_gratis") }}
            </v-btn>

            <v-btn
              size="x-large"
              variant="text"
              rounded="lg"
              to="/login"
            >
              {{ $t("register.hasAccount") }}
            </v-btn>
          </div>
        </div>
      </div>
    </v-container>
  </section>
</template>