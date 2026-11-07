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

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val wakeWords = listOf(
        "hey cade", "hi cade", "ok cade", "cade", "cadence",
        "hai cade", "hei cade", "halo cade", "oke cade",
        "hey cadence", "hi cadence", "halo cadence", "hai cadence",
        "hi candy", "hai candy", "hay cade", "hei kade", "hai kade",
        "hey kade", "hi kade", "candy"
    )

    fun start() {
        try {
            val rawLocale = locale.lowercase()
            val isIndonesian = rawLocale in listOf("id", "in", "id-id", "in-id")
            val modelName = if (isIndonesian) "id" else "en"
            val modelDir: File = ModelExtractor.extractIfNeeded(context, modelName)
            model = Model(modelDir.absolutePath)
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
            Log.i(TAG, "Wake word detector started (locale=$locale)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word detector", e)
        }
    }

    private fun checkWakeWord(json: String) {
        val lower = json.lowercase()
        for (ww in wakeWords) {
            if (lower.contains(ww)) {
                _detected.value = true
                Log.i(TAG, "Wake word detected: $ww")
                return
            }
        }
    }

    fun resetDetection() { _detected.value = false }

    fun stop() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        model?.close()
        model = null
        Log.i(TAG, "Wake word detector stopped")
    }
}
