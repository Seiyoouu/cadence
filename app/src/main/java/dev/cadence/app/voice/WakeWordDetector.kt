package dev.cadence.app.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

class WakeWordDetector(
    private val context: Context,
    private val locale: String
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val SAMPLE_RATE = 16000.0f
        private const val DEBOUNCE_MS = 1500L
    }

    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    private var speechService: SpeechService? = null

    // Strictly compound wake phrases with intentional greetings to prevent false triggers during casual speech and singing
    private val wakeWords = listOf(
        // Primary Cade variants
        "hey cade", "hi cade", "hai cade", "halo cade", "hei cade", "ok cade", "oke cade",

        // Indonesian phonetic Kadi variants (natural Indonesian pronunciation for helmet mics)
        "hai kadi", "halo kadi", "hey kadi", "hi kadi", "hei kadi", "oke kadi",

        // Kade variants
        "hai kade", "halo kade", "hey kade", "hi kade", "hei kade", "oke kade",

        // Brand variants
        "hey cadence", "hi cadence", "hai cadence", "halo cadence", "cadence"
    )

    // Component acoustic anchors allow Kaldi to resolve words under road rumble without falling into [unk]
    private val acousticGrammarTokens = listOf(
        "hey", "hai", "halo", "hei", "hi", "ok", "oke",
        "cade", "kadi", "kade", "cadence",
        "hey cade", "hi cade", "hai cade", "halo cade", "hei cade", "ok cade", "oke cade",
        "hey kadi", "hi kadi", "hai kadi", "halo kadi", "hei kadi", "oke kadi",
        "hey kade", "hi kade", "hai kade", "halo kade", "hei kade", "oke kade",
        "hey cadence", "hi cadence", "hai cadence", "halo cadence"
    )

    private val WAKE_REGEX = Regex(
        "\\b(hai|halo|hei|hey|hi|ok|oke)\\s+(cade|kadi|kade|cadence)\\b|\\b(cadence)\\b",
        RegexOption.IGNORE_CASE
    )

    private var lastDetectionTime = 0L

    fun start() {
        try {
            val model = VoskModelHolder.getModel(context)
            val grammarJson = acousticGrammarTokens.distinct().joinToString(",") { "\"$it\"" } + ",\"[unk]\""
            val recognizer = Recognizer(model, SAMPLE_RATE, "[$grammarJson]")
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(h: String?) { h?.let { checkWakeWord(it) } }
                override fun onResult(h: String?) { h?.let { checkWakeWord(it) } }
                override fun onFinalResult(h: String?) {}
                override fun onError(e: Exception?) { Log.e(TAG, "Wake error", e) }
                override fun onTimeout() {}
            })
            Log.i(TAG, "Wake word detector started with acoustic anchors (locale=$locale)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word detector", e)
        }
    }

    private fun checkWakeWord(json: String) {
        val now = System.currentTimeMillis()
        if (now - lastDetectionTime < DEBOUNCE_MS) return

        val extracted = extractTextFromJson(json)
        if (extracted.isBlank()) return

        // Verify with word-boundary regex to prevent partial phonetic collisions during singing
        if (WAKE_REGEX.containsMatchIn(extracted)) {
            lastDetectionTime = now
            _detected.value = true
            Log.i(TAG, "Wake word confirmed: '$extracted'")
        }
    }

    private fun extractTextFromJson(json: String): String {
        return try {
            val obj = JSONObject(json)
            val partial = obj.optString("partial")
            if (partial.isNotBlank()) return partial.trim().lowercase()
            val text = obj.optString("text")
            if (text.isNotBlank()) return text.trim().lowercase()
            ""
        } catch (e: Exception) {
            val lower = json.lowercase()
            if (lower.contains("\"partial\"")) {
                lower.substringAfter("\"partial\"", "").substringAfter("\"", "").substringBefore("\"")
            } else if (lower.contains("\"text\"")) {
                lower.substringAfter("\"text\"", "").substringAfter("\"", "").substringBefore("\"")
            } else {
                ""
            }
        }
    }

    fun resetDetection() { _detected.value = false }

    fun stop() {
        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down speechService", e)
        }
        speechService = null
        Log.i(TAG, "Wake word detector stopped (model retained in cache)")
    }
}
