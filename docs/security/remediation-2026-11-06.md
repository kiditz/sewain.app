# Remediasi security review — 6 November 2026

Ketujuh temuan utama pada [review awal](/Users/rifkyaditya/Playground/sewain/docs/security/review-2026-11-06.md) sudah diberi patch dan regresi terfokus. Tidak ada migrasi database atau perubahan data toko yang dijalankan oleh pekerjaan ini. Ini bukan klaim bahwa seluruh risiko aplikasi/infrastruktur sudah hilang.

| Temuan | Perubahan | Bukti regresi |
|---|---|---|
| SEC-01 | Update merchant memeriksa owner target sebelum mapping; tidak lagi mengganti `userId` | MerchantOwnershipTest |
| SEC-02 | Controller migrasi HTTP dihapus dan URL lama ditolak security chain; migrasi hanya lewat runner maintenance opt-in | ApiSecurityBoundaryTest |
| SEC-03 | Semua operasi cart menolak customer ID yang hanya dinyatakan caller; GET juga memvalidasi owner; guest token tetap didukung | CartOwnershipTest |
| SEC-04 | Query/DTO katalog publik terpisah; status ACTIVE dipaksakan; biaya beli, serial, barcode, unit fisik dan catatan inventaris tidak diserialisasi | PublicCatalogSecurityTest + ApiSecurityBoundaryTest |
| SEC-05 | HTML rich text dibersihkan dengan parser pada write/import, pembacaan data lama, dan render Vue SSR/browser | ContentSecurityTest + ThemeLibraryServiceTest + storefront-content-security.test.mjs |
| SEC-06 | Upload didekode dan di-encode ulang sebagai raster, nama/ekstensi/MIME ditentukan server, dimensi dibatasi, file aktif lama diblokir | SafeRasterUploadTest + MediaFileReplacementTest |
| SEC-07 | URL navigasi block memakai resolver protokol bersama, termasuk Button/Logo/Policy dan setting impor/legacy | ContentSecurityTest + storefront-content-security.test.mjs |

## Perubahan kontrak API

Endpoint merchant `/api/products/**`, `/api/collections/**`, dan `/api/merchants` memerlukan autentikasi. Storefront memakai GET:

- `/api/storefront/products`, `/api/storefront/products/{id}`, `/api/storefront/products/slug/{slug}`;
- `/api/storefront/products/types`;
- `/api/storefront/collections`, `/api/storefront/collections/{slug}`, `/api/storefront/collections/{id}/products`.

Status DRAFT dan UNLISTED tidak dibuka lewat katalog publik, termasuk detail by ID/slug. Public variant menyediakan `inventoryQuantity` sebagai angka agregat; daftar `inventoryUnits` dan `buyPrice` tidak dikirim. Adapter frontend mempertahankan tipe UI bersama tanpa mengambil kembali data internal.

Cart publik tetap memakai `X-Guest-Token`. `X-Customer-Id` ditolak sampai identitas customer benar-benar berasal dari sesi/token terverifikasi. Menambahkan customer login di masa depan harus melalui owner resolver server, bukan membuka kembali header caller.

**Deploy backend dan frontend sebagai pasangan**, karena bundle storefront lama masih memanggil URL katalog lama. Reload storefront setelah deploy.

## Operasi maintenance

`POST /api/merchants/migrate` tidak boleh dipakai lagi, termasuk oleh owner merchant.

Runner migrasi hanya terdaftar ketika properti `app.maintenance.migrate-tenants=true` diberikan lewat konfigurasi deployment terpercaya, misalnya environment `APP_MAINTENANCE_MIGRATE_TENANTS=true`. Jalankan sebagai instance maintenance yang tidak menerima traffic, dengan database role yang sesuai, lalu matikan flag. Jangan mengaktifkannya pada startup aplikasi rutin. Flag ini tidak diaktifkan selama pengujian.

## Format upload dan kompatibilitas

- Format input raster: PNG, JPEG, GIF, dan WebP yang berhasil didekode.
- Output upload: JPEG untuk input JPEG, PNG untuk format lain; metadata dihapus dan GIF animasi menjadi frame pertama.
- Batas: input 5 MB/file, maksimum 8192 per sisi dan 16 juta piksel, output 20 MB/file, total output satu batch 20 MB. Batas multipart request 20 MB tetap berlaku.
- SVG/HTML aktif tidak diterima. Upload lama dengan ekstensi aktif tidak disajikan publik; gambar SVG lama perlu diganti raster. SVG statis bawaan aplikasi di `web/public` tidak terpengaruh.
- Hapus-background tetap mempertahankan ID dan URL file lama. Resolver mengenali hasil PNG meskipun ekstensi URL lama JPEG/SVG/HTML, dan menyajikannya sebagai `image/png`, bukan sebagai dokumen aktif.
- Pembacaan data HTML lama disanitasi tanpa harus menjalankan rewrite massal database. Field plain text seperti judul halaman tidak diubah menjadi HTML entity.

## Validasi

- Regresi backend terfokus mencakup filter security asli melalui MockMvc, owner checks, public projection, sanitizer, import/legacy theme, validasi raster dan replacement/rollback media.
- Tiga tes frontend memeriksa payload HTML/URL, format Quill yang sah, dan SSR komponen BlockLabel yang sebenarnya.
- Kompilasi Vue dan TypeScript pada file terdampak lolos.
- Build production client, SSR, dan Nitro lolos dengan Node 24.22.1, termasuk setelah update Nuxt 4.6.4.
- Tidak ada browser, pentest terhadap toko nyata, proses migrasi tenant, atau pembuatan order yang dijalankan.

## Dependensi: penurunan risiko dan sisa pekerjaan

Pemasangan sanitizer diikuti audit registry. Pembaruan kompatibel dilakukan tanpa `--force` dan tanpa menjalankan script instalasi. Nuxt dinaikkan ke baseline `^4.6.4`; lockfile ikut diperbarui. Versi model/library hapus-background tidak dinaikkan secara mayor.

Hasil `npm audit` sebelum/sesudah pada pekerjaan ini:

| Severity paket | Sebelum | Sesudah |
|---|---:|---:|
| Critical | 3 | 0 |
| High | 18 | 5 |
| Moderate | 19 | 12 |
| Low | 3 | 6 |
| Total | 43 | 23 |

Angka tersebut adalah hasil klasifikasi paket npm audit, bukan jumlah exploit yang dibuktikan atau CVE unik. Sisa high melibatkan rantai `@huggingface/transformers` → `sharp`, `zod`/`@vee-validate/zod`, dan beberapa versi transitive `postcss`. Sebagian memerlukan migrasi mayor atau perbaikan upstream; tidak ditutup dengan pemaksaan versi yang belum kompatibel. Sanitizer tidak mengizinkan atribut style dan menonaktifkan parsing style attributes.

Pekerjaan berikutnya: migrasi validation/model dependencies dengan regresi tersendiri; konfirmasi reachability advisory pada runtime/deployment; matriks role merchant, fail-closed tenant, rate limiting, dan hardening session/CSP/konfigurasi produksi sebagaimana dicatat pada review awal. Jangan menganggap output audit Critical=0 sebagai sertifikat aman.

## Referensi kebijakan parser

- [jsoup: sanitasi dengan parser dan safelist](https://jsoup.org/cookbook/cleaning-html/safelist-sanitizer).
- [sanitize-html: konfigurasi tag, atribut, dan protokol](https://github.com/apostrophecms/sanitize-html).
- [TwelveMonkeys ImageIO 3.15.3](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.15.3), digunakan untuk decoding WebP sebelum re-encode.
