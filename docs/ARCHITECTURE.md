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

The Voice Layer provides completely offline, bilingual voice control via VOSK Speech Recognition engine:

- **Offline Models**: Dual-model bundling inside assets (`model-en` and `model-id`), uncompressed and extracted to internal storage (`filesDir/vosk/`) on first run.
- **Wake Word Detection**: `dev.cadence.app.voice.WakeWordDetector` uses acoustic models with targeted vocabulary grammar constraints for high precision and minimal battery consumption.
  - Supports English: `"hey cade"`, `"hi cade"`, `"ok cade"`, `"cadence"`.
  - Supports Indonesian: `"hai cade"`, `"hei cade"`, `"halo cade"`, `"oke cade"`.
- **Speech-to-Text (STT)**: `dev.cadence.app.voice.VoskRecognizer` performs full offline phrase recognition once the wake word triggers.
- **Background Persistence**: `dev.cadence.app.service.VoiceService` operates as a high-priority `ForegroundService` with `foregroundServiceType="microphone"`, keeping voice detection active while riding.
- **Command Parsing & Dispatch**: `dev.cadence.app.engine.CommandParser` processes recognized transcripts using bilingual rule sets with filler-stripping, routing to `TransportEngine` and `SearchEngine`.
- **Sensory Feedback**: Dual-channel non-visual feedback for helmet/motorcycle riders:
  - `VibrationFeedback`: short pulse (transport controls), long pulse (search started), double pulse (unknown command or error).
  - `TtsFeedback`: optional spoken audio confirmation ("Playing [title]", "Song not found", "Command not recognized") localized according to device language.
- **Reactive State Flow**: Exposes `VoiceState` (`Idle`, `ListeningWake`, `ListeningCommand`, `Processing`) and `lastTranscript` via Kotlin `StateFlow`.

---

## Layer 4: Rider Mode

<!-- Implementation details to follow in Phase 6 -->

---

## VOSK Models

Cadence bundles two lightweight, offline VOSK speech recognition models directly within the APK under `app/src/main/assets/`:
- `model-id/`: Indonesian acoustic model (`vosk-model-small-id-0.22`)
- `model-en/`: English (US) acoustic model (`vosk-model-small-en-us-0.15`)

Both models are obtained from the official Alpha Cephei repository at [alphacephei.com/vosk/models](https://alphacephei.com/vosk/models). Bundling both models enables zero-setup offline operation for motorcycle riders straight after installation.
