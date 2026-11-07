# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-08

### Added
- Voice control dengan wake word "hai cade" / "hey cade" via lightweight VOSK engine.
- Bilingual command recognition (ID + EN) via Android SpeechRecognizer (`SpeechRecognizerEngine`).
- Search & play lagu otomatis via Piped & Invidious API + deep link auto-play.
- Bluetooth auto-start & stop lifecycle (Rider Mode via `RiderModeService`).
- TWS physical media button fallback (`MediaButtonReceiver`).
- Multi-slide interactive Onboarding flow (`OnboardingActivity`).
- Settings toggles (Bluetooth auto-start, TTS feedback, vibration feedback) with persistent preferences.
- Full bilingual UI localization (English & Bahasa Indonesia).
- About section dengan versi, lisensi GPL-3.0, dan repository link.
- Continuous screen-off execution support for motorcycle riding.

### Fixed
- Migrated command STT to native Android SpeechRecognizer for high-accuracy Indonesian and English transcription.
- Cleaned up experimental hardcodes and overfit regex patterns from earlier prototype phases.
- Resolved Android 14 foreground service start-from-background restrictions using `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`.

### Removed
- Removed redundant VOSK `model-id` asset, reducing overall APK size from 177MB to ~98MB.
- `CommandParser`: simultaneous recognition of English and Indonesian voice commands (Next, Previous, Pause, Play, Search, Unknown).
- Multi-channel sensory feedback: `VibrationFeedback` (short, long, double taps) and `TtsFeedback` (locale-aware Text-to-Speech).
- Persistent TTS feedback toggle setting in `MainActivity` with `SharedPreferences` persistence.
- Verified voice-controlled playback switching and search deep link launch with screen off.
- 43 unit tests passing in `CommandParserTest` covering edge cases, fillers, and bilingual speech patterns.
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
