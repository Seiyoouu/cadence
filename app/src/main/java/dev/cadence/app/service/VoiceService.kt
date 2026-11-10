package dev.cadence.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
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
    private var wakeLock: PowerManager.WakeLock? = null
    private var wasPlayingBeforeWake = false

    override fun onCreate() {
        super.onCreate()
        val rawLocale = Locale.getDefault().language.lowercase()
        val isIndonesian = rawLocale in listOf("id", "in", "id-id", "in-id")
        locale = if (isIndonesian) "id" else "en"
        Log.i(TAG, "VoiceService created with rawLocale=$rawLocale resolved to locale=$locale")

        // Acquire partial wakelock to maintain audio capture while screen is locked in rider pocket
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cadence:VoiceServiceLock")?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12-hour safe auto-release
            }
            Log.i(TAG, "VoiceService partial WakeLock acquired")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock", e)
        }

        createNotificationChannel()

        val notif = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            // Ignored
        }
        wakeLock = null
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
                    wasPlayingBeforeWake = TransportEngine.playbackInfo.value.isPlaying
                    Log.i(TAG, "Wake word detected -> transitioning to STT (wasPlaying=$wasPlayingBeforeWake)")
                    VibrationFeedback.shortTap(this@VoiceService)
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
        val languageTag = resolveSttLanguage()
        stt = SpeechRecognizerEngine(this, languageTag)
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
        stt?.startWithCandidates(
            onReady = {
                VibrationFeedback.shortTap(this@VoiceService)
            },
            onResults = { candidates ->
                Log.i(TAG, "STT candidates: $candidates")
                val primary = candidates.firstOrNull() ?: ""
                _lastTranscript.value = primary
                _voiceState.value = VoiceState.Processing
                stopStt()
                handleCommandCandidates(candidates)
            },
            onError = { err ->
                Log.e(TAG, "STT error: $err")
                stopStt()
                if (wasPlayingBeforeWake && !TransportEngine.playbackInfo.value.isPlaying) {
                    Log.i(TAG, "Restoring playback after STT error")
                    TransportEngine.play()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    startWakeListening()
                }, 400)
            },
            onTimeout = {
                Log.i(TAG, "STT timeout")
                stopStt()
                if (wasPlayingBeforeWake && !TransportEngine.playbackInfo.value.isPlaying) {
                    Log.i(TAG, "Restoring playback after STT timeout")
                    TransportEngine.play()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    startWakeListening()
                }, 400)
            }
        )
    }

    private fun resolveSttLanguage(): String {
        val prefs = getSharedPreferences("cadence_prefs", Context.MODE_PRIVATE)
        val prefLang = prefs.getString("voice_language", "id-ID") ?: "id-ID"
        if (prefLang == "auto") {
            val raw = Locale.getDefault().language.lowercase()
            return if (raw in listOf("id", "in", "id-id", "in-id")) "id-ID" else "en-US"
        }
        return prefLang
    }

    private fun isIndonesian(): Boolean {
        val raw = Locale.getDefault().language.lowercase()
        return raw in listOf("id", "in", "id-id", "in-id")
    }

    private fun handleCommandCandidates(candidates: List<String>) {
        val primary = candidates.firstOrNull() ?: ""
        val command = CommandParser.parseCandidates(candidates)
        Log.i(TAG, "Parsed command: $command (primary='$primary', count=${candidates.size})")
        executeCommand(command, primary)
    }

    fun handleCommand(transcript: String) {
        val command = CommandParser.parse(transcript)
        Log.i(TAG, "Parsed command: $command (from '$transcript')")
        executeCommand(command, transcript)
    }

    private fun executeCommand(command: CommandParser.Command, transcript: String) {

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
                if (wasPlayingBeforeWake && !TransportEngine.playbackInfo.value.isPlaying) {
                    Log.i(TAG, "Restoring playback after unknown command")
                    TransportEngine.play()
                }
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
