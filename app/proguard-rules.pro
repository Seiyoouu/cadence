# Cadence Proguard Rules

# Keep Vosk JNI and model classes
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
