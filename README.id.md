[Bahasa Indonesia](README.id.md) · [English](README.md)

# Cadence

> Pengendali musik berbasis suara tanpa sentuh untuk YouTube Music Morphe, dirancang khusus untuk pengendara motor dengan interkom helm Bluetooth dan TWS.

[![CI Build](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![Pre-release](https://img.shields.io/github/v/release/Seiyoouu/cadence?include_prereleases&color=00E5FF&label=pre-release)](https://github.com/Seiyoouu/cadence/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-0F172A?logo=android&logoColor=00E5FF)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-GPLv3-1E293B.svg)](LICENSE)

<p align="center">
  <img src="docs/test-artifacts/ui_craft_top_v2.png" alt="Cadence Rider Cockpit" width="340" style="border-radius: 24px; box-shadow: 0 20px 40px rgba(0, 229, 255, 0.15);" />
</p>

---

## Gambaran Umum

Mengganti lagu atau mencari musik saat berkendara motor sangat berisiko. Pengendara terpaksa harus menepi ke bahu jalan, melepas sarung tangan tebal, membuka kunci ponsel di bawah terik matahari, dan menekan tombol layar sentuh kecil.

**Cadence** menghilangkan sentuhan fisik seutuhnya. Ponsel tetap terkunci di saku jaket, cukup berbicara melalui mikrofon helm Anda, dan kendalikan pemutaran musik tanpa perlu melihat layar.

---

## Kemampuan Utama

| Kemampuan | Mekanisme Teknis | Manfaat Pengendara |
|:---|:---|:---|
| **Eksekusi Layar Mati & Di Saku** | Foreground service dengan `PARTIAL_WAKE_LOCK` serta tipe service `microphone` dan `connectedDevice`. | Berjalan stabil tanpa dibungkam OS meski layar mati dan HP tersimpan lama di saku jaket. |
| **Mesin Suara Bilingual Cepat** | Stack hibrida: Model akustik VOSK Kaldi tersimpan di RAM untuk deteksi kata panggil instan (<10ms) dipadu Android `SpeechRecognizer` native. | Memahami Bahasa Indonesia dan English secara simultan tanpa delay memuat ulang model storage. |
| **Pencarian Lagu Alami** | Resolusi kueri terdesentralisasi via endpoint REST Piped & Invidious langsung ke deep link YouTube Music. | Cukup sebutkan judul lagu atau artis secara langsung (misal *"Dewa 19 Kangen"*, *"Sheila On 7 Dan"*, atau *"Coldplay Yellow"*), tanpa perlu awalan kata kaku. |
| **Rider Mode Bluetooth Otomatis** | BroadcastReceiver reaktif memantau status koneksi A2DP dan Headset. | Suara otomatis aktif saat interkom helm terhubung, dan langsung nonaktif saat terputus guna menghemat baterai. |
| **Umpan Balik Haptic Helm** | Pola getaran presisi (`Vibrator` / `VibrationEffect`). | Memberikan getaran konfirmasi fisik yang jelas di saku saat kata panggil dan perintah berhasil dieksekusi. |
| **Fallback Tombol Fisik TWS** | Intent routing perangkat keras `MEDIA_BUTTON`. | Tombol fisik media TWS tetap menjadi cadangan instan saat kebisingan angin di jalan raya terlalu kencang. |
| **Arsitektur 100% Rootless** | IPC langsung via Android `MediaSession` dan `NotificationListenerService`. | Tanpa root, Magisk, atau Xposed. Mendukung package Morphe (`app.morphe...`), ReVanced, dan Google stock YTM. |

---

## Persyaratan Sistem

- **Sistem Operasi**: Android 8.0 (API level 26) ke atas.
- **Aplikasi Musik Pendamping**: [YouTube Music Morphe](https://morphe.software) (`app.morphe.android.apps.youtube.music`), ReVanced, atau YouTube Music resmi.
- **Layanan Suara**: Aplikasi Google / Speech Services by Google (bawaan standar 99% HP Android).
- **Perangkat Audio**: Interkom helm Bluetooth (Sena, Cardo, FreedConn, Ejeas) atau TWS earbud dengan mikrofon.

---

## Perintah Suara yang Didukung

### Kata Panggil (Wake Words)
Cadence menggunakan sapaan majemuk untuk mencegah aktivasi tak sengaja saat mengobrol biasa di jalan:
- `"Hai Kadi"` / `"Halo Kadi"` / `"Hey Kadi"` / `"Oke Kadi"`
- `"Hai Musik"` / `"Halo Musik"`
- `"Hai Bro"` / `"Halo Bro"`
- `"Hai Cade"` / `"Hey Cade"` / `"Halo Cade"`
- `"Cadence"`

### Kontrol Pemutaran & Pencarian
```text
Kontrol Pemutaran:
├── Lagu Berikutnya : "next", "skip", "lagu berikutnya", "ganti lagu", "selanjutnya"
├── Lagu Sebelumnya : "previous", "prev", "back", "lagu sebelumnya", "kembali"
├── Jeda / Berhenti : "pause", "stop", "jeda", "berhenti", "matikan"
├── Lanjutkan       : "play", "resume", "lanjut", "mainkan", "putar"
└── Cari Langsung   : Cukup sebutkan judul lagu atau artis:
                      • "Dewa 19 Kangen"
                      • "Sheila On 7 Dan"
                      • "Coldplay Yellow"
                      • "Putar lagu Komang"
```

---

## Arsitektur

```text
┌─────────────────────────────────────────────────────────────┐
│                        MIKROFON HELM                        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Aliran Audio (16kHz PCM)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 ENGINE KATA PANGGIL (Offline)               │
│   Model Akustik VOSK Kaldi di RAM (assets/model-en/)        │
│   Latensi: <10ms deteksi instan | Hemat daya baterai        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Kata panggil terdeteksi + Getar Haptic
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 STT PERINTAH (Android Native)               │
│   Multi-Hypothesis SpeechRecognizer (id-ID + en-US)         │
│   Mengekstrak 5 kandidat tebakan audio                      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Array teks kandidat
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 PARSER PERINTAH BILINGUAL                   │
│   Evaluasi multi-pass regex frasa alami Indonesia & Inggris │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
       [Perintah Transport]           [Kueri Pencarian]
                │                             │
                ▼                             ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│       TRANSPORT ENGINE       │ │    SEARCH & PLAY ENGINE    │
│   MediaSession / Controller  │ │   Piped/Invidious REST API │
│   Play / Pause / Next / Prev │ │   Deep link auto-play      │
└───────────────┬──────────────┘ └────────────┬───────────────┘
                │                             │
                └──────────────┬──────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│             YOUTUBE MUSIC MORPHE / REVANCED                 │
│         Berputar di latar belakang saat HP di saku          │
└─────────────────────────────────────────────────────────────┘
```

---

## Panduan Instalasi

### 1. Unduh File APK
Ambil rilis terbaru dari halaman [GitHub Releases](https://github.com/Seiyoouu/cadence/releases/tag/v1.0.0):
- **File**: `Cadence-v1.0.0.apk`

> **Catatan**: Jika Anda memiliki instalasi Cadence versi uji sebelumnya, hapus (*uninstall*) terlebih dahulu agar verifikasi tanda tangan paket berjalan lancar.

### 2. Panduan Sideloading

#### Peringatan Google Play Protect
Karena Cadence merupakan aplikasi sumber terbuka non-Play Store yang meminta akses background mikrofon dan notifikasi, Play Protect mungkin memunculkan dialog verifikasi pengembang:
- Buka Play Store: **Foto Profil > Play Protect > Ikon Gerigi (Pengaturan) > Jeda selama 24 jam**.
- Lanjutkan instalasi file APK. Play Protect dapat diaktifkan kembali setelah selesai.

#### Android 13+ Setelan yang Dibatasi (Restricted Settings)
Pada versi Android baru, izin sensitif (Akses Notifikasi) untuk aplikasi sideload dibatasi secara bawaan:
1. Buka menu **Setelan HP > Aplikasi > Cadence**.
2. Ketuk **menu titik tiga (⋮)** di pojok kanan atas layar.
3. Pilih **Izinkan setelan yang dibatasi** dan konfirmasi kunci layar Anda.
4. Buka aplikasi Cadence dan berikan Akses Notifikasi melalui panduan onboarding.

---

## Kompilasi dari Sumber (Build)

Prasyarat: JDK 17 dan Android SDK Platform 34.

```bash
git clone https://github.com/Seiyoouu/cadence.git
cd cadence
./gradlew assembleRelease
```

File APK rilis akan dihasilkan di:
`app/build/outputs/apk/release/Cadence-v1.0.0.apk`

---

## Privasi & Keamanan

- **Mikrofon**: Pemrosesan audio untuk deteksi kata panggil berlangsung 100% lokal di perangkat. Transkripsi perintah diteruskan ke sistem native Android. Cadence tidak mengumpulkan, menyimpan, atau mengirim rekaman suara ke server mana pun.
- **Keamanan Akun**: Cadence berjalan 100% rootless dan tidak pernah meminta kredensial akun Google.
- **Telemetri**: Tanpa pelacak, tanpa analitik, dan tanpa SDK pihak ketiga.

---

## Lisensi

Cadence adalah perangkat lunak sumber terbuka yang dilisensikan di bawah [GNU General Public License v3.0](LICENSE).
