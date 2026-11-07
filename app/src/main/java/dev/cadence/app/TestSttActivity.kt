package dev.cadence.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import dev.cadence.app.voice.ModelExtractor
import dev.cadence.app.voice.VoskRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream

class TestSttActivity : Activity() {

    companion object {
        private const val TAG = "VoskRecognizer"
        private const val SAMPLE_RATE = 16000.0f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = "Testing VOSK STT..."
        }
        setContentView(tv)

        val wavPath = intent.getStringExtra("wav") ?: "/sdcard/Download/test_id.wav"
        val locale = intent.getStringExtra("locale") ?: "en"

        Log.i(TAG, "TestSttActivity started for wav=$wavPath, locale=$locale")

        CoroutineScope(Dispatchers.Main).launch {
            val transcript = withContext(Dispatchers.IO) {
                runRecognition(wavPath, locale)
            }
            Log.i(TAG, "Transcript: $transcript")
            tv.text = "WAV: $wavPath\nTranscript: $transcript"
        }
    }

    private fun runRecognition(wavPath: String, locale: String): String {
        return try {
            val file = File(wavPath)
            if (!file.exists()) {
                Log.e(TAG, "WAV file does not exist: $wavPath")
                return "File not found"
            }
            val modelName = if (locale == "id") "id" else "en"
            val modelDir = ModelExtractor.extractIfNeeded(this, modelName)
            val model = Model(modelDir.absolutePath)
            val recognizer = Recognizer(model, SAMPLE_RATE)

            val bytes = FileInputStream(file).use { it.readBytes() }
            // If WAV header is present (starts with RIFF), we can feed either whole or payload
            val audioPayload = if (bytes.size > 44 && String(bytes.sliceArray(0 until 4)) == "RIFF") {
                bytes.sliceArray(44 until bytes.size)
            } else {
                bytes
            }

            recognizer.acceptWaveForm(audioPayload, audioPayload.size)
            val finalResultJson = recognizer.finalResult
            Log.i(TAG, "Raw finalResult: $finalResultJson")
            val text = org.json.JSONObject(finalResultJson).optString("text", "")

            recognizer.close()
            model.close()
            text
        } catch (e: Exception) {
            Log.e(TAG, "runRecognition failed", e)
            "Error: ${e.message}"
        }
    }
}
