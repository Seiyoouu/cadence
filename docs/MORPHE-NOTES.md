# Technical Notes: YouTube Music Morphe Integration

## Target Package

Cadence interacts strictly with the following package:
```text
com.google.android.apps.youtube.music
```

> ⚠️ **CRITICAL WARNING**: Do NOT use `app.morphe.android.apps.youtube.music`. That is not the primary package maintained by Morphe patches and will break MediaSession hooks and accessibility traversal.

---

## Google Authentication & MicroG-RE

- YouTube Music Morphe is a patched, re-signed APK.
- Due to signature modification, standard Google Play Services authentication does not operate directly; Google login requires MicroG-RE / GmsCore companion.
- **Cadence does not and must not attempt to authenticate into Google accounts**. Cadence only interfaces with the running `MediaSession` and accessibility view hierarchy of YouTube Music.

---

## Official Sources

Always source Morphe patches and binaries exclusively from the official channels:
- Website: [https://morphe.software](https://morphe.software)
- Source & Patches: [https://github.com/MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)

> ⚠️ **SECURITY WARNING**: Never download Morphe APKs or patches from unofficial, third-party sites or unverified repositories.
