<script setup lang="ts">
definePageMeta({ layout: 'merchant', middleware: ['merchant'] });
useHead({ title: 'Import Produk' });
const file = ref<File | null>(null);
const { status, connection, error, busy, uploading, upload, snapshot } = useProductImport();
const labels: Record<string, string> = { QUEUED: 'Menunggu antrean', RUNNING: 'Mengimport produk', COMPLETED: 'Import selesai', FAILED: 'Import terhenti' };
const progress = computed(() => status.value?.total ? status.value.processed / status.value.total * 100 : 0);
const fileError = ref('');
function select(event: Event) {
  const selected = (event.target as HTMLInputElement).files?.[0] ?? null;
  fileError.value = '';
  if (selected && (!selected.name.toLowerCase().endsWith('.csv') || selected.size > 5 * 1024 * 1024)) {
    fileError.value = 'Pilih file CSV maksimal 5 MB.'; file.value = null; return;
  }
  file.value = selected;
}
</script>
<template>
  <v-container fluid class="pa-4">
    <v-btn variant="text" prepend-icon="mdi-arrow-left" to="/merchant/products" class="mb-4">Produk</v-btn>
    <h1 class="text-headline-small mb-2">Import produk dari CSV</h1>
    <p class="text-body-medium text-medium-emphasis mb-6">Upload katalog produk beserta URL gambar. Proses berjalan di background; halaman ini boleh direfresh.</p>
    <v-card variant="outlined" class="pa-4 mb-6">
      <h2 class="text-title-large mb-3">Siapkan file</h2>
      <p class="mb-3">Satu baris mewakili satu varian dengan harga sewa per hari. Baris dengan slug sama digabung menjadi satu produk, meskipun tidak berurutan. Maksimal 1000 baris varian per file. Slug yang sudah ada akan dilewati tanpa mengubah produk lama.</p>
      <v-btn href="/templates/products-import.csv" download variant="tonal" prepend-icon="mdi-download" class="mb-4">Download template CSV</v-btn>
      <p class="mb-3 text-body-medium">Kolom wajib: <code>name, slug, sku, rental_fee</code>. Isi variant_name untuk produk dengan beberapa varian; satu varian tanpa nama memakai Standar. Nama varian dan SKU harus unik dalam satu produk. Ulangi name, description, dan status yang sama untuk setiap varian produk. Stok default 0 (total maksimal 1000 per produk), status default DRAFT. Nominal tanpa pemisah ribuan, gunakan titik untuk desimal.</p>
      <p class="mb-4 text-body-medium">Isi <code>image_urls</code> dengan URL langsung gambar publik, pisahkan beberapa gambar memakai <code>|</code>. Maksimal 5 gambar per varian, masing-masing 5 MB. Gambar pertama yang tersedia menurut urutan baris menjadi gambar utama produk; setiap varian memakai gambarnya sendiri. Semua gambar tersimpan di Media. URL yang mengalihkan ke alamat lain perlu diganti dengan URL akhirnya.</p>
      <label for="product-import-file" class="d-block mb-2">File CSV</label>
      <input id="product-import-file" type="file" accept=".csv,text/csv" :disabled="busy" class="csv-input mb-4" @change="select" />
      <v-alert v-if="fileError || error" type="error" variant="tonal" class="mb-4">{{ fileError || error }}</v-alert>
      <v-btn color="primary" :disabled="!file || busy" :loading="uploading" @click="file && upload(file)">Import produk</v-btn>
    </v-card>
    <v-alert v-if="connection !== 'CONNECTED'" type="info" variant="tonal" class="mb-4">
      Sambungan progress: {{ connection }}. Import yang sudah dimulai tetap diproses di backend.
      <v-btn variant="text" @click="snapshot">Muat status</v-btn>
    </v-alert>
    <v-card v-if="status" variant="outlined" class="pa-4" aria-live="polite">
      <h2 class="text-title-large mb-3">{{ labels[status.status] }}</h2>
      <v-progress-linear :model-value="progress" :indeterminate="status.status === 'QUEUED'" color="primary" class="mb-3" />
      <p>{{ status.processed }} / {{ status.total }} produk diproses · {{ status.succeeded }} berhasil · {{ status.skipped }} dilewati · {{ status.failed }} gagal</p>
      <v-alert v-if="status.message" type="warning" variant="tonal" class="mt-4">{{ status.message }}</v-alert>
      <p v-if="status.failed" class="mt-4">Jika satu varian gagal, seluruh produk tersebut dibatalkan dan semua record variannya ditandai gagal. Perbaiki lalu upload ulang. Nomor di bawah menghitung header sebagai record 1; teks multiline tetap dihitung satu record. Ditampilkan maksimal 100 kegagalan pertama.</p>
      <v-table v-if="status.errors.length" class="mt-3">
        <thead><tr><th>Record CSV</th><th>Masalah</th></tr></thead>
        <tbody><tr v-for="item in status.errors" :key="item.row"><td>{{ item.row }}</td><td>{{ item.message }}</td></tr></tbody>
      </v-table>
    </v-card>
  </v-container>
</template>
<style scoped>
.csv-input { display: block; max-width: 100%; width: 100%; }
code { overflow-wrap: anywhere; }
</style>
