<script setup lang="ts">
import type { Setting, SettingCommand } from '~/types/settings'

definePageMeta({
  layout: 'merchant',
  middleware: ['auth', 'merchant'],
})

interface RentalSettings {
  minimumNoticeHours: number
  maximumAdvanceDays: number
  bufferHours: number
  operatingDays: string[]
  openingTime: string
  closingTime: string
  lateReturnEnabled: boolean
  lateReturnGraceHours: number
}

const { findByKey, create, update } = useSettings()
const { locale, t } = useI18n()
useHead({ title: () => t("menu.rentalSettings") });
const { apiErrorMessage } = useApiError()

const defaultSettings = (): RentalSettings => ({
  minimumNoticeHours: 2,
  maximumAdvanceDays: 90,
  bufferHours: 0,
  operatingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'],
  openingTime: '09:00',
  closingTime: '18:00',
  lateReturnEnabled: true,
  lateReturnGraceHours: 1,
})

const rental = reactive<RentalSettings>(defaultSettings())

const weekdays = computed(() => {
  const values = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
  const formatter = new Intl.DateTimeFormat(locale.value, { weekday: 'long', timeZone: 'UTC' })
  return values.map((value, index) => ({
    value,
    title: formatter.format(new Date(Date.UTC(2024, 0, index + 1))),
  }))
})

const loading = ref(false)
const saving = ref(false)
const saved = ref(false)
const errorMessage = ref('')

const isNotFound = (error: unknown) => {
  const e = error as {
    status?: number
    statusCode?: number
    response?: { status?: number }
  }

  return (
    e.status === 404 ||
    e.statusCode === 404 ||
    e.response?.status === 404
  )
}

const getSetting = async (key: string): Promise<Setting | null> => {
  try {
    return await findByKey(key)
  } catch (error) {
    if (isNotFound(error)) {
      return null
    }

    throw error
  }
}

const loadSettings = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const setting = await getSetting('RENTAL')

    Object.assign(rental, defaultSettings())

    if (setting?.value && typeof setting.value === 'object') {
      const value = setting.value as Partial<RentalSettings>

      rental.minimumNoticeHours =
        value.minimumNoticeHours ?? rental.minimumNoticeHours

      rental.maximumAdvanceDays =
        value.maximumAdvanceDays ?? rental.maximumAdvanceDays

      rental.bufferHours =
        value.bufferHours ?? rental.bufferHours

      rental.operatingDays =
        value.operatingDays ?? rental.operatingDays

      rental.openingTime =
        value.openingTime ?? rental.openingTime

      rental.closingTime =
        value.closingTime ?? rental.closingTime

      rental.lateReturnEnabled =
        value.lateReturnEnabled ?? rental.lateReturnEnabled

      rental.lateReturnGraceHours =
        value.lateReturnGraceHours ?? rental.lateReturnGraceHours

    }
  } catch (error) {
    errorMessage.value = t(apiErrorMessage(error, "rentalSettings.errors.loadFailed"))
  } finally {
    loading.value = false
  }
}

const saveSetting = async (
  key: string,
  value: Record<string, unknown>,
) => {
  const command: SettingCommand = { key, value }
  const existing = await getSetting(key)

  if (existing) {
    await update(existing.id, command)
  } else {
    await create(command)
  }
}

const saveSettings = async () => {
  errorMessage.value = ''
  saved.value = false

  if (rental.minimumNoticeHours < 0) {
    errorMessage.value = t("rentalSettings.errors.minimumNoticeNonNegative")
    return
  }

  if (rental.maximumAdvanceDays < 1) {
    errorMessage.value = t("rentalSettings.errors.maximumAdvanceMinOneDay")
    return
  }

  if (rental.bufferHours < 0) {
    errorMessage.value = t("rentalSettings.errors.bufferNonNegative")
    return
  }

  if (rental.operatingDays.length === 0) {
    errorMessage.value = t("rentalSettings.errors.operatingDaysRequired")
    return
  }

  if (rental.openingTime >= rental.closingTime) {
    errorMessage.value = t("rentalSettings.errors.closingAfterOpening")
    return
  }

  if (rental.lateReturnGraceHours < 0) {
    errorMessage.value = t("rentalSettings.errors.gracePeriodNonNegative")
    return
  }

  saving.value = true

  try {
    await saveSetting('RENTAL', {
      minimumNoticeHours: rental.minimumNoticeHours,
      maximumAdvanceDays: rental.maximumAdvanceDays,
      bufferHours: rental.bufferHours,
      operatingDays: rental.operatingDays,
      openingTime: rental.openingTime,
      closingTime: rental.closingTime,
      lateReturnEnabled: rental.lateReturnEnabled,
      lateReturnGraceHours: rental.lateReturnGraceHours,
    })

    saved.value = true
  } catch (error) {
    errorMessage.value = t(apiErrorMessage(error, "rentalSettings.errors.saveFailed"))
  } finally {
    saving.value = false
  }
}

