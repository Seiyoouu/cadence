package dev.cadence.app.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerEngine(
    private val context: Context,
    private val locale: String
) {
    companion object {
        private const val TAG = "SpeechRecognizerEngine"
    }

    private var recognizer: SpeechRecognizer? = null
    private var active = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onTimeout: () -> Unit
    ) {
        if (!isAvailable()) {
            Log.e(TAG, "SpeechRecognizer not available")
            onError("SpeechRecognizer not available")
            return
        }
        active = true
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.i(TAG, "Ready for speech")
                }
                override fun onBeginningOfSpeech() {
                    Log.i(TAG, "Beginning of speech")
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.i(TAG, "End of speech")
                }
                override fun onError(error: Int) {
                    if (!active) return
                    val msg = errorToString(error)
                    Log.e(TAG, "Recognizer error: $msg ($error)")
                    active = false
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> onTimeout()
                        else -> onError(msg)
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (!active) return
                    active = false
                    val texts = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val best = texts?.firstOrNull()?.trim() ?: ""
                    Log.i(TAG, "Result: $best")
                    if (best.isEmpty()) onTimeout() else onResult(best)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val texts = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val partial = texts?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        Log.d(TAG, "Partial: $partial")
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val langTag = when (locale.lowercase()) {
            "id", "in", "id-id", "in-id" -> "id-ID"
            else -> "en-US"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        active = false
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun destroy() {
        stop()
    }

    private fun errorToString(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        else -> "ERROR_UNKNOWN_$error"
    }
}
