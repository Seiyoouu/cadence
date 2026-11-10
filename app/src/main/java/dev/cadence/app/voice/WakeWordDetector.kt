package dev.cadence.app.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.vosk.Recognizer
import kotlin.concurrent.thread

class WakeWordDetector(
    private val context: Context,
    private val locale: String
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val SAMPLE_RATE = 16000
        private const val DEBOUNCE_MS = 1500L
        private const val POCKET_GAIN = 2.5f // 2.5x amplification to compensate for jacket pocket fabric attenuation
    }

    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    @Volatile
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var recognizer: Recognizer? = null

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
        if (isRecording) return
        try {
            val model = VoskModelHolder.getModel(context)
            val grammarJson = acousticGrammarTokens.distinct().joinToString(",") { "\"$it\"" } + ",\"[unk]\""
            val rec = Recognizer(model, SAMPLE_RATE.toFloat(), "[$grammarJson]")
            recognizer = rec

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, (SAMPLE_RATE * 0.2f).toInt())

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                record.release()
                return
            }

            audioRecord = record
            isRecording = true

            recordingThread = thread(start = true, name = "Cadence-PocketWakeDetector") {
                try {
                    record.startRecording()
                    Log.i(TAG, "PocketWakeDetector started (MODE_NORMAL, 2.5x PocketBoost, bufferSize=$bufferSize)")
                    val buffer = ShortArray(bufferSize)

                    while (isRecording && !Thread.currentThread().isInterrupted) {
                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            // Apply Pocket-Mode Software Boost with soft limiter:
                            // Compensates for ~10-15dB acoustic loss through motorcycle jacket pocket
                            for (i in 0 until read) {
                                val boosted = (buffer[i] * POCKET_GAIN).toInt()
                                buffer[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }

                            if (rec.acceptWaveForm(buffer, read)) {
                                val result = rec.result
                                checkWakeWord(result)
                            } else {
                                val partial = rec.partialResult
                                checkWakeWord(partial)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in recording thread", e)
                } finally {
                    try {
                        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            record.stop()
                        }
                        record.release()
                    } catch (e: Exception) {
                        // Ignored
                    }
                    Log.i(TAG, "PocketWakeDetector recording thread terminated")
                }
            }
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
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored
        }
        audioRecord = null
        try {
            recognizer?.close()
        } catch (e: Exception) {
            // Ignored
        }
        recognizer = null
        Log.i(TAG, "Wake word detector stopped (model retained in cache)")
    }
}
