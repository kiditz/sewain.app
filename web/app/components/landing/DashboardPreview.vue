<script setup lang="ts">
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
const { t } = useI18n()

if (import.meta.client) {
  gsap.registerPlugin(ScrollTrigger)
}

const section = ref<HTMLElement>()
const stats = computed(() => [
  { icon: 'mdi-calendar-check', label: t('landing.dashboard.booking'), value: '24' },
  { icon: 'mdi-package-variant', label: t('landing.dashboard.product'), value: '128' },
  { icon: 'mdi-account-group', label: t('landing.dashboard.customer'), value: '86' },
  { icon: 'mdi-cash-multiple', label: t('landing.dashboard.revenue'), value: t('landing.dashboard.revenueAmount') },
])

onMounted(() => {
  const ctx = gsap.context(() => {
    gsap.from('.dashboard-header > *', {
      scrollTrigger: {
        trigger: '.dashboard-header',
        start: 'top 80%',
      },
      y: 40,
      opacity: 0,
      duration: 0.7,
      stagger: 0.15,
    })

    gsap.from('.dashboard-preview', {
      scrollTrigger: {
        trigger: '.dashboard-preview',
        start: 'top 80%',
      },
      y: 100,
      opacity: 0,
      scale: 0.95,
      duration: 1,
    })
  }, section.value)

  onBeforeUnmount(() => ctx.revert())
})
</script>

<template>
  <section
    ref="section"
    class="dashboard-section"
  >
    <v-container>
      <div class="dashboard-header text-center">
        <v-chip
          color="primary"
          variant="tonal"
          class="mb-4"
        >
          {{ $t("dashboard.title") }}
        </v-chip>

        <h2 class="text-h3 text-md-h2 font-weight-bold">
          {{ $t("ui.semua_terlihat_jelas") }}
        </h2>

        <p class="text-body-1 text-medium-emphasis mx-auto mt-4">
          {{ $t("ui.pantau_aktivitas_rental_kamu_melalui_dashboard_yang_sederhana_da") }}
        </p>
      </div>

      <div class="dashboard-preview">
        <v-card
          rounded="xl"
          elevation="12"
          class="dashboard-window"
        >
          <div class="dashboard-toolbar">
            <div class="d-flex ga-2">
              <span />
              <span />
              <span />
            </div>

            <div class="dashboard-toolbar-title">
              {{ $t("ui.baby_rent") }}
            </div>

            <div />
          </div>

          <div class="dashboard-content">
            <div class="dashboard-sidebar">
              <div class="sidebar-logo">
                <v-icon icon="mdi-storefront-outline" />
              </div>

              <div class="sidebar-item active">
                <v-icon icon="mdi-view-dashboard-outline" />
              </div>

              <div class="sidebar-item">
                <v-icon icon="mdi-package-variant-closed" />
              </div>

              <div class="sidebar-item">
                <v-icon icon="mdi-calendar-month-outline" />
              </div>

              <div class="sidebar-item">
                <v-icon icon="mdi-account-group-outline" />
              </div>
            </div>

            <div class="dashboard-main">
              <div class="dashboard-main-header">
                <div>
                  <div class="text-headline-small font-weight-bold">
                    {{ $t("dashboard.title") }}
                  </div>

                  <div class="text-body-medium text-medium-emphasis">
                    {{ $t("ui.selamat_datang_kembali") }}
                  </div>
                </div>

                <v-btn
                  color="primary"
                  prepend-icon="mdi-plus"
                >
                  {{ $t("ui.tambah_rental") }}
                </v-btn>
              </div>

              <div class="dashboard-stats">
                <v-card
                  v-for="stat in stats"
                  :key="stat.label"
                  variant="outlined"
                  rounded="lg"
                  class="stat-card"
                >
                  <v-card-text>
                    <div class="d-flex align-center ga-3">
                      <v-avatar
                        color="primary"
                        variant="tonal"
                      >
                        <v-icon :icon="stat.icon" />
                      </v-avatar>

                      <div>
                        <div class="text-body-small text-medium-emphasis">
                          {{ stat.label }}
                        </div>

                        <div class="text-title-medium font-weight-bold">
                          {{ stat.value }}
                        </div>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>

              <v-card
                variant="outlined"
                rounded="lg"
                class="dashboard-chart"
              >
                <v-card-text>
                  <div class="d-flex justify-space-between align-center mb-6">
                    <div>
                      <div class="text-title-medium font-weight-bold">
                        {{ $t("ui.aktivitas_rental") }}
                      </div>

                      <div class="text-body-small text-medium-emphasis">
                        {{ $t("finance.period30") }}
                      </div>
                    </div>

                    <v-chip
                      size="small"
                      color="success"
                      variant="tonal"
                    >
                      +18.4%
                    </v-chip>
                  </div>

                  <div class="fake-chart">
                    <span
                      v-for="height in [
                        35, 48, 42, 60, 52, 68, 55, 72, 64, 82,
                        70, 88, 76, 94
                      ]"
                      :key="height"
                      :style="{ height: `${height}%` }"
                    />
                  </div>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </v-card>
      </div>
    </v-container>
  </section>
</template>
