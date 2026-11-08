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
    }

    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    private var speechService: SpeechService? = null

    // Strictly compound wake phrases + acoustic phonetic variants
    private val wakeWords = listOf(
        // Cade & Cadence variants
        "hey cade", "hi cade", "hai cade", "halo cade", "hei cade", "ok cade", "oke cade",
        "hey cadence", "hi cadence", "hai cadence", "halo cadence", "cadence",

        // Phonetic Kaldi acoustic neighbors for 'Cade'
        "hey kate", "hi kate", "hai kate", "halo kate",
        "hey caddy", "hi caddy", "hai caddy", "halo caddy",
        "hey chad", "hi chad", "hai chad", "halo chad",
        "hey candy", "hi candy", "hai candy", "halo candy",

        // Kadi & Kade variants
        "hai kadi", "halo kadi", "hey kadi", "hi kadi", "oke kadi", "hei kadi",
        "hai kade", "halo kade", "hey kade", "hi kade", "oke kade", "hei kade", "hay kade",

        // Musik variants
        "hai musik", "halo musik", "hey music", "hi music", "hai music",

        // Bro variants
        "hai bro", "halo bro", "hey bro", "hi bro", "ok bro", "oke bro"
    )

    private val WAKE_REGEX = Regex(
        "\\b(hai|halo|hei|hey|hi|ok|oke)\\s+(kadi|kade|cade|cadence|kate|caddy|chad|candy|musik|music|bro)\\b|\\b(cadence)\\b",
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

        for (ww in wakeWords) {
            if (extracted.contains(ww)) {
                _detected.value = true
                Log.i(TAG, "Wake word detected via exact phrase: '$ww' in '$extracted'")
                return
            }
        }

        if (WAKE_REGEX.containsMatchIn(extracted)) {
            _detected.value = true
            Log.i(TAG, "Wake word detected via regex match: '$extracted'")
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