watch(
  rental,
  () => {
    saved.value = false
  },
  { deep: true },
)

onMounted(loadSettings)
</script>

<template>
  <v-container fluid class="pa-0">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <h1 class="text-title-large font-weight-bold">
          {{ $t("menu.rentalSettings") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ $t("ui.atur_kebijakan_dan_jadwal_penyewaan_tokomu") }}
        </p>
      </div>

      <v-btn
        color="primary"
        prepend-icon="mdi-content-save"
        :loading="saving"
        :disabled="loading"
        @click="saveSettings"
      >
        {{ $t("rentalplan.save") }}
      </v-btn>
    </div>

    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

    <v-alert
      v-if="saved"
      type="success"
      variant="tonal"
      class="mb-4"
    >
      {{ $t("ui.pengaturan_rental_berhasil_disimpan") }}
    </v-alert>

    <v-progress-linear
      v-if="loading"
      indeterminate
      color="primary"
      class="mb-4"
    />

    <v-row>
      <v-col cols="12" md="8">
        <v-card rounded="lg" border flat class="mb-4">
          <v-card-title class="text-title-medium font-weight-bold pa-5">
            {{ $t("ui.kebijakan_pemesanan") }}
          </v-card-title>

          <v-divider />

          <v-card-text class="pa-5">
            <v-row>
              <v-col cols="12" sm="6">
                <v-label class="mb-2">
                  {{ $t("ui.minimal_waktu_sebelum_mulai_sewa") }}
                </v-label>

                <v-text-field
                  v-model.number="rental.minimumNoticeHours"
                  type="number"
                  min="0"
                  suffix="jam"
                  variant="outlined"
                  
                  hide-details
                />

                <div class="text-body-small text-medium-emphasis mt-2">
                  {{ $t("ui.contoh_pemesanan_harus_dilakukan_minimal_2_jam_sebelum_mulai_sew") }}
                </div>
              </v-col>

              <v-col cols="12" sm="6">
                <v-label class="mb-2">
                  {{ $t("ui.maksimal_pemesanan_di_muka") }}
                </v-label>

                <v-text-field
                  v-model.number="rental.maximumAdvanceDays"
                  type="number"
                  min="1"
                  suffix="hari"
                  variant="outlined"
                  
                  hide-details
                />

                <div class="text-body-small text-medium-emphasis mt-2">
                  {{ $t("ui.seberapa_jauh_pelanggan_bisa_memesan_tanggal_rental") }}
                </div>
              </v-col>

              <v-col cols="12">
                <v-label class="mb-2">
                  {{ $t("ui.jeda_antarpenyewaan") }}
                </v-label>

                <v-text-field
                  v-model.number="rental.bufferHours"
                  type="number"
                  min="0"
                  suffix="jam"
                  variant="outlined"
                  
                  hide-details
                />

                <div class="text-body-small text-medium-emphasis mt-2">
                  {{ $t("ui.waktu_untuk_inspeksi_pembersihan_atau_persiapan_barang_sebelum_d") }}
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card rounded="lg" border flat class="mb-4">
          <v-card-title class="text-title-medium font-weight-bold pa-5">
            {{ $t("ui.jadwal_operasional") }}
          </v-card-title>

          <v-divider />

          <v-card-text class="pa-5">
            <div class="text-body-medium mb-3">
              {{ $t("ui.hari_operasional") }}
            </div>

            <div class="d-flex flex-wrap ga-2 mb-6">
              <v-checkbox
                v-for="day in weekdays"
                :key="day.value"
                v-model="rental.operatingDays"
                :label="day.title"
                :value="day.value"
                density="compact"
                hide-details
                color="primary"
              />
            </div>

            <v-row>
              <v-col cols="12" sm="6">
                <v-label class="mb-2">
                  {{ $t("ui.jam_buka") }}
                </v-label>

                <v-text-field
                  v-model="rental.openingTime"
                  type="time"
                  variant="outlined"
                  
                  hide-details
                />
              </v-col>

              <v-col cols="12" sm="6">
                <v-label class="mb-2">
                  {{ $t("ui.jam_tutup") }}
                </v-label>

                <v-text-field
                  v-model="rental.closingTime"
                  type="time"
                  variant="outlined"
                  
                  hide-details
                />
              </v-col>
            </v-row>

            <v-alert
              type="info"
              variant="tonal"
              
              class="mt-4"
            >
              {{ $t("ui.jadwal_ini_menjadi_acuan_operasional_merchant_aturan_waktu_penga") }}
            </v-alert>
          </v-card-text>
        </v-card>

        <v-card rounded="lg" border flat>
          <v-card-title class="text-title-medium font-weight-bold pa-5">
            {{ $t("ui.keterlambatan_pengembalian") }}
          </v-card-title>

          <v-divider />

          <v-card-text class="pa-5">
            <v-switch
              v-model="rental.lateReturnEnabled"
              :label="$t('ui.terapkan_biaya_keterlambatan')"
              color="primary"
              hide-details
              inset
            />

            <template v-if="rental.lateReturnEnabled">
              <v-divider class="my-4" />

              <v-row>
                <v-col cols="12" sm="6">
                  <v-label class="mb-2">
                    {{ $t("orders.detail.gracePeriod") }}
                  </v-label>

                  <v-text-field
                    v-model.number="rental.lateReturnGraceHours"
                    type="number"
                    min="0"
                    suffix="jam"
                    variant="outlined"
                    
                    hide-details
                  />

                  <div class="text-body-small text-medium-emphasis mt-2">
                    {{ $t("ui.keterlambatan_di_bawah_batas_ini_tidak_dikenakan_biaya") }}
                  </div>
                </v-col>

                <v-col cols="12">
                  <v-alert type="info" variant="tonal" density="compact">
                    {{ $t("ui.denda_dihitung_dari_harga_sewa_harian_pada_order_untuk_setiap_ha") }}
                  </v-alert>
                </v-col>
              </v-row>
            </template>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card rounded="lg" border flat class="mb-4">
          <v-card-title class="text-title-medium font-weight-bold pa-5">
            {{ $t("ui.ringkasan_kebijakan") }}
          </v-card-title>

          <v-divider />

          <v-list class="py-0">
            <v-list-item
              :title="$t('ui.minimal_waktu_pemesanan')"
              :subtitle="`${rental.minimumNoticeHours} jam sebelumnya`"
            >
              <template #prepend>
                <v-icon icon="mdi-clock-outline" />
              </template>
            </v-list-item>

            <v-divider />

            <v-list-item
              :title="$t('ui.pemesanan_di_muka')"
              :subtitle="`Maksimal ${rental.maximumAdvanceDays} hari`"
            >
              <template #prepend>
                <v-icon icon="mdi-calendar-range" />
              </template>
            </v-list-item>

            <v-divider />

            <v-list-item
              :title="$t('ui.jeda_antarpenyewaan')"
              :subtitle="`${rental.bufferHours} jam`"
            >
              <template #prepend>
                <v-icon icon="mdi-timer-sand" />
              </template>
            </v-list-item>

            <v-divider />

            <v-list-item
              :title="$t('ui.jam_operasional')"
              :subtitle="`${rental.openingTime} – ${rental.closingTime}`"
            >
              <template #prepend>
                <v-icon icon="mdi-store-clock-outline" />
              </template>
            </v-list-item>

            <v-divider />

            <v-list-item
              :title="$t('ui.hari_operasional')"
              :subtitle="`${rental.operatingDays.length} hari per minggu`"
            >
              <template #prepend>
                <v-icon icon="mdi-calendar-check-outline" />
              </template>
            </v-list-item>
          </v-list>
        </v-card>

        <v-card rounded="lg" border flat>
          <v-card-text class="pa-5">
            <div class="d-flex align-center ga-2 mb-3">
              <v-icon icon="mdi-information-outline" color="primary" />
              <span class="text-subtitle-2 font-weight-bold">
                {{ $t("checkout.notesTitle") }}
              </span>
            </div>

            <p class="text-body-medium text-medium-emphasis mb-3">
              {{ $t("ui.pengaturan_ini_berlaku_untuk_merchant_secara_keseluruhan") }}
            </p>

            <p class="text-body-medium text-medium-emphasis mb-3">
              {{ $t("ui.rental_plan_pada_produk_tetap_menentukan_durasi_dan_harga_sewa") }}
            </p>

            <p class="text-body-medium text-medium-emphasis">
              {{ $t("ui.ketersediaan_unit_tetap_ditentukan_oleh_inventory_dan_periode_re") }}
            </p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
