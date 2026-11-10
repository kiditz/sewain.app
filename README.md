# Sewain

Aplikasi rental multi-tenant: backend Spring Boot, frontend Nuxt, PostgreSQL, serta gateway WhatsApp Web. Panduan ini untuk development lokal, dengan Docker maupun menjalankan service langsung di komputer.

## Struktur project

| Folder/file | Isi |
| --- | --- |
| `backend/` | Spring Boot, Java 21, Gradle Wrapper, Flyway, business logic dan API |
| `web/` | Nuxt/Vue dan dashboard merchant/storefront |
| `whatsapp-api/` | Gateway Node.js berbasis whatsapp-web.js dan Chromium |
| `compose.yaml` | Stack Docker utama untuk semua service |
| `.env.example` | Template konfigurasi stack lokal |
| `web/e2e/` | Test Playwright terpisah dari dependency aplikasi web |
| `scripts/` | Script data dummy dan utilitas |
| `docs/` | Dokumentasi fitur dan pengujian |

## Persiapan environment

Jalankan dari root repository. Salin template **hanya untuk setup pertama**; kalau `.env` sudah ada, edit file tersebut tanpa menimpanya.

```sh
cp .env.example .env
```

Isi tiga secret berikut di `.env`. Jalankan generator masing-masing lalu salin hasilnya ke field yang sesuai, tanpa memasukkan output contoh ke repository:

```sh
# POSTGRES_PASSWORD
openssl rand -hex 32

# WHATSAPP_API_KEY
openssl rand -hex 32

# INTEGRATIONS_ENCRYPTION_KEY
openssl rand -base64 32
```

| Variable | Fungsi |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database Docker; default nama DB/user `sewain` |
| `INTEGRATIONS_ENCRYPTION_KEY` | Kunci base64 dari 32 byte untuk credential integrasi Email; simpan tetap antar restart/deployment |
| `WHATSAPP_API_KEY` | Akses privat gateway; Compose juga memakainya sebagai secret tanda tangan webhook |
| `PUBLIC_API_URL` | URL API/media yang bisa diakses browser; default `http://localhost:8080` |
| `APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` | Origin browser yang diizinkan untuk koneksi STOMP; template sudah memuat domain lokal Sewain |
| `MIGRATE_TENANTS` | Jalankan migrasi schema merchant lama saat backend startup; default `true` di root Compose |

Secret hanya untuk backend/gateway. Jangan taruh API key atau encryption key di konfigurasi `NUXT_PUBLIC_*`. Mengubah password di `.env` setelah volume PostgreSQL dibuat tidak otomatis mengubah password role database yang sudah ada.

## Jalankan dengan Docker

### Kebutuhan

- Docker Engine/Desktop aktif dan Docker Compose v2 (`docker compose`).
- Port host tersedia: web `3000`, backend `8080`, PostgreSQL `5433`, gateway `3001`.
- Java, Node.js, dan Gradle tidak perlu diinstall di host untuk mode ini.

Gunakan **root `compose.yaml`**. Compose di subfolder adalah konfigurasi terpisah; jangan dinyalakan bersamaan pada port yang sama.

### Build dan start pertama

Dari root project, setelah `.env` diisi:

```sh
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
```

Dockerfile backend sudah menjalankan Gradle `bootJar -x test`. Jadi tidak perlu build JAR terlebih dahulu. Build image tidak menjalankan test backend. Build pertama membutuhkan download base image, dependency Java/npm, dan Gradle.

Lihat startup/migrasi:

```sh
docker compose logs -f --tail=100 backend web whatsapp-api
```

Pastikan backend selesai startup dan migrasi tidak error. Healthcheck backend saat ini hanya mengecek port TCP, bukan kesiapan seluruh tenant.

### Alamat lokal

