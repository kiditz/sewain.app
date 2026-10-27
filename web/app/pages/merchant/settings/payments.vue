
<script setup lang="ts">
import type { Setting, SettingCommand } from '~/types/settings'

definePageMeta({
  layout: 'merchant',
  middleware: ['auth', 'merchant'],
})

interface PaymentSettings {
  bankTransfer: boolean
  cashAtPickup: boolean
  cashAtReturn: boolean
  instructions: string
  downPaymentEnabled: boolean
  downPaymentType: 'percentage' | 'fixed'
  downPaymentValue: number
  fullPaymentEnabled: boolean
}

interface PaymentBankAccount {
  id: string
  bankName: string
  accountNumber: string
  accountHolder: string
}

const {
  findByKey,
  create,
  update,
} = useSettings()

const loading = ref(true)
const saving = ref(false)
const saved = ref(false)
const saveError = ref('')

const payment = reactive<PaymentSettings>({
  bankTransfer: true,
  cashAtPickup: false,
  cashAtReturn: false,
  instructions: '',
  downPaymentEnabled: true,
  downPaymentType: 'percentage',
  downPaymentValue: 30,
  fullPaymentEnabled: true,
})

const bankAccounts = ref<PaymentBankAccount[]>([])

function isNotFound(error: unknown): boolean {
  if (!error || typeof error !== 'object') return false

  const err = error as {
    status?: number
    statusCode?: number
    response?: { status?: number; statusCode?: number }
    data?: { statusCode?: number }
  }

  return [
    err.status,
    err.statusCode,
    err.response?.status,
    err.response?.statusCode,
    err.data?.statusCode,
  ].includes(404)
}

async function getSetting(key: string): Promise<Setting | null> {
  try {
    return await findByKey(key)
  } catch (error) {
    if (isNotFound(error)) return null
    throw error
  }
}

async function loadSettings() {
  loading.value = true
  saveError.value = ''

  try {
    const [paymentSetting, bankSetting] = await Promise.all([
      getSetting('PAYMENT'),
      getSetting('PAYMENT_BANK_ACCOUNTS'),
    ])

    if (
      paymentSetting?.value &&
      typeof paymentSetting.value === 'object'
    ) {
      const value = paymentSetting.value as Partial<PaymentSettings>

      payment.bankTransfer = value.bankTransfer ?? true
      payment.cashAtPickup = value.cashAtPickup ?? false
      payment.cashAtReturn = value.cashAtReturn ?? false
      payment.instructions = value.instructions ?? ''
      payment.downPaymentEnabled = value.downPaymentEnabled ?? true
      payment.downPaymentType = value.downPaymentType ?? 'percentage'
      payment.downPaymentValue = value.downPaymentValue ?? 30
      payment.fullPaymentEnabled = value.fullPaymentEnabled ?? true
    }

    if (bankSetting?.value && typeof bankSetting.value === 'object') {
      const value = bankSetting.value as {
        accounts?: PaymentBankAccount[]
      }

      bankAccounts.value = Array.isArray(value.accounts)
        ? value.accounts.map(account => ({ ...account }))
        : []
    }
  } catch {
    saveError.value = 'Gagal memuat pengaturan pembayaran.'
  } finally {
    loading.value = false
  }
}

function addBankAccount() {
  bankAccounts.value.push({
    id: crypto.randomUUID(),
    bankName: '',
    accountNumber: '',
    accountHolder: '',
  })

  saved.value = false
}

function removeBankAccount(id: string) {
  bankAccounts.value = bankAccounts.value.filter(
    account => account.id !== id,
  )

  saved.value = false
}

async function saveSetting(
  key: string,
  value: Record<string, unknown>,
) {
  const command: SettingCommand = { key, value }
  const existing = await getSetting(key)

  if (existing) {
    await update(existing.id, command)
  } else {
    await create(command)
  }
}

