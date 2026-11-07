🇮🇩 Bahasa Indonesia | 🇬🇧 [English](README.md)

# Cadence

[![Build Debug APK](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/Seiyoouu/cadence?include_prereleases&color=orange)](https://github.com/Seiyoouu/cadence/releases)

Cadence adalah aplikasi Android 100% rootless yang memungkinkan pengendara motor mengontrol YouTube Music Morphe sepenuhnya lewat suara tanpa menyentuh layar, dirancang khusus untuk helm dengan TWS.

---

## 🚧 Status: Sedang Dalam Pengembangan (Fase 1/9)

Cadence sedang dalam tahap pengembangan aktif. Fondasi repositori dan kerangka awal aplikasi Android pada Fase 1 telah selesai.

---

## 📱 Persyaratan Sistem

- **Versi Android**: Android 8.0+ (API level 26 ke atas)
- **Aplikasi Pemutar Musik**: [YouTube Music Morphe](https://morphe.software) (v9.15.51+)
- **Perangkat Keras**: TWS Bluetooth atau interkom helm dengan mikrofon

---

## 🛠️ Cara Build

Prasyarat: JDK 17 dan Android SDK (API 34).

```bash
# Clone repositori
git clone https://github.com/Seiyoouu/cadence.git
cd cadence

# Build APK debug
./gradlew assembleDebug
```

File APK hasil kompilasi akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 Lisensi

Proyek ini dilisensikan di bawah [GNU General Public License v3.0](LICENSE).

---

🇮🇩 Bahasa Indonesia | 🇬🇧 [English](README.md)
