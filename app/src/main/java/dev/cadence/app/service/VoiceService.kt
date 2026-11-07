package dev.cadence.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.cadence.app.R
import dev.cadence.app.voice.VoskRecognizer
import dev.cadence.app.voice.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val CHANNEL_ID = "cadence_voice_v2"
        private const val NOTIF_ID = 1001

        private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
        val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

        private val _lastTranscript = MutableStateFlow<String?>(null)
        val lastTranscript: StateFlow<String?> = _lastTranscript.asStateFlow()
    }

    sealed class VoiceState {
        data object Idle : VoiceState()
        data object ListeningWake : VoiceState()
        data object ListeningCommand : VoiceState()
        data object Processing : VoiceState()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeDetector: WakeWordDetector? = null
    private var stt: VoskRecognizer? = null
    private var wakeJob: Job? = null
    private lateinit var locale: String

    override fun onCreate() {
        super.onCreate()
        locale = Locale.getDefault().language
        Log.i(TAG, "VoiceService created with locale=$locale")
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        startWakeListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("test_transcript")?.let { transcript ->
            Log.i(TAG, "Test transcript injected: $transcript")
            _lastTranscript.value = transcript
        }
        intent?.getBooleanExtra("test_wake", false)?.let { testWake ->
            if (testWake) {
                Log.i(TAG, "Test wake triggered")
                stopWakeListening()
                startStt()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAll()
        scope.cancel()
        Log.i(TAG, "VoiceService destroyed")
        super.onDestroy()
    }

    private fun startWakeListening() {
        _voiceState.value = VoiceState.ListeningWake
        wakeDetector = WakeWordDetector(this, locale)
        wakeDetector?.start()
        wakeJob?.cancel()
        wakeJob = scope.launch {
            wakeDetector?.detected?.collect { detected ->
                if (detected) {
                    Log.i(TAG, "Wake word detected -> transitioning to STT")
                    stopWakeListening()
                    startStt()
                }
            }
        }
    }

    private fun stopWakeListening() {
        wakeJob?.cancel()
        wakeJob = null
        wakeDetector?.stop()
        wakeDetector = null
    }

    private fun startStt() {
        _voiceState.value = VoiceState.ListeningCommand
        stt = VoskRecognizer(this, locale)
        stt?.start(
            onComplete = { transcript ->
                Log.i(TAG, "STT complete: '$transcript'")
                _lastTranscript.value = transcript
                _voiceState.value = VoiceState.Processing
                stopStt()
                Handler(Looper.getMainLooper()).postDelayed({
                    startWakeListening()
                }, 500)
            },
            onTimeout = {
                Log.i(TAG, "STT timeout")
                stopStt()
                startWakeListening()
            }
        )
    }

    private fun stopStt() {
        stt?.stop()
        stt = null
    }

    private fun stopAll() {
        stopWakeListening()
        stopStt()
        _voiceState.value = VoiceState.Idle
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.voice_service_title))
            .setContentText(getString(R.string.voice_service_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
}
