# Permissions Guide

This document outlines all system permissions requested by Cadence and the technical rationale for each. Cadence is 100% rootless and requests only permissions necessary for hands-free motorcycle operation.

---

## 1. Notification Access (`NotificationListenerService`)

- **Why it is needed**: To detect active `MediaSession` playback from YouTube Music Morphe (`com.google.android.apps.youtube.music`), read track metadata (title, artist, state), and route transport controls (play, pause, next, previous) zero-latency without root.
- **Privacy**: Cadence strictly filters notifications for YouTube Music Morphe and ignores all private messages, personal apps, and notifications.

---

## 2. Microphone (`android.permission.RECORD_AUDIO`)

- **Why it is needed**:
  1. Offline wake word listening via VOSK English acoustic model (`"Hey Cade"`, `"Hai Cade"`).
  2. One-shot command recognition via Android `SpeechRecognizer` (`"next"`, `"jeda"`, `"putar lagu Dewa 19"`).
- **Privacy**: Audio is analyzed locally on the device. Audio frames are never uploaded to Cadence servers (Cadence has no backend servers).

---

## 3. Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH`)

- **Why it is needed**: Enables **Rider Mode** auto-start. When your motorcycle helmet intercom or TWS connects via Bluetooth A2DP/Headset, Cadence starts `VoiceService` automatically so you never have to take out your phone before riding. When Bluetooth disconnects, the service stops immediately to save battery.

---

## 4. Foreground Service (`FOREGROUND_SERVICE_CONNECTED_DEVICE`, `FOREGROUND_SERVICE_MICROPHONE`)

- **Why it is needed**: To keep voice detection and Bluetooth listener active in the background while your phone is locked and kept inside your jacket pocket while riding.

---

## 5. Post Notifications (`android.permission.POST_NOTIFICATIONS`)

- **Why it is needed**: Required on Android 13+ to display the ongoing notification that shows Cadence voice status and allows immediate shutdown.
