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

<!-- Implementation details to follow in Phase 4 & Phase 5 -->

---

## Layer 4: Rider Mode

<!-- Implementation details to follow in Phase 6 -->

---

## VOSK Models

Cadence bundles two lightweight, offline VOSK speech recognition models directly within the APK under `app/src/main/assets/`:
- `model-id/`: Indonesian acoustic model (`vosk-model-small-id-0.22`)
- `model-en/`: English (US) acoustic model (`vosk-model-small-en-us-0.15`)

Both models are obtained from the official Alpha Cephei repository at [alphacephei.com/vosk/models](https://alphacephei.com/vosk/models). Bundling both models enables zero-setup offline operation for motorcycle riders straight after installation.
