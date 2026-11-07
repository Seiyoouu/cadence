🇬🇧 English | 🇮🇩 [Bahasa Indonesia](README.id.md)

# Cadence

[![Build Debug APK](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/Seiyoouu/cadence?include_prereleases&color=orange)](https://github.com/Seiyoouu/cadence/releases)

**Cadence** is a 100% rootless Android application providing complete hands-free voice control for [YouTube Music Morphe](https://morphe.software), crafted specifically for motorcycle riders wearing helmets with TWS earbuds or intercoms.

---

## 🏍️ Why Cadence?

Changing songs or searching for tracks while riding a motorcycle is dangerous: riders have to pull over, take off gloves, unlock their phones, and navigate small touch targets. 

Cadence solves this with zero physical screen contact:
- Keep your phone safely locked in your jacket pocket.
- Say **"Hey Cade"** or **"Hai Cade"** into your helmet microphone.
- Control playback, skip tracks, or search and play any song directly.

---

## ✨ Features

- **Continuous Offline Wake Word**: Powered by a lightweight VOSK engine (`"Hey Cade"`, `"Hi Cade"`, `"Hai Cade"`, `"Cadence"`).
- **Bilingual Voice Commands**: Seamlessly understands English AND Bahasa Indonesia commands simultaneously:
  - **Next**: `next`, `skip`, `lagu berikutnya`, `ganti lagu`, `maju`
  - **Previous**: `previous`, `prev`, `back`, `lagu sebelumnya`, `kembali`, `mundur`
  - **Pause**: `pause`, `stop`, `jeda`, `berhenti`, `diam`
  - **Play**: `play`, `resume`, `lanjut`, `mainkan`, `putar`
  - **Search & Play**: `"play [title]"`, `"putar lagu [title]"`, `"mainkan [title]"`, `"cari [title]"`
- **Deep Link Auto-Play**: Song search resolves via decentralized Piped/Invidious APIs and launches YouTube Music Morphe instantly even when the screen is locked and off.
- **Rider Mode (Bluetooth Automation)**: Automatically starts voice control when your helmet TWS/intercom connects, and halts when disconnected to preserve battery life.
- **Physical Media Button Fallback**: Use TWS buttons when wind noise is too loud.
- **Sensory Helmet Feedback**: Distinct haptic vibration patterns and optional Text-to-Speech (TTS) voice confirmation.
- **100% Rootless**: No root, Magisk, or Xposed required. Works cleanly with patched Morphe APKs.

---

## 📱 Requirements

- **Android Version**: Android 8.0+ (API level 26 or higher).
- **Target Music App**: [YouTube Music Morphe](https://morphe.software) (`com.google.android.apps.youtube.music`).
- **Speech Service**: Android `SpeechRecognizer` (Google app / Speech Services).
- **Hardware**: Bluetooth TWS earbuds or helmet Bluetooth intercom with mic.

---

## 📦 APK Size & Architecture

- **APK Size**: **~99MB** (Production v1.0).
- **Hybrid Speech Architecture**:
  - **Wake Word**: Offline bundled VOSK acoustic model (`assets/model-en/`).
  - **Command Transcription**: Android native `SpeechRecognizer` (high-accuracy bilingual Indonesian and English without bundling bulky redundant speech models).

---

## 🛠️ Build from Source

Prerequisites: JDK 17 and Android SDK (API 34).

```bash
git clone https://github.com/Seiyoouu/cadence.git
cd cadence
./gradlew assembleDebug
```

The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Permissions Explained

| Permission | Purpose |
|---|---|
| `Notification Access` | Detects active YouTube Music Morphe session and controls playback. |
| `RECORD_AUDIO` | Listens for wake words and voice commands. |
| `BLUETOOTH_CONNECT` | Detects helmet TWS connection for Rider Mode auto-start. |
| `FOREGROUND_SERVICE` | Keeps voice recognition active while riding with screen off. |

For detailed security disclosures, see [docs/PERMISSIONS.md](docs/PERMISSIONS.md).

---

## 📜 License

Licensed under the [GNU General Public License v3.0](LICENSE).

---

🇬🇧 English | 🇮🇩 [Bahasa Indonesia](README.id.md)
