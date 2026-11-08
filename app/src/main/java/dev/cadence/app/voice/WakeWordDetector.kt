package dev.cadence.app.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }

    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    private var speechService: SpeechService? = null

    // Strictly compound wake phrases to prevent random false triggers during casual speech
    private val wakeWords = listOf(
        "hai kadi", "halo kadi", "hey kadi", "oke kadi", "hei kadi",
        "hai kade", "halo kade", "hey kade", "oke kade", "hei kade", "hay kade",
        "hai cade", "halo cade", "hey cade", "ok cade", "hei cade", "hi cade",
        "hai cadence", "halo cadence", "hey cadence", "hi cadence",
        "hai musik", "halo musik", "hey music", "hai music",
        "hai bro", "halo bro", "hey bro", "ok bro",
        "cadence"
    )

    private val WAKE_REGEX = Regex(
        "\\b(hai|halo|hei|hey|oke|ok)\\s+(kadi|kade|cade|cadence|musik|music|bro)\\b|\\b(cadence)\\b",
        RegexOption.IGNORE_CASE
    )

    fun start() {
        try {
            val model = VoskModelHolder.getModel(context)
            val grammarJson = wakeWords.joinToString(",") { "\"$it\"" } + ",\"[unk]\""
            val recognizer = Recognizer(model, SAMPLE_RATE, "[$grammarJson]")
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(h: String?) { h?.let { checkWakeWord(it) } }
                override fun onResult(h: String?) { h?.let { checkWakeWord(it) } }
                override fun onFinalResult(h: String?) {}
                override fun onError(e: Exception?) { Log.e(TAG, "Wake error", e) }
                override fun onTimeout() {}
            })
            Log.i(TAG, "Wake word detector started with memory-cached model (locale=$locale)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word detector", e)
        }
    }

    private fun checkWakeWord(json: String) {
        val extracted = extractTextFromJson(json)
        if (extracted.isBlank()) return

        if (WAKE_REGEX.containsMatchIn(extracted)) {
            _detected.value = true
            Log.i(TAG, "Wake word detected via regex match: '$extracted'")
        }
    }

    private fun extractTextFromJson(json: String): String {
        return try {
            val lower = json.lowercase()
            if (lower.contains("\"partial\"")) {
                lower.substringAfter("\"partial\" : \"", "")
                    .substringAfter("\"partial\" :\"", "")
                    .substringBefore("\"")
            } else if (lower.contains("\"text\"")) {
                lower.substringAfter("\"text\" : \"", "")
                    .substringAfter("\"text\" :\"", "")
                    .substringBefore("\"")
            } else {
                lower
            }
        } catch (e: Exception) {
            json.lowercase()
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
