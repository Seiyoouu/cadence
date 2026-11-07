🇬🇧 English | 🇮🇩 [Bahasa Indonesia](README.id.md)

# Cadence

[![Build Debug APK](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/Seiyoouu/cadence?include_prereleases&color=orange)](https://github.com/Seiyoouu/cadence/releases)

Cadence is a 100% rootless Android application providing hands-free voice control for YouTube Music Morphe, designed specifically for motorcycle riders wearing helmets with TWS earbuds.

---

## 🚧 Status: Under Development (Phase 3/9)

Cadence is currently under active development. Phase 3 (Search Engine with API + Deep Link auto-play) is fully implemented, screen-off verified, and rootless against YouTube Music Morphe.

---

## 📱 Requirements

- **Android Version**: Android 8.0+ (API level 26 or higher)
- **Target Music Player**: [YouTube Music Morphe](https://morphe.software) (v9.15.51+)
- **Hardware**: Bluetooth TWS earbuds or helmet intercom with microphone

---

## 🛠️ Build

Prerequisites: JDK 17 and Android SDK (API 34).

```bash
# Clone the repository
git clone https://github.com/Seiyoouu/cadence.git
cd cadence

# Build debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

---

🇬🇧 English | 🇮🇩 [Bahasa Indonesia](README.id.md)
