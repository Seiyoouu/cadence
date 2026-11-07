package dev.cadence.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.cadence.app.R
import dev.cadence.app.engine.CommandParser
import dev.cadence.app.engine.SearchEngine
import dev.cadence.app.engine.TransportEngine
import dev.cadence.app.feedback.TtsFeedback
import dev.cadence.app.feedback.VibrationFeedback
import dev.cadence.app.voice.SpeechRecognizerEngine
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
    private var stt: SpeechRecognizerEngine? = null
    private var wakeJob: Job? = null
    private lateinit var locale: String
    private var tts: TtsFeedback? = null

    override fun onCreate() {
        super.onCreate()
        val rawLocale = Locale.getDefault().language.lowercase()
        val isIndonesian = rawLocale in listOf("id", "in", "id-id", "in-id")
        locale = if (isIndonesian) "id" else "en"
        Log.i(TAG, "VoiceService created with rawLocale=$rawLocale resolved to locale=$locale")
        createNotificationChannel()

        val notif = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // If started from background (e.g. Bluetooth broadcast), start as CONNECTED_DEVICE
                // to comply with Android 14 while-in-use background microphone restrictions.
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback startForeground", e)
            startForeground(NOTIF_ID, notif)
        }

        tts = TtsFeedback(this)
        startWakeListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("test_transcript")?.let { transcript ->
            Log.i(TAG, "Test transcript injected: $transcript")
            _lastTranscript.value = transcript
            _voiceState.value = VoiceState.Processing
            handleCommand(transcript)
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
        tts?.destroy()
        tts = null
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
        stt = SpeechRecognizerEngine(this, locale)
        if (!stt!!.isAvailable()) {
            Log.e(TAG, "SpeechRecognizer not available on this device")
            _voiceState.value = VoiceState.Idle
            if (isTtsEnabled()) {
                tts?.speak(
                    if (isIndonesian()) "Pengenalan suara tidak tersedia"
                    else "Speech recognition not available"
                )
            }
            startWakeListening()
            return
        }
        stt?.start(
            onResult = { transcript ->
                Log.i(TAG, "STT result: $transcript")
                _lastTranscript.value = transcript
                _voiceState.value = VoiceState.Processing
                stopStt()
                handleCommand(transcript)
            },
            onError = { err ->
                Log.e(TAG, "STT error: $err")
                stopStt()
                startWakeListening()
            },
            onTimeout = {
                Log.i(TAG, "STT timeout")
                stopStt()
                startWakeListening()
            }
        )
    }

    private fun isIndonesian(): Boolean {
        val raw = Locale.getDefault().language.lowercase()
        return raw in listOf("id", "in", "id-id", "in-id")
    }

    fun handleCommand(transcript: String) {
        val command = CommandParser.parse(transcript)
        Log.i(TAG, "Parsed command: $command (from '$transcript')")

        when (command) {
            is CommandParser.Command.Next -> {
                TransportEngine.next()
                VibrationFeedback.shortTap(this)
                if (isTtsEnabled()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val info = TransportEngine.playbackInfo.value
                        if (info.title.isNotBlank()) {
                            tts?.speak(info.title)
                        }
                    }, 800)
                }
            }
            is CommandParser.Command.Previous -> {
                TransportEngine.previous()
                VibrationFeedback.shortTap(this)
                if (isTtsEnabled()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val info = TransportEngine.playbackInfo.value
                        if (info.title.isNotBlank()) {
                            tts?.speak(info.title)
                        }
                    }, 800)
                }
            }
            is CommandParser.Command.Pause -> {
                TransportEngine.pause()
                VibrationFeedback.shortTap(this)
            }
            is CommandParser.Command.Play -> {
                TransportEngine.play()
                VibrationFeedback.shortTap(this)
            }
            is CommandParser.Command.TogglePlayPause -> {
                TransportEngine.togglePlayPause()
                VibrationFeedback.shortTap(this)
            }
            is CommandParser.Command.Search -> {
                VibrationFeedback.longTap(this)
                SearchEngine.searchAndPlay(command.query, scope)
                if (isTtsEnabled()) {
                    val msg = if (locale == "id" || locale == "in") "Memutar ${command.query}"
                    else "Playing ${command.query}"
                    tts?.speak(msg)
                }
            }
            is CommandParser.Command.Unknown -> {
                VibrationFeedback.doubleTap(this)
                if (isTtsEnabled()) {
                    val msg = if (locale == "id" || locale == "in") "Perintah tidak dikenali"
                    else "Command not recognized"
                    tts?.speak(msg)
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startWakeListening()
        }, 800)
    }

    private fun isTtsEnabled(): Boolean {
        return getSharedPreferences("cadence_prefs", MODE_PRIVATE)
            .getBoolean("tts_enabled", false)
    }

    private fun stopStt() {
        stt?.destroy()
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
