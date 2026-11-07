🇮🇩 Bahasa Indonesia | 🇬🇧 [English](README.md)

# Cadence

[![Build Debug APK](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/Seiyoouu/cadence?include_prereleases&color=orange)](https://github.com/Seiyoouu/cadence/releases)

**Cadence** adalah aplikasi Android 100% rootless yang memungkinkan pengendara motor mengontrol [YouTube Music Morphe](https://morphe.software) sepenuhnya lewat suara tanpa menyentuh layar, dirancang khusus untuk helm dengan TWS atau interkom Bluetooth.

---

## 🏍️ Mengapa Cadence?

Mengganti lagu atau mencari musik saat berkendara motor sangat berbahaya: pengendara harus menepi, melepas sarung tangan, membuka kunci ponsel, dan menyentuh layar.

Cadence menghilangkan kebutuhan menyentuh layar:
- Ponsel tetap aman terkunci di dalam saku jaket.
- Cukup ucapkan **"Hai Cade"** atau **"Hey Cade"** melalui mikrofon helm/TWS Anda.
- Kontrol pemutaran, ganti lagu, atau cari dan putar lagu apa saja secara instan.

---

## ✨ Fitur Utama

- **Continuous Offline Wake Word**: Didukung engine VOSK ringan (`"Hai Cade"`, `"Halo Cade"`, `"Hey Cade"`, `"Cadence"`).
- **Perintah Suara Bilingual**: Memahami Bahasa Indonesia DAN Bahasa Inggris secara simultan:
  - **Berikutnya**: `lagu berikutnya`, `ganti lagu`, `maju`, `next`, `skip`
  - **Sebelumnya**: `lagu sebelumnya`, `kembali`, `mundur`, `previous`, `prev`, `back`
  - **Jeda**: `jeda`, `berhenti`, `diam`, `pause`, `stop`
  - **Putar**: `lanjut`, `mainkan`, `putar`, `play`, `resume`
  - **Cari & Putar**: `"putar lagu [judul]"`, `"mainkan [judul]"`, `"cari [judul]"`, `"play [title]"`
- **Deep Link Auto-Play**: Pencarian lagu di-resolve via API Piped/Invidious dan langsung memicu YouTube Music Morphe auto-play bahkan saat layar mati dan terkunci di saku.
- **Rider Mode (Otomatisasi Bluetooth)**: Layanan suara otomatis aktif saat TWS/interkom helm terhubung, dan otomatis mati saat terputus untuk menghemat baterai.
- **Fallback Tombol Fisik TWS**: Dukungan tombol media TWS saat suara bising angin terlalu kencang.
- **Feedback Sensorik Helm**: Getaran haptic khusus dan konfirmasi suara Text-to-Speech (TTS) opsional.
- **100% Rootless**: Tanpa root, Magisk, atau Xposed.

---

## 📱 Persyaratan Sistem

- **Versi Android**: Android 8.0+ (API level 26 ke atas).
- **Aplikasi Musik Target**: [YouTube Music Morphe](https://morphe.software) (`com.google.android.apps.youtube.music`).
- **Layanan Suara**: Android `SpeechRecognizer` (Aplikasi Google / Speech Services).
- **Perangkat Keras**: TWS Bluetooth atau interkom helm Bluetooth dengan mikrofon.

---

## 📦 Ukuran APK & Arsitektur

- **Ukuran APK**: **~99MB** (Rilis Produksi v1.0).
- **Arsitektur Hybrid Speech**:
  - **Kata Kunci (Wake Word)**: Model akustik offline VOSK (`assets/model-en/`).
  - **Transkripsi Perintah**: Android `SpeechRecognizer` native (akurasi tinggi Bahasa Indonesia dan Inggris tanpa perlu mendownload model raksasa tambahan).

---

## 🛠️ Cara Build dari Sumber

Prasyarat: JDK 17 dan Android SDK (API 34).

```bash
git clone https://github.com/Seiyoouu/cadence.git
cd cadence
./gradlew assembleDebug
```

File APK hasil kompilasi akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Penjelasan Izin Sistem

| Izin | Tujuan |
|---|---|
| `Akses Notifikasi` | Mendeteksi sesi aktif YouTube Music Morphe dan mengontrol playback. |
| `RECORD_AUDIO` | Mendengarkan kata kunci dan perintah suara. |
| `BLUETOOTH_CONNECT` | Mendeteksi koneksi TWS helm untuk auto-start Rider Mode. |
| `FOREGROUND_SERVICE` | Menjaga pengenalan suara tetap aktif saat layar mati. |

Untuk dokumentasi privasi lengkap, lihat [docs/PERMISSIONS.md](docs/PERMISSIONS.md).

---

## 📜 Lisensi

Dilisensikan di bawah [GNU General Public License v3.0](LICENSE).

---

🇮🇩 Bahasa Indonesia | 🇬🇧 [English](README.md)
