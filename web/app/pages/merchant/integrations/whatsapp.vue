<script setup lang="ts">
import QrcodeVue from 'qrcode.vue';
definePageMeta({ layout: 'merchant', middleware: ['merchant'] });
useHead({ title: 'Integrasi WhatsApp' });
const { status, disconnectFailed, messages, total, page, loading, busy, error, connection, refresh, connect, disconnect, send } = useWhatsAppIntegration();
const to = ref('');
const body = ref('');
const requestId = ref('');
const uncertain = ref(false);
const confirmDisconnect = ref(false);
const formError = ref('');
const states: Record<string, string> = {
  NOT_CONNECTED: 'Belum terhubung', STARTING: 'Menyiapkan WhatsApp', QR: 'Menunggu scan QR',
  AUTHENTICATING: 'Menghubungkan perangkat', CONNECTED: 'Terhubung', DISCONNECTED: 'Terputus',
  FAILED: 'Gagal menghubungkan', UNAVAILABLE: 'Gateway tidak tersedia',
};
const canConnect = computed(() => status.value?.configured && ['NOT_CONNECTED', 'DISCONNECTED', 'FAILED'].includes(status.value.state));
const canDisconnect = computed(() => disconnectFailed.value || (status.value?.enabled && ['STARTING', 'QR', 'AUTHENTICATING', 'CONNECTED', 'UNAVAILABLE'].includes(status.value.state)));
function resetDraft() { to.value = ''; body.value = ''; requestId.value = ''; uncertain.value = false; }
const merchant = useMerchantStore();
watch(() => merchant.current?.id, () => { resetDraft(); formError.value = ''; confirmDisconnect.value = false; });
async function sendText() {
  formError.value = '';
  if (!/^\+?[1-9][0-9]{7,14}$/.test(to.value) || !body.value.trim() || body.value.length > 4096) {
    formError.value = 'Isi nomor dengan kode negara (contoh 628123456789) dan pesan maksimal 4096 karakter.'; return;
  }
  requestId.value ||= crypto.randomUUID();
  const result = await send({ requestId: requestId.value, to: to.value, body: body.value });
  if (result?.status === 'SENT') resetDraft();
  else uncertain.value = true;
}
function localDate(value: string) { return new Date(value).toLocaleString(); }
</script>
<template>
  <v-container fluid class="pa-4 pa-md-6">
    <v-btn to="/merchant/integrations" variant="text" prepend-icon="mdi-arrow-left" class="mb-4">Integrasi</v-btn>
    <div class="d-flex flex-wrap align-center justify-space-between ga-3 mb-6">
      <div><h1 class="text-headline-small">WhatsApp</h1><p class="text-medium-emphasis mt-2">Hubungkan nomor WhatsApp toko untuk menerima dan mengirim pesan teks.</p></div>
      <v-btn variant="outlined" :loading="loading" @click="refresh">Muat status</v-btn>
    </div>
    <v-alert v-if="error || formError" type="error" variant="tonal" class="mb-4">{{ error || formError }}</v-alert>
    <v-alert v-if="status && !status.configured" type="info" variant="tonal" class="mb-4">Gateway WhatsApp belum dikonfigurasi oleh pengelola aplikasi.</v-alert>
    <v-alert v-if="connection !== 'CONNECTED'" type="info" variant="tonal" class="mb-4">Sambungan pembaruan langsung belum tersedia. Gunakan Muat status; sesi WhatsApp tetap berjalan di gateway.</v-alert>
    <v-row>
      <v-col cols="12" md="5">
        <v-card variant="outlined" class="pa-4">
          <h2 class="text-title-large mb-3">Nomor toko</h2>
          <v-chip :color="status?.state === 'CONNECTED' ? 'success' : undefined" class="mb-3">{{ states[status?.state ?? 'STARTING'] ?? status?.state }}</v-chip>
          <p v-if="status?.phone" class="mb-3">+{{ status.phone }}</p>
          <v-progress-linear v-if="['STARTING', 'AUTHENTICATING'].includes(status?.state ?? '')" indeterminate class="mb-4" />
          <div v-if="status?.qr && status.state === 'QR'" class="qr-panel my-4">
            <QrcodeVue :value="status.qr" :size="240" level="M" render-as="svg" />
          </div>
          <p v-if="status?.state === 'QR'" class="mb-4">Buka WhatsApp di HP → Perangkat tertaut → Tautkan perangkat, lalu scan QR ini.</p>
          <p class="text-body-small text-medium-emphasis mb-4">Koneksi memakai WhatsApp Web melalui gateway aplikasi. Satu nomor/sesi terpisah untuk setiap toko.</p>
          <div class="d-flex flex-wrap ga-2">
            <v-btn v-if="canConnect" color="primary" :loading="busy" @click="connect">Hubungkan WhatsApp</v-btn>
            <v-btn v-if="canDisconnect" variant="outlined" color="error" :disabled="busy" @click="confirmDisconnect = true">Putuskan koneksi</v-btn>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" md="7">
        <v-card variant="outlined" class="pa-4">
          <h2 class="text-title-large mb-4">Kirim pesan</h2>
          <v-text-field v-model="to" label="Nomor tujuan dengan kode negara" placeholder="628123456789" :disabled="busy || uncertain" inputmode="tel" maxlength="16" />
          <v-textarea v-model="body" label="Pesan" :disabled="busy || uncertain" maxlength="4096" counter rows="4" />
          <v-alert v-if="uncertain" type="warning" variant="tonal" class="mb-4">Status pengiriman belum pasti. Periksa chat WhatsApp sebelum membuat pesan baru agar tidak terkirim dua kali.</v-alert>
          <div class="d-flex flex-wrap ga-2">
            <v-btn color="primary" :loading="busy" :disabled="status?.state !== 'CONNECTED' || uncertain || !to || !body.trim()" @click="sendText">Kirim</v-btn>
            <v-btn v-if="uncertain" variant="outlined" @click="resetDraft">Buat pesan baru</v-btn>
          </div>
        </v-card>
      </v-col>
    </v-row>
    <v-card variant="outlined" class="pa-4 mt-6">
      <h2 class="text-title-large mb-4">Pesan masuk dan keluar</h2>
      <p class="text-body-small text-medium-emphasis mb-4">Pesan teks langsung yang diterima sejak integrasi aktif. Grup, media, dan riwayat lama belum disinkronkan. SENT berarti diterima gateway, bukan konfirmasi dibaca penerima.</p>
      <p v-if="!messages.length">Belum ada pesan.</p>
      <v-list v-else lines="three" class="pa-0">
        <v-list-item v-for="message in messages" :key="message.id" class="message-row px-0 py-3">
          <div class="d-flex flex-wrap align-center ga-2 mb-2"><strong>{{ message.peer }}</strong><v-chip size="x-small">{{ message.direction === 'INBOUND' ? 'Masuk' : 'Keluar' }} · {{ message.status }}</v-chip></div>
          <p class="message-body">{{ message.body }}</p>
          <p class="text-body-small text-medium-emphasis mt-2">{{ localDate(message.occurredAt) }}</p>
        </v-list-item>
      </v-list>
      <div class="d-flex flex-wrap align-center justify-end ga-2 mt-4">
        <v-btn variant="text" :disabled="page === 0 || loading" @click="page--">Sebelumnya</v-btn>
        <span>Halaman {{ page + 1 }}</span>
        <v-btn variant="text" :disabled="(page + 1) * 30 >= total || loading" @click="page++">Berikutnya</v-btn>
      </div>
    </v-card>
    <v-dialog v-model="confirmDisconnect" max-width="420">
      <v-card title="Putuskan WhatsApp?" text="Perangkat tertaut di gateway akan dilepas. Riwayat pesan integrasi tetap tersimpan.">
        <v-card-actions><v-btn @click="confirmDisconnect = false">Batal</v-btn><v-btn color="error" @click="confirmDisconnect = false; disconnect()">Putuskan</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
<style scoped>
.qr-panel { background: white; padding: 16px; width: fit-content; max-width: 100%; }
.qr-panel :deep(svg) { display: block; max-width: 100%; height: auto; }
.message-row { border-bottom: 1px solid rgba(var(--v-theme-on-surface), .12); }
.message-body { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
