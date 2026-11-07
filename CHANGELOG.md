# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Phase 4: Voice Layer implementation with offline VOSK Speech Recognition and Wake Word detection.
- Bundled dual VOSK models (`model-en` and `model-id`) with automated zero-compression asset extraction on first run.
- Bilingual wake word detector supporting short triggers ("hey cade", "hai cade", "halo cade", "cadence").
- `VoiceService` Foreground Service with persistent microphone notification and state flow management.
- Integration test harness `TestSttActivity` for deterministic WAV audio injection verification.
- UI voice toggle switch, real-time voice state indicator, and live speech transcript box in `MainActivity`.
- Full bilingual resource strings for Voice Control across English and Indonesian.
- Phase 3: Search Engine implementation with lightweight Piped/Invidious API and deep link auto-play.
- Hands-free song search capability supporting pocket / screen-off operation without AccessibilityService overhead.
- Dual-instance API resilience: primary `api.piped.private.coffee` with fallback to `yt.chocolatemoo53.com`.
- Full UI search query input and reactive search status feedback (`Idle`, `Searching`, `Playing`, `Failed`).
- Bilingual localization for search controls and statuses across English and Bahasa Indonesia.
- Phase 2: Transport Engine implementation with zero-latency `MediaNotificationListener` hook.
- Real-time `StateFlow` playback updates (`TransportEngine.playbackInfo`) reflecting track title, artist, and playback status.
- UI bilingual controls (`values/`, `values-id/`, and `values-in/`) for Play, Pause, Next, and Previous.
- Full end-to-end computer-use verification with YouTube Music Morphe v9.30.53.
- Project scaffolding (Phase 1).
