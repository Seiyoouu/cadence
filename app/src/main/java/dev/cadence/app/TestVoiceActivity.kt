package dev.cadence.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import dev.cadence.app.engine.CommandParser
import dev.cadence.app.engine.SearchEngine
import dev.cadence.app.engine.TransportEngine
import dev.cadence.app.service.VoiceService
import dev.cadence.app.voice.ModelExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream

class TestVoiceActivity : Activity() {

    companion object {
        private const val TAG = "TestVoiceActivity"
        private const val SAMPLE_RATE = 16000.0f
    }

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tvStatus = TextView(this).apply {
            textSize = 15f
            setPadding(24, 24, 24, 24)
            text = "Testing Voice End-to-End..."
        }
        setContentView(tvStatus)

        TransportEngine.init(this)
        SearchEngine.init(this)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val rawWavPath = intent.getStringExtra("wav") ?: "/data/user/0/dev.cadence.app/files/t1.wav"
        val locale = intent.getStringExtra("locale") ?: "en"

        Log.i(TAG, "TestVoiceActivity started: wav=$rawWavPath, locale=$locale")

        CoroutineScope(Dispatchers.Main).launch {
            val transcript = withContext(Dispatchers.IO) {
                recognizeWav(rawWavPath, locale)
            }
            Log.i(TAG, "VoskRecognizer Transcript: $transcript")

            val command = CommandParser.parse(transcript)
            Log.i(TAG, "CommandParser Result: $command")

            // Forward to VoiceService to trigger complete pipeline (TTS, vibration, engines)
            val serviceIntent = Intent(this@TestVoiceActivity, VoiceService::class.java).apply {
                putExtra("test_transcript", transcript)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            tvStatus.text = "WAV: $rawWavPath\nTranscript: '$transcript'\nCommand: $command\nExecuted: OK"
        }
    }

    private fun recognizeWav(rawPath: String, locale: String): String {
        return try {
            var file = File(rawPath)
            if (!file.exists()) {
                val fallbackFile = File(filesDir, rawPath.substringAfterLast("/"))
                if (fallbackFile.exists()) {
                    file = fallbackFile
                } else {
                    Log.e(TAG, "File not found: $rawPath (also checked ${fallbackFile.absolutePath})")
                    return ""
                }
            }

            val rawLocale = locale.lowercase()
            val isIndonesian = rawLocale in listOf("id", "in", "id-id", "in-id")
            val modelName = if (isIndonesian) "id" else "en"
            val modelDir = ModelExtractor.extractIfNeeded(this, modelName)
            val model = Model(modelDir.absolutePath)
            val recognizer = Recognizer(model, SAMPLE_RATE)

            val bytes = FileInputStream(file).use { it.readBytes() }
            val audioPayload = if (bytes.size > 44 && String(bytes.sliceArray(0 until 4)) == "RIFF") {
                bytes.sliceArray(44 until bytes.size)
            } else {
                bytes
            }

            recognizer.acceptWaveForm(audioPayload, audioPayload.size)
            val finalJson = recognizer.finalResult
            Log.i(TAG, "Raw finalResult: $finalJson")
            val text = org.json.JSONObject(finalJson).optString("text", "")

            recognizer.close()
            model.close()
            text
        } catch (e: Exception) {
            Log.e(TAG, "recognizeWav failed", e)
            ""
        }
    }
}
