🇮🇩 Bahasa Indonesia | 🇬🇧 [English](README.md)

# Cadence 🎵🏍️ (Kontrol Suara untuk YT Music di Motor)

[![Build Debug APK](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Seiyoouu/cadence?color=orange)](https://github.com/Seiyoouu/cadence/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

<p align="center">
  <img src="docs/test-artifacts/demo.gif" alt="Cadence Demo" width="300" />
</p>

---

## 🛑 Masalah Utama

Mengganti lagu atau mencari musik saat berkendara motor sangat berbahaya. Pengendara seringkali harus menepi ke pinggir jalan, melepas sarung tangan, membuka kunci HP, dan menekan tombol layar sentuh kecil di bawah terik matahari. **Cadence** menyelesaikan masalah ini dengan kontrol suara 100% hands-free tanpa perlu menyalakan layar, dirancang khusus untuk pengendara helm dengan TWS atau interkom Bluetooth.

---

## ✨ Fitur Utama

- 🎙️ **Voice Control**: Putar, jeda, lewati trek, dan cari lagu langsung dengan suara tanpa menyentuh layar.
- 🗣️ **Bilingual Speech Engine**: Memahami perintah Bahasa Indonesia DAN Bahasa Inggris secara simultan, apa pun pengaturan bahasa HP Anda.
- 🔋 **Rider Mode (Otomatisasi Bluetooth)**: Otomatis mendengarkan saat TWS/interkom helm terhubung, dan otomatis nonaktif saat terputus untuk menghemat baterai.
- 📱 **Dukungan Layar Mati (Screen-Off)**: Tetap bisa ganti lagu dan cari musik meski HP berada di dalam saku jaket dengan layar terkunci.
- 🔓 **100% Rootless**: Aman dan bersih tanpa root, Magisk, atau Xposed. Bekerja langsung dengan YouTube Music Morphe via MediaSession dan deep link.
- 🎛️ **Fallback Tombol Fisik TWS**: Dukungan tombol media fisik ketika kebisingan angin di jalan raya terlalu tinggi.
- 📳 **Feedback Haptic & Suara**: Pola getaran helm khusus dan konfirmasi suara Text-to-Speech (TTS) opsional.

---

## 📱 Persyaratan Sistem

- **Versi Android**: Android 8.0+ (API level 26 ke atas).
- **Aplikasi Musik Target**: [YouTube Music Morphe](https://morphe.software) (`com.google.android.apps.youtube.music`).
- **Layanan Suara**: Aplikasi Google / Speech Services (untuk Android native `SpeechRecognizer`).
- **Perangkat Keras**: TWS Bluetooth atau interkom helm dengan mikrofon.

---

## 🚀 Panduan Instalasi & Pengaturan

1. **Unduh APK**: Ambil file `app-release.apk` terbaru dari halaman [Releases](https://github.com/Seiyoouu/cadence/releases).
2. **⚠️ Peringatan Play Protect & Pengaturan Dibatasi (PENTING)**:
   > **Saat memasang Cadence via sideload**, Google Play Protect mungkin menampilkan *"Aplikasi tidak dikenal"*. Ketuk **Rincian selengkapnya** → **Tetap instal**.<br>
   > Pada **Android 13+**, jika Akses Notifikasi berwarna abu-abu atau bertuliskan *"Setelan dibatasi"*, buka **Pengaturan HP → Aplikasi → Cadence → ketuk ikon titik tiga (⋮) di pojok kanan atas → pilih "Izinkan setelan yang dibatasi"** sebelum mengaktifkan Akses Notifikasi. Panduan lengkap per merek HP tersedia di [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).
3. **Onboarding Interaktif**: Buka Cadence dan selesaikan panduan 4 langkah (Akses Notifikasi, Mikrofon, Bluetooth).
4. **Siap Berkendara**: Sambungkan TWS helm Anda, ucapkan **"Hai Cade"** atau **"Hey Cade"**, dan nikmati perjalanan tanpa repot!

---

## 🧠 Cara Kerja (Arsitektur)

```text
[Mikrofon Helm / TWS] ──> Kata Kunci ("Hai Cade" / VOSK) ──> STT Perintah (Android SpeechRecognizer)
                                                                           │
[Saku / Layar Mati] ◄── Auto-Play Deep Link ◄── [Piped / Invidious API] ───┘
                                   │
                                   ▼
                        [YouTube Music Morphe]
```

1. **Lapisan Kata Kunci (Wake Word)**: Pemantauan offline hemat daya menggunakan model akustik VOSK ringan (`assets/model-en/`).
2. **Lapisan STT Perintah**: Pengenalan multibahasa instan via Android native `SpeechRecognizer` (mendukung paket bahasa offline).
3. **Lapisan Eksekusi**: Mengirim kontrol playback lewat `MediaSession` atau men-resolve kueri pencarian musik terdesentralisasi via deep link Piped/Invidious.

---

## 🤝 Berkontribusi

Kontribusi kode, laporan bug, dan pull request sangat terbuka! Silakan baca panduan lengkap di [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📜 Lisensi

Cadence adalah perangkat lunak sumber terbuka yang dilisensikan di bawah [GNU General Public License v3.0](LICENSE).
