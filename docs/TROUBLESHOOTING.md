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

<!-- Guidance on microphone gain, helmet wind noise, TWS Bluetooth audio routing, and model selection to follow in Phase 8 -->
