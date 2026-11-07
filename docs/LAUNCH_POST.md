# I got tired of pulling over to skip songs on my motorcycle, so I built a voice-controlled YT Music app

Every motorcycle rider knows the frustration: you're cruising on the open road or stuck in commuter traffic, and a completely wrong song comes on. You either suffer through it, or pull over dangerously onto the shoulder, take off your riding gloves, unlock your phone in the bright glare, and fumble with tiny touch buttons.

I built **Cadence** to solve this once and for all. It's a 100% rootless, open-source Android app designed from the ground up for riders with helmet TWS earbuds or Bluetooth intercoms.

---

### 🏍️ What Cadence Does

With your phone locked deep inside your jacket pocket and your screen completely off:
- Say **"Hey Cade"** (or **"Hai Cade"**) into your helmet microphone.
- Speak your command: *"next"*, *"skip"*, *"lagu berikutnya"*, *"jeda"*, or search anything: *"play Bohemian Rhapsody"*, *"putar lagu Dewa 19 Kangen"*.
- YouTube Music instantly changes or plays the song in the background.

### ✨ Key Highlights:
1. **Screen-Off & Pocket Ready**: Full background execution via `MediaSession` and deep links. You never have to take your phone out of your pocket.
2. **True Bilingual Recognition**: Recognizes Bahasa Indonesia AND English commands simultaneously.
3. **Rider Mode (Bluetooth Automation)**: Automatically begins listening when your helmet TWS/intercom connects, and shuts down when you turn it off to preserve battery life.
4. **Physical Button Fallback**: When cruising at highway speeds with high wind noise, your TWS physical media buttons act as instant transport fallbacks.
5. **Helmet Haptic Feedback**: Distinct vibration pulses confirm commands through your pocket, with optional spoken voice feedback.
6. **100% Rootless**: No root, Magisk, or Xposed.

---

### 🔍 Honest Technical Limitations

- **Companion App**: Requires [YouTube Music Morphe](https://morphe.software) (`com.google.android.apps.youtube.music`).
- **Speech Engine**: Requires Google App / Speech Services for one-shot command recognition.
- **Microphone Quality**: Heavy wind noise at speeds >80 km/h without a chin-curtain wind deflector may reduce wake word sensitivity; physical TWS buttons act as the fallback.

---

### 📥 Download & Open Source

- **GitHub Release (v1.0.0 APK)**: https://github.com/Seiyoouu/cadence/releases/tag/v1.0.0
- **Source Code**: https://github.com/Seiyoouu/cadence (GPL-3.0)

I'd love to hear your feedback, ride reports, and feature suggestions! If you find it useful on your daily commute or road trips, please drop a ⭐ on GitHub and let me know how it performs with your helmet setup.

---

### 🎯 Target Communities for Launch
- **Reddit**: r/motorcycles, r/YouTubeMusic, r/Android, r/revancedapp
- **Telegram**: Morphe / ReVanced Community, Indonesian Bikers Community
- **Forums**: XDA Developers (Apps & Navigation section)
