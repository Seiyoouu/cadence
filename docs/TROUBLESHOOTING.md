# Troubleshooting Guide

Common issues and remediation procedures for Cadence. Detailed step-by-step guides will be finalized in Phase 8.

---

## Service killed in background (Xiaomi/Oppo/Vivo/Samsung)

Beberapa OEM Android (Xiaomi, Oppo, Vivo, Realme, Samsung) memiliki manajemen baterai agresif yang bisa membunuh VoiceService. Berikut cara mengatasinya:

### Xiaomi / Redmi / POCO (MIUI / HyperOS)
1. Settings → Apps → Manage Apps → Cadence → Autostart → ON
2. Settings → Apps → Manage Apps → Cadence → Battery saver → No restrictions
3. Settings → Battery & performance → App battery saver → Cadence → No restrictions
4. Recent apps → lock Cadence (tap and hold, pilih lock icon)

### Oppo / Realme (ColorOS)
1. Settings → Battery → App Battery Management → Cadence → Allow background activity
2. Settings → Apps → Cadence → Battery usage → Allow background activity

### Vivo (FuntouchOS)
1. Settings → Battery → Background power consumption management → Cadence → Allow
2. i Manager → App manager → Autostart manager → Cadence → ON

### Samsung (One UI)
1. Settings → Apps → Cadence → Battery → Unrestricted
2. Settings → Battery and device care → Battery → Background usage limits → Cadence → Never sleeping apps

### Generic (AOSP / Pixel)
1. Settings → Apps → Cadence → Battery → Unrestricted

Setelah setting di atas, restart HP dan test ulang Bluetooth auto-start.

---

## Notification Access not detected

<!-- Guidance on Android Notification Listener security toggles and re-enabling access to follow in Phase 8 -->

---

## Accessibility cannot find Play button

<!-- Guidance on UI layout changes, fallback vibration cues, and YouTube Music Morphe compatibility to follow in Phase 8 -->

---

## VOSK wake word not detected

1. Make sure your microphone permission is granted in Android Settings → Apps → Cadence → Permissions → Microphone.
2. Ensure you speak clearly into your helmet or TWS mic. The wake word triggers on `"Hey Cade"`, `"Hi Cade"`, `"Ok Cade"`, `"Cadence"`, or `"Hai Cade"`.
3. If riding at highway speeds with high wind noise, use physical TWS buttons (Media Button fallback) or check helmet wind-deflector padding.

---

## SpeechRecognizer not available

Cadence uses Android's native `SpeechRecognizer` service for high-accuracy multilingual command understanding.
1. On Google Play certified devices: Ensure the **Google** app is installed and enabled.
2. On custom ROMs or de-googled devices (MicroG / GrapheneOS): Install `Speech Services by Google` or an open-source offline speech provider that supports Android's `SpeechRecognizer` API.

---

## Speech recognition offline not working

To use voice command transcription without an active internet connection:
1. Open phone **Settings** → **Google** (or System) → **Voice**.
2. Select **Offline speech recognition**.
3. Under the **All** tab, download **Indonesian (Bahasa Indonesia)** and **English (US)** language packs.
