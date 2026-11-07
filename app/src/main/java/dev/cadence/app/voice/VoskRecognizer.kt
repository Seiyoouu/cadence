package dev.cadence.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
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

class VoskRecognizer(
    private val context: Context,
    private val locale: String
) {
    companion object {
        private const val TAG = "VoskRecognizer"
        private const val SAMPLE_RATE = 16000.0f
        private const val TIMEOUT_MS = 5000L
    }

    private val _transcript = MutableStateFlow<String?>(null)
    val transcript: StateFlow<String?> = _transcript.asStateFlow()
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null

    fun start(onComplete: (String) -> Unit, onTimeout: () -> Unit) {
        try {
            val modelName = if (locale == "id") "id" else "en"
            val modelDir: File = ModelExtractor.extractIfNeeded(context, modelName)
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, SAMPLE_RATE)
            _transcript.value = null
            _isListening.value = true

            timeoutHandler = Handler(Looper.getMainLooper())
            timeoutRunnable = Runnable {
                Log.i(TAG, "STT timeout")
                stop()
                onTimeout()
            }
            timeoutHandler?.postDelayed(timeoutRunnable!!, TIMEOUT_MS)

            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(h: String?) {}
                override fun onResult(h: String?) {
                    val text = parseText(h)
                    if (text.isNotBlank()) {
                        _transcript.value = text
                        Log.i(TAG, "Transcript: $text")
                    }
                }
                override fun onFinalResult(h: String?) {
                    val text = parseText(h)
                    if (text.isNotBlank()) _transcript.value = text
                    stop()
                    onComplete(_transcript.value ?: "")
                }
                override fun onError(e: Exception?) {
                    Log.e(TAG, "STT error", e)
                    stop()
                    onTimeout()
                }
                override fun onTimeout() {
                    stop()
                    onTimeout()
                }
            })
            Log.i(TAG, "STT listening started (locale=$locale)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start STT", e)
            _isListening.value = false
        }
    }

    /**
     * Synchronous recognition directly on raw PCM 16kHz audio buffer (useful for WAV injection testing).
     */
    fun recognizePcm(audioData: ByteArray): String {
        return try {
            val modelName = if (locale == "id") "id" else "en"
            val modelDir: File = ModelExtractor.extractIfNeeded(context, modelName)
            val directModel = Model(modelDir.absolutePath)
            val directRecognizer = Recognizer(directModel, SAMPLE_RATE)
            directRecognizer.acceptWaveForm(audioData, audioData.size)
            val finalJson = directRecognizer.finalResult
            val resultText = parseText(finalJson)
            directRecognizer.close()
            directModel.close()
            Log.i(TAG, "Direct PCM recognition result: '$resultText'")
            resultText
        } catch (e: Exception) {
            Log.e(TAG, "recognizePcm failed", e)
            ""
        }
    }

    private fun parseText(json: String?): String {
        if (json == null) return ""
        return try {
            JSONObject(json).optString("text", "")
        } catch (e: Exception) { "" }
    }

    fun stop() {
        timeoutRunnable?.let { timeoutHandler?.removeCallbacks(it) }
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
        _isListening.value = false
    }
}