| Service | Alamat dari komputer/browser |
| --- | --- |
| Web | [sewain.app.localhost:3000](http://sewain.app.localhost:3000) |
| API/media | [localhost:8080](http://localhost:8080) |
| Gateway health | [localhost:3001/ping](http://localhost:3001/ping) |
| PostgreSQL | `127.0.0.1:5433`, database/user mengikuti `.env` |

Pakai `sewain.app.localhost` untuk web agar sesuai pola host tenant/CORS lokal. Host merchant dapat berupa `http://<slug-merchant>.sewain.app.localhost:3000`. Jika resolver/browser tidak mengenali hostname tersebut, arahkan hostname yang dipakai ke `127.0.0.1` melalui pengaturan hosts/resolver OS.

Di jaringan Docker, alamat antar service berbeda dari alamat browser: `postgres:5432`, `backend:8080`, dan `whatsapp-api:3000`. Compose mengatur `NUXT_API_BASE_URL=http://backend:8080` untuk SSR; browser memakai `PUBLIC_API_URL`.

### Update hanya service yang berubah

Ubah frontend saja:

```sh
docker compose build web
docker compose up -d --no-deps web
```

Ubah backend saja:

```sh
docker compose build backend
docker compose up -d --no-deps backend
```

Ubah gateway saja:

```sh
docker compose build whatsapp-api
docker compose up -d --no-deps whatsapp-api
```

Kalau file Compose/environment turut berubah, recreate service terkait dengan `up -d`. `restart` saja tidak menerapkan environment atau image baru. `--no-deps` mencegah Compose ikut menjalankan dependency ketika update satu service; dependency tersebut harus sudah aktif.

### Stop dan data persisten

```sh
# Stop container tanpa menghapusnya
docker compose stop

# Hapus container/network; named volume tetap ada
docker compose down
```

| Named volume | Data |
| --- | --- |
| `postgres-data` | Database dan schema tenant |
| `backend-data` | Key JWT dan file data backend |
| `backend-upload` | File media di `/app/upload` |
| `whatsapp-sessions` | Credential perangkat WhatsApp tertaut |
| `whatsapp-cache` | Cache gateway |

**`docker compose down -v` menghapus volume dan data di atas.** Jangan gunakan untuk update biasa. Database/file native di komputer tidak otomatis masuk ke named volume Docker; migrasi data harus melalui backup/restore dan penyalinan file yang sesuai.

## Jalankan tanpa Docker

### Kebutuhan

- JDK **21**.
- Node.js **22** dan npm (selaras dengan Dockerfile).
- PostgreSQL lokal; versi **16** selaras dengan stack Docker. Extension `pgcrypto` harus tersedia.
- Untuk gateway: Google Chrome/Chromium lokal, atau browser Puppeteer yang terinstall bersama dependency.

Gradle global tidak diperlukan; gunakan `backend/gradlew`. Konfigurasi saat ini menonaktifkan auto-configuration vector store, jadi extension `vector` tidak diperlukan untuk startup biasa. Docker memakai image PostgreSQL dengan pgvector agar siap jika fitur tersebut diaktifkan.

### 1. Siapkan PostgreSQL lokal

Untuk instalasi baru, masuk ke `psql` sebagai administrator PostgreSQL. Sesuaikan username admin dengan instalasi komputer:

```sh
psql -U postgres -d postgres
```

Di dalam `psql`:

```sql
CREATE ROLE sewain LOGIN;
\password sewain
-- Isi password yang sama dengan POSTGRES_PASSWORD di .env.
CREATE DATABASE sewain OWNER sewain;
ALTER DATABASE sewain SET timezone TO 'UTC';
\connect sewain
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\quit
```

Lewati `CREATE ROLE`/`CREATE DATABASE` jika sudah ada. Contoh berikut memakai PostgreSQL native port **5432**, bukan port host Docker **5433**. Role aplikasi harus memiliki database/schema dan bisa membuat schema merchant serta menjalankan Flyway.

### 2. Backend — terminal pertama

Dari root project, buka terminal baru:

```sh
cd backend

# Spring tidak membaca .env root otomatis. Muat file milik project sendiri ke shell.
set -a
source ../.env
set +a

export SPRING_DOCKER_COMPOSE_ENABLED=false
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB:-sewain}"
export SPRING_DATASOURCE_USERNAME="${POSTGRES_USER:-sewain}"
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
export APP_INTEGRATIONS_ENCRYPTION_KEY="$INTEGRATIONS_ENCRYPTION_KEY"
export APP_MAINTENANCE_MIGRATE_TENANTS="${MIGRATE_TENANTS:-true}"
export APP_MEDIA_STORAGE_PATH=upload
export APP_MEDIA_BASE_URL="${PUBLIC_API_URL:-http://localhost:8080}"
export WHATSAPP_BASE_URL=http://localhost:3001
export SERVER_PORT="${BACKEND_PORT:-8080}"
export SPRING_JPA_SHOW_SQL=false
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=INFO

./gradlew bootRun
```

`SPRING_DOCKER_COMPOSE_ENABLED=false` penting: dependency development Spring bisa mencoba menyalakan `backend/compose.yaml` jika tidak dimatikan. Jangan mengandalkan credential bawaan `application.yaml`; contoh di atas mengoverride-nya dari environment.

Dengan working directory `backend`, JWT/data tersimpan di `backend/data` dan media di `backend/upload`. JVM/JDBC memakai UTC. File tersebut perlu dipertahankan saat restart, sama seperti volume di mode Docker.

### 3. Frontend — terminal kedua

Dari root project:

```sh
cd web
npm ci
export NUXT_API_BASE_URL=http://localhost:8080
export NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
npm run dev -- --host 0.0.0.0
```

Buka [http://sewain.app.localhost:3000](http://sewain.app.localhost:3000). Jika backend memakai port lain, sesuaikan kedua URL tersebut. Jika web memakai port lain, sesuaikan juga origin CORS/STOMP yang diizinkan.

Untuk menjalankan hasil build Nuxt secara lokal:

```sh
npm run build
NUXT_API_BASE_URL=http://localhost:8080 NUXT_PUBLIC_API_BASE_URL=http://localhost:8080 node .output/server/index.mjs
```

Hentikan dev server terlebih dahulu agar port tidak bentrok.

### 4. Gateway WhatsApp — terminal ketiga (opsional)

Backend/frontend bisa digunakan tanpa gateway; integrasi WhatsApp baru berfungsi setelah gateway tersedia. Contoh memakai Chrome yang sudah terinstall di macOS:

```sh
cd whatsapp-api
set -a
source ../.env
set +a

export PORT=3001
export API_KEY="$WHATSAPP_API_KEY"
export WEBHOOK_SECRET="$WHATSAPP_API_KEY"
export WEBHOOK_TEXT_ONLY=true
export BASE_WEBHOOK_URL=http://localhost:8080/api/integrations/whatsapp/webhook
export SESSIONS_PATH=./sessions
export RECOVER_SESSIONS=true
export SET_MESSAGES_AS_SEEN=false
export ENABLE_LOCAL_CALLBACK_EXAMPLE=false
export ENABLE_SWAGGER_ENDPOINT=false
export CHROME_BIN='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'

# Browser lokal sudah tersedia, jadi tidak perlu download browser Puppeteer.
PUPPETEER_SKIP_DOWNLOAD=true npm ci
npm start
```

Linux/Windows: sesuaikan `CHROME_BIN` ke executable Chrome/Chromium. Jika menggunakan browser bawaan Puppeteer, install dependency tanpa `PUPPETEER_SKIP_DOWNLOAD` dan jangan set `CHROME_BIN`; proses install dapat mendownload browser.

Pastikan `API_KEY` gateway sama dengan `WHATSAPP_API_KEY` backend, dan `WEBHOOK_SECRET` sama dengan secret webhook backend. Dengan contoh di atas, fallback backend menggunakan key yang sama. Jika memakai `WHATSAPP_WEBHOOK_SECRET` terpisah, samakan nilainya di `WEBHOOK_SECRET` gateway.

Cek [http://localhost:3001/ping](http://localhost:3001/ping). Setelah itu buka **Merchant → Integrasi → WhatsApp → Hubungkan WhatsApp**, lalu scan QR dari HP merchant. Sesi native disimpan di `whatsapp-api/sessions`, terpisah dari volume Docker.

### Alternatif development: hanya DB memakai Docker

Untuk menjalankan Spring/Nuxt langsung di host tetapi menggunakan database root Compose:

```sh
# Dari root, .env harus sudah diisi.
docker compose up -d postgres
```

Gunakan langkah backend native dengan URL berikut sebagai pengganti URL port 5432:

```sh
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/${POSTGRES_DB:-sewain}"
./gradlew bootRun
```

Lakukan override dan `bootRun` dari terminal/folder `backend` yang environment-nya sudah dimuat. Jika `POSTGRES_PORT` di `.env` diubah, gunakan port tersebut. Untuk gateway lokal tetap gunakan langkah native, supaya tidak tercampur callback hostname Docker dan host.

## Registrasi dan setup merchant

1. Buka web, register akun, lalu login.
2. Buat merchant dan tunggu proses penyiapan toko selesai. Schema tenant dan default settings dibuat lewat Flyway/provisioning.
3. Lengkapi pengaturan toko, regional, rental dan pembayaran.
4. Tambahkan produk/varian/inventaris, atau gunakan **Produk → Import CSV** dan download template dari halaman tersebut.
5. Integrasi Email membutuhkan akun SMTP/IMAP merchant. Integrasi WhatsApp memakai nomor merchant yang ditautkan melalui QR.

Public Flyway berjalan saat backend startup. Untuk merchant lama, `APP_MAINTENANCE_MIGRATE_TENANTS=true` menjalankan migrasi tenant. Error settings/table setelah update perlu dicek bersama log migrasi. Tunggu status provisioning READY sebelum memakai halaman merchant.

## Test

Backend, dari root:

```sh
cd backend
./gradlew test
```

Unit test memakai mock/direct instance. Test arsitektur memverifikasi modul dan membuat dokumentasi tanpa menyalakan application context. Gradle tetap mengompilasi seluruh test source meskipun memakai `--tests` untuk satu kelas.

Panduan suite business logic: [docs/backend-business-unit-tests.md](docs/backend-business-unit-tests.md).

Test frontend yang tersedia, dari root:

```sh
cd web
npm run test:storefront
npm run test:product-tags
```

E2E browser menggunakan stack yang sudah berjalan, dengan dependency terpisah:

```sh
cd web/e2e
npm install
npx playwright install chromium
```

Isi `E2E_BASE_URL`, `E2E_EMAIL`, `E2E_PASSWORD`, `E2E_MERCHANT_NAME`, dan `E2E_REQUIRE_AUTH=true`, lalu jalankan `npm test` dari folder itu. Detail skenario dan report: [web/e2e/README.md](web/e2e/README.md).

## Troubleshooting singkat

| Gejala | Yang dicek |
| --- | --- |
| Koneksi PostgreSQL ditolak | Service aktif, port native `5432` atau Docker `5433`, URL dan credential sesuai mode |
| Register/login mengarah ke Nuxt `/api/auth/...` lalu 404 | `NUXT_PUBLIC_API_BASE_URL` harus menunjuk Spring; SSR juga memerlukan `NUXT_API_BASE_URL` yang benar |
| STOMP ditolak | `APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` mencakup origin lengkap web (scheme, hostname, port) |
| Settings/table tenant belum ada | Log provisioning/Flyway, `MIGRATE_TENANTS` di Compose atau `APP_MAINTENANCE_MIGRATE_TENANTS` untuk native |
| WhatsApp gateway unavailable | Gateway aktif, backend memakai URL internal Docker atau `localhost:3001` sesuai mode, API key cocok |
| Pesan inbound tidak masuk | Callback URL bisa dijangkau gateway, secret webhook cocok, session aktif; mode saat ini hanya pesan teks langsung |
| Build web ikut menjalankan backend | Build service eksplisit; recreate web dengan `docker compose up -d --no-deps web` |
| Gradle download lagi | Wrapper mengunduh versi yang dikonfigurasi jika cache hilang/versi berubah; pruning build cache juga dapat membuang cache Gradle Docker |

Tidak perlu menghapus volume untuk memperbaiki frontend, mengupdate image, atau menjalankan test. Command di README ini adalah panduan; penulisannya tidak menjalankan build, container, migrasi, atau test.

## Dokumentasi lanjutan

- [Docker dan storage](DOCKER.md)
- [WhatsApp integration](docs/whatsapp-integration.md)
- [Product CSV import](docs/product-csv-import.md)
- [Standar HTTP keluar backend](docs/backend-http-clients.md)
- [Backend business unit tests](docs/backend-business-unit-tests.md)
- [Frontend E2E](web/e2e/README.md)
