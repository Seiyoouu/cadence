package dev.cadence.app.feedback

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsFeedback(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsFeedback"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale.getDefault()
            val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            Log.i(TAG, "TTS initialized, ready=$ready, locale=$locale")
        } else {
            Log.e(TAG, "TTS init failed, status=$status")
        }
    }

    fun speak(text: String) {
        if (!ready) {
            Log.w(TAG, "TTS not ready, skipping: $text")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cadence_tts")
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
