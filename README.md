🇬🇧 English | 🇮🇩 [Bahasa Indonesia](README.id.md)

# Cadence 🎵🏍️ (Voice Control for YT Music on Motorcycles)

[![Build Debug APK](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml/badge.svg)](https://github.com/Seiyoouu/cadence/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Seiyoouu/cadence?color=orange)](https://github.com/Seiyoouu/cadence/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

<p align="center">
  <img src="docs/test-artifacts/demo.gif" alt="Cadence Demo" width="300" />
</p>

---

## 🛑 The Problem

Changing songs or searching for music while riding a motorcycle is inconvenient and hazardous. Riders frequently have to pull over to the shoulder, take off riding gloves, unlock their phones, and fiddle with tiny touchscreen buttons in the glare. **Cadence** completely resolves this with 100% hands-free, screen-off voice control tailored for riders wearing helmets with Bluetooth TWS or intercoms.

---

## ✨ Features

- 🎙️ **Voice Control**: Play, pause, skip tracks, and search songs directly using your voice without touching the screen.
- 🗣️ **Flexible Wake Words**: Use whatever is easiest for you: *"Hai Kadi"*, *"Hai Kade"*, *"Hai Musik"*, *"Hai Bro"*, *"Cadence"*, or *"Hey Cade"*.
- 🔍 **Natural Song Search**: Simply state the song title or artist directly (e.g., *"Dewa 19 Kangen"*, *"Sheila On 7 Dan"*, *"Komang"*, *"Coldplay Yellow"*), with or without saying *"play"*.
- 🌐 **Bilingual Speech Engine**: Seamlessly understands Bahasa Indonesia AND English commands simultaneously.
- 🔋 **Rider Mode (Bluetooth Automation)**: Automatically starts listening when your helmet TWS/intercom connects, and halts immediately upon disconnect to save battery.
- 📱 **Screen-Off Support**: Change and search songs even while your phone stays locked deep inside your jacket pocket.
- 🔓 **100% Rootless**: Safe and clean without root, Magisk, or Xposed. Supports YouTube Music Morphe (`app.morphe...`, `app.revanced...`) and Google stock YTM.
- 🎛️ **Physical TWS Fallback**: Physical media button support when wind noise at highway speeds is too loud for voice recognition.
- 📳 **Haptic & Spoken Feedback**: Instant helmet vibration pulse upon wake word detection and optional spoken TTS confirmations.

---

## 📱 Requirements

- **Android Version**: Android 8.0+ (API level 26 or higher).
- **Target Music App**: [YouTube Music Morphe](https://morphe.software), ReVanced, or Stock YTM.
- **Speech Service**: Google App / Speech Services (for Android native `SpeechRecognizer`).
- **Hardware**: Bluetooth TWS earbuds or helmet intercom with microphone.

---

## 🚀 Installation & Setup

1. **Download APK**: Grab the latest `Cadence-v1.0.0.apk` from the [Releases](https://github.com/Seiyoouu/cadence/releases) page.
2. **⚠️ Handling Google Play Protect Warnings**:
   > Because Cadence is an open-source sideloaded application requesting notification & mic access, Google Play Protect may warn or block installation. **This is completely normal and safe.**
   > 
   > **Smoothest Installation Method (The 24-Hour Pause):**
   > 1. Open the **Google Play Store** app.
   > 2. Tap your **profile picture** (top right) > **Play Protect** > **Gear icon** (Settings).
   > 3. Toggle off **"Scan apps with Play Protect"**.
   > 4. Google will offer an option to **"Pause for 24 hours"**. Select **Yes / Pause**.
   > 5. Now open the downloaded Cadence APK and install normally.
   > 6. *(Optional)* After installation, you can re-enable Play Protect at any time.
3. **🔓 Fixing Greyed-Out Notification Permission (Restricted Settings)**:
   > If Notification Access is greyed out or shows *"Restricted setting"* (Android 13+), go to **Settings > Apps > Cadence > 3-Dot Menu (top right) > Allow restricted settings**. Afterwards, return to Cadence and enable Notification Access. Full OEM guide in [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).
4. **Interactive Onboarding**: Launch Cadence and complete the 4-step setup (Notification Access, Microphone, Bluetooth).
5. **Hit the Road**: Connect your helmet TWS, say **"Hey Cade"** or **"Hai Cade"**, and command your music hands-free!

---

## 🧠 How It Works (Architecture)

```text
[Helmet / TWS Mic] ──> Wake Word ("Hey Cade" / VOSK) ──> Command STT (Android SpeechRecognizer)
                                                                    │
[Pocket / Screen-Off] ◄── Auto-Play Deep Link ◄── [Piped / Invidious API]
                                     │
                                     ▼
                          [YouTube Music Morphe]
```

1. **Wake Word Layer**: Continuous, battery-efficient offline listening via bundled VOSK acoustic engine (`assets/model-en/`).
2. **Command STT Layer**: One-shot multi-language recognition via Android's native `SpeechRecognizer` (supports offline language packs).
3. **Execution Layer**: Dispatches transport controls via `MediaSession` or resolves decentralized music search queries via Piped/Invidious deep links.

---

## 🤝 Contributing

Contributions, bug reports, and pull requests are warmly welcomed! Please check [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines, commit standards, and build instructions.

---

## 📜 License

Cadence is free and open-source software licensed under the [GNU General Public License v3.0](LICENSE).