async function saveSettings() {
  saved.value = false
  saveError.value = ''

  if (!payment.downPaymentEnabled && !payment.fullPaymentEnabled) {
    saveError.value = 'Aktifkan minimal satu skema pembayaran.'
    return
  }

  if (
    payment.downPaymentEnabled &&
    (
      !Number.isFinite(payment.downPaymentValue) ||
      payment.downPaymentValue <= 0 ||
      (
        payment.downPaymentType === 'percentage' &&
        payment.downPaymentValue > 100
      )
    )
  ) {
    saveError.value =
      'Nilai DP harus lebih dari 0 dan persentase tidak boleh melebihi 100%.'
    return
  }

  if (
    payment.bankTransfer &&
    bankAccounts.value.some(
      account =>
        !account.bankName.trim() ||
        !account.accountNumber.trim() ||
        !account.accountHolder.trim(),
    )
  ) {
    saveError.value = 'Lengkapi semua informasi rekening bank.'
    return
  }

  saving.value = true

  try {
    await Promise.all([
      saveSetting('PAYMENT', {
        bankTransfer: payment.bankTransfer,
        cashAtPickup: payment.cashAtPickup,
        cashAtReturn: payment.cashAtReturn,
        instructions: payment.instructions,
        downPaymentEnabled: payment.downPaymentEnabled,
        downPaymentType: payment.downPaymentType,
        downPaymentValue: payment.downPaymentValue,
        fullPaymentEnabled: payment.fullPaymentEnabled,
        separateDeposit: true,
      }),
      saveSetting('PAYMENT_BANK_ACCOUNTS', {
        accounts: bankAccounts.value,
      }),
    ])

    saved.value = true
  } catch {
    saveError.value = 'Gagal menyimpan pengaturan pembayaran.'
  } finally {
    saving.value = false
  }
}

watch(
  [payment, bankAccounts],
  () => {
    saved.value = false
  },
  { deep: true },
)

onMounted(loadSettings)
</script>

