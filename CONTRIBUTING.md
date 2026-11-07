# Contributing to Cadence

Thank you for your interest in contributing to Cadence!

Cadence is an open-source, rootless Android application providing hands-free voice control for YouTube Music Morphe, designed specifically for motorcycle riders.

---

## 🛠️ Build Requirements

- **JDK**: Java 17 (e.g. Eclipse Temurin 17)
- **Android SDK**: `compileSdk 34`, `minSdk 26`
- **Gradle**: 8.7+ (wrapper included)

### Building Debug APK

```bash
./gradlew assembleDebug
```

The output APK will be placed at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📝 Commit Conventions

We strictly follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

- `feat:` A new feature
- `fix:` A bug fix
- `docs:` Documentation changes
- `chore:` Maintenance, dependency bumps, or toolchain changes
- `refactor:` Code changes that neither fix bugs nor add features
- `ci:` Changes to CI/CD workflows and scripts
- `test:` Adding or updating tests

Examples:
- `feat: add bilingual wake word support for vosk`
- `fix: handle accessibility timeout on slow devices`
- `docs: update troubleshooting guide for MIUI background killer`

---

## 🌐 Bilingual Requirements

Cadence is strictly bilingual (English & Bahasa Indonesia):

1. **UI Strings**:
   - Every user-facing UI text must reside in `res/values/strings.xml` (English fallback) and `res/values-id/strings.xml` (Bahasa Indonesia).
   - No hardcoded strings in layouts (`.xml`) or Kotlin code.
2. **Voice Commands**:
   - Voice recognition commands must parse both languages simultaneously.
   - Any new command must support English and Indonesian variants in `CommandParser`.

---

## 🚀 Submitting Pull Requests

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Verify that the project builds cleanly without errors:
   ```bash
   ./gradlew assembleDebug
   ```
3. Commit your changes following Conventional Commits.
4. Push to your branch and submit a Pull Request targeting `main`.
5. Ensure all CI checks pass.
