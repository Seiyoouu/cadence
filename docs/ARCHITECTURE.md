# Cadence Architecture

## Overview

Cadence is a 100% rootless Android application designed to deliver an entirely hands-free music control experience for motorcycle riders using YouTube Music Morphe. The application is built on a modular four-layer architecture comprising the Transport Engine, Search Engine, Voice Layer, and Rider Mode.

---

## Layer 1: Transport Engine

The Transport Engine provides zero-latency rootless playback controls by hooking into Android's `MediaSessionManager` via `MediaNotificationListenerService` (`NotificationListenerService`).

- **Target Package**: `com.google.android.apps.youtube.music` (YouTube Music Morphe).
- **Core Controller**: `dev.cadence.app.engine.TransportEngine`.
- **Operations**:
  - `play()`, `pause()`, `togglePlayPause()`
  - `next()`, `previous()`
  - Real-time reactive state updates via Kotlin Coroutines `StateFlow<PlaybackInfo>` (Title, Artist, PlaybackState, ConnectionStatus).
- **Fallback**: Secondary hardware media key emulation via `AudioManager.dispatchMediaKeyEvent()` when controller callbacks are delayed or unbound.

---

## Layer 2: Search Engine

The Search Engine allows riders to request specific songs hands-free without relying on UI accessibility or active screen state (screen-off capable).

- **Architecture**: External Lightweight API + Deep Link auto-launch (bypassing the need for screen-on AccessibilityService UI clicking).
- **Primary Search API**: Piped API (`https://api.piped.private.coffee`)
  - Endpoint: `/search?q={query}&filter=music_songs`
  - Average Latency: ~0.57s - 0.95s
  - Reliability: 100% verified across popular and regional queries.
- **Secondary Fallback API**: Invidious API (`https://yt.chocolatemoo53.com`)
  - Endpoint: `/api/v1/search?q={query}&type=video`
  - Used automatically if Piped encounters network timeouts or rate limits.
- **Playback Execution**: Deep Link Intent (`android.intent.action.VIEW`) directed to `https://music.youtube.com/watch?v={videoId}` targeting `com.google.android.apps.youtube.music`.
- **Pocket / Screen-Off Mode**: Intent launches and playback starts even when the device screen is off, satisfying motorcycle rider ergonomic constraints.

---

## Layer 3: Voice Layer

The Voice Layer provides a hybrid speech architecture combining lightweight offline wake word detection with native Android speech recognition:

- **Wake Word Detection**: `dev.cadence.app.voice.WakeWordDetector` runs continuously via the bundled lightweight VOSK English model (`app/src/main/assets/model-en/`) with constrained grammar for low power consumption:
  - Recognizes: `"Hey Cade"`, `"Hi Cade"`, `"Ok Cade"`, `"Cadence"`, `"Hai Cade"`, `"Halo Cade"`.
- **Command Speech-to-Text (STT)**: `dev.cadence.app.voice.SpeechRecognizerEngine` handles one-shot command recognition after wake detection using the native Android `SpeechRecognizer` (Google speech service with offline pack support). This provides native, high-accuracy Bahasa Indonesia (`id-ID`) and English (`en-US`) transcription without extra model bloat.
- **Background Persistence**: `dev.cadence.app.service.VoiceService` operates as a `ForegroundService` with `connectedDevice|microphone` types to maintain wake word listening and Bluetooth triggers while screen is off.
- **Command Parsing & Dispatch**: `dev.cadence.app.engine.CommandParser` parses commands into typed actions (`Next`, `Previous`, `Play`, `Pause`, `Search`, `Unknown`).
- **Sensory Feedback**: Dual-channel non-visual feedback for helmet riders:
  - `VibrationFeedback`: short pulse (transport controls), long pulse (search started), double pulse (unknown command or error).
  - `TtsFeedback`: optional spoken audio confirmation localized to system language.

---

## Layer 4: Rider Mode

Rider Mode manages the hands-free riding lifecycle and physical fallbacks:

- **Bluetooth Lifecycle Automation**: `dev.cadence.app.service.RiderModeService` monitors `BluetoothA2dp` and `BluetoothHeadset` connection states. Connecting helmet TWS or intercom automatically starts `VoiceService`; disconnecting stops it to save battery.
- **Physical Media Button Fallback**: `dev.cadence.app.service.MediaButtonReceiver` listens for hardware TWS button clicks (`KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`, `KEYCODE_MEDIA_PLAY_PAUSE`), enabling seamless control even in heavy helmet wind noise.
- **User Preferences**: Persistent toggles in `MainActivity` for Bluetooth auto-start, vibration feedback, and TTS spoken feedback.

---

## Offline Models

Cadence bundles the lightweight VOSK English US acoustic model (`vosk-model-small-en-us-0.15`) under `app/src/main/assets/model-en/` specifically for zero-latency, offline wake word listening. One-shot command recognition uses Android's native SpeechRecognizer, keeping total APK size at ~99MB.