<template>
  <v-container fluid class="pa-0">
    <div class="d-flex justify-space-between align-start flex-wrap ga-4 mb-6">
      <div>
        <h1 class="text-title-large font-weight-bold">
          {{ $t("ui.payment_settings") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ $t("ui.kelola_metode_pembayaran_skema_pembayaran_dan_rekening_merchant") }}
        </p>
      </div>

      <v-btn
        color="primary"
        prepend-icon="mdi-content-save-outline"
        :loading="saving"
        :disabled="loading || saving"
        @click="saveSettings"
      >
        {{ $t("common.saveChange") }}
      </v-btn>
    </div>

    <v-alert
      v-if="saveError"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="saveError = ''"
    >
      {{ saveError }}
    </v-alert>

    <v-alert
      v-if="saved"
      type="success"
      variant="tonal"
      class="mb-4"
    >
      {{ $t("ui.pengaturan_pembayaran_berhasil_disimpan") }}
    </v-alert>

    <div v-if="loading" class="d-flex justify-center pa-12">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <v-row v-else>
      <v-col cols="12" lg="8">
        <v-card variant="outlined" class="mb-5">
          <v-card-item>
            <v-card-title>{{ $t("checkout.paymentMethod") }}</v-card-title>
            <v-card-subtitle>
              {{ $t("ui.aktifkan_metode_pembayaran_yang_tersedia_untuk_customer") }}
            </v-card-subtitle>
          </v-card-item>

          <v-divider />

          <v-list>
            <v-list-item
              prepend-icon="mdi-bank-transfer"
              :title="$t('checkout.bankTransfer')"
              subtitle="Customer membayar melalui transfer ke rekening merchant."
            >
              <template #append>
                <v-switch
                  v-model="payment.bankTransfer"
                  color="primary"
                  inset
                  hide-details
                  :aria-label="$t('ui.aktifkan_transfer_bank')"
                />
              </template>
            </v-list-item>

            <v-divider />

            <v-list-item
              prepend-icon="mdi-cash"
              :title="$t('ui.cash_saat_pengambilan')"
              subtitle="Customer membayar ketika mengambil barang."
            >
              <template #append>
                <v-switch
                  v-model="payment.cashAtPickup"
                  color="primary"
                  inset
                  hide-details
                  :aria-label="$t('ui.aktifkan_cash_saat_pengambilan')"
                />
              </template>
            </v-list-item>

            <v-divider />

            <v-list-item
              prepend-icon="mdi-cash-check"
              :title="$t('ui.cash_saat_pengembalian')"
              subtitle="Customer membayar ketika mengembalikan barang."
            >
              <template #append>
                <v-switch
                  v-model="payment.cashAtReturn"
                  color="primary"
                  inset
                  hide-details
                  :aria-label="$t('ui.aktifkan_cash_saat_pengembalian')"
                />
              </template>
            </v-list-item>
          </v-list>
        </v-card>

        <v-card variant="outlined" class="mb-5">
          <v-card-item>
            <v-card-title>{{ $t("ui.payment_terms") }}</v-card-title>
            <v-card-subtitle>
              {{ $t("ui.tentukan_skema_pembayaran_yang_tersedia_untuk_customer") }}
            </v-card-subtitle>
          </v-card-item>

          <v-divider />

          <v-card-text>
            <v-switch
              v-model="payment.downPaymentEnabled"
              :label="$t('ui.down_payment_dp')"
              color="primary"
              inset
              hide-details
            />

            <div v-if="payment.downPaymentEnabled" class="mt-4 mb-6">
              <v-radio-group
                v-model="payment.downPaymentType"
                :label="$t('ui.perhitungan_dp')"
                inline
                hide-details
              >
                <v-radio :label="$t('ui.persentase')" value="percentage" />
                <v-radio :label="$t('ui.nominal_tetap')" value="fixed" />
              </v-radio-group>

              <v-text-field
                v-model.number="payment.downPaymentValue"
                :label="payment.downPaymentType === 'percentage'
                  ? 'Persentase DP'
                  : 'Nominal DP'"
                :prefix="payment.downPaymentType === 'fixed' ? 'Rp' : undefined"
                :suffix="payment.downPaymentType === 'percentage' ? '%' : undefined"
                type="number"
                min="0"
                :max="payment.downPaymentType === 'percentage' ? 100 : undefined"
                variant="outlined"
                
                class="mt-4"
                hide-details="auto"
              />
            </div>

            <v-divider class="my-4" />

            <v-switch
              v-model="payment.fullPaymentEnabled"
              :label="$t('ui.full_payment')"
              color="primary"
              inset
              hide-details
            />

            <v-alert
              v-if="!payment.downPaymentEnabled && !payment.fullPaymentEnabled"
              type="error"
              variant="tonal"
              class="mt-4"
            >
              {{ $t("ui.aktifkan_minimal_satu_skema_pembayaran") }}
            </v-alert>

            <v-alert v-else type="info" variant="tonal" class="mt-4">
              {{ $t("ui.dp_dihitung_dari_subtotal_biaya_sewa_tidak_termasuk_deposit_depo") }}
            </v-alert>
          </v-card-text>
        </v-card>

        <v-card
          v-if="payment.bankTransfer"
          variant="outlined"
          class="mb-5"
        >
          <v-card-item>
            <v-card-title>{{ $t("ui.rekening_bank") }}</v-card-title>
            <v-card-subtitle>
              {{ $t("ui.informasi_rekening_yang_ditampilkan_kepada_customer") }}
            </v-card-subtitle>

            <template #append>
              <v-btn
                color="primary"
                variant="tonal"
                prepend-icon="mdi-plus"
                @click="addBankAccount"
              >
                {{ $t("ui.tambah_rekening") }}
              </v-btn>
            </template>
          </v-card-item>

          <v-divider />

          <v-card-text>
            <div v-if="bankAccounts.length === 0" class="text-center pa-6">
              <v-icon
                icon="mdi-bank-plus"
                size="40"
                color="medium-emphasis"
                class="mb-3"
              />
              <p class="text-body-1 font-weight-medium">
                {{ $t("ui.belum_ada_rekening_bank") }}
              </p>
              <p class="text-body-medium text-medium-emphasis mt-1">
                {{ $t("ui.tambahkan_rekening_untuk_menerima_pembayaran") }}
              </p>
            </div>

            <v-card
              v-for="(account, index) in bankAccounts"
              :key="account.id"
              variant="flat"
              class="pa-4 mb-4"
            >
              <div class="d-flex justify-space-between align-center mb-4">
                <span class="text-title-medium font-weight-medium">
                  {{ $t("ui.rekening") }} {{ index + 1 }}
                </span>

                <v-btn
                  icon="mdi-delete-outline"
                  color="error"
                  variant="text"
                  :aria-label="$t('ui.hapus_rekening')"
                  @click="removeBankAccount(account.id)"
                />
              </div>

              <v-row>
                <v-col cols="12" sm="6">
                  <v-text-field
                    v-model="account.bankName"
                    :label="$t('ui.nama_bank')"
                    :placeholder="$t('ui.contoh_bca')"
                    variant="outlined"
                    
                    hide-details="auto"
                  />
                </v-col>

                <v-col cols="12" sm="6">
                  <v-text-field
                    v-model="account.accountNumber"
                    :label="$t('ui.nomor_rekening')"
                    :placeholder="$t('ui.masukkan_nomor_rekening')"
                    variant="outlined"
                    
                    hide-details="auto"
                  />
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="account.accountHolder"
                    :label="$t('ui.nama_pemilik_rekening')"
                    :placeholder="$t('ui.sesuai_nama_di_rekening_bank')"
                    variant="outlined"
                    
                    hide-details="auto"
                  />
                </v-col>
              </v-row>
            </v-card>
          </v-card-text>
        </v-card>

        <v-card variant="outlined">
          <v-card-item>
            <v-card-title>{{ $t("ui.instruksi_pembayaran") }}</v-card-title>
            <v-card-subtitle>
              {{ $t("ui.informasi_tambahan_untuk_customer_saat_melakukan_pembayaran") }}
            </v-card-subtitle>
          </v-card-item>

          <v-divider />

          <v-card-text>
            <v-textarea
              v-model="payment.instructions"
              :label="$t('ui.instruksi')"
              :placeholder="$t('ui.contoh_transfer_sesuai_total_tagihan_dan_cantumkan_nomor_pesanan')"
              variant="outlined"
              rows="4"
              auto-grow
              hide-details="auto"
            />
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card variant="outlined" class="mb-5">
          <v-card-item>
            <v-card-title>{{ $t("ui.ringkasan_metode") }}</v-card-title>
          </v-card-item>

          <v-divider />

          <v-list >
            <v-list-item
              prepend-icon="mdi-bank-transfer"
              :title="$t('checkout.bankTransfer')"
              :subtitle="payment.bankTransfer ? 'Aktif' : 'Nonaktif'"
            >
              <template #append>
                <v-icon
                  :icon="payment.bankTransfer ? 'mdi-check-circle' : 'mdi-minus-circle'"
                  :color="payment.bankTransfer ? 'success' : 'medium-emphasis'"
                />
              </template>
            </v-list-item>

            <v-list-item
              prepend-icon="mdi-cash"
              :title="$t('ui.cash_saat_pengambilan')"
              :subtitle="payment.cashAtPickup ? 'Aktif' : 'Nonaktif'"
            />

            <v-list-item
              prepend-icon="mdi-cash-check"
              :title="$t('ui.cash_saat_pengembalian')"
              :subtitle="payment.cashAtReturn ? 'Aktif' : 'Nonaktif'"
            />
          </v-list>
        </v-card>

        <v-card variant="outlined" class="mb-5">
          <v-card-item>
            <v-card-title>{{ $t("ui.skema_pembayaran") }}</v-card-title>
          </v-card-item>

          <v-divider />

          <v-list >
            <v-list-item
              prepend-icon="mdi-pie-chart"
              :title="$t('ui.down_payment')"
              :subtitle="payment.downPaymentEnabled
                ? payment.downPaymentType === 'percentage'
                  ? `${payment.downPaymentValue}% dari biaya sewa`
                  : `Rp ${payment.downPaymentValue.toLocaleString('id-ID')}`
                : 'Nonaktif'"
            />

            <v-list-item
              prepend-icon="mdi-cash-multiple"
              :title="$t('ui.full_payment')"
              :subtitle="payment.fullPaymentEnabled ? 'Aktif' : 'Nonaktif'"
            />
          </v-list>
        </v-card>

        <v-card variant="tonal">
          <v-card-item>
            <template #prepend>
              <v-icon
                icon="mdi-shield-check-outline"
                color="primary"
                size="28"
              />
            </template>

            <v-card-title>{{ $t("ui.deposit_sewa") }}</v-card-title>
          </v-card-item>

          <v-card-text class="text-body-medium">
            {{ $t("ui.biaya_sewa_dan_deposit_dicatat_sebagai_dua_komponen_terpisah_dep") }}
          </v-card-text>

          <v-card-text class="pt-0">
            <v-chip color="success" variant="tonal">
              <v-icon start icon="mdi-check-circle" />
              {{ $t("ui.pencatatan_terpisah") }}
            </v-chip>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <div v-if="!loading" class="d-flex justify-end mt-6">
      <v-btn
        color="primary"
        prepend-icon="mdi-content-save-outline"
        :loading="saving"
        :disabled="saving"
        @click="saveSettings"
      >
        {{ $t("common.saveChange") }}
      </v-btn>
    </div>
  </v-container>
</template>
