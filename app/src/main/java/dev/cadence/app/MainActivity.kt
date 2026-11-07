package dev.cadence.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.cadence.app.databinding.ActivityMainBinding
import dev.cadence.app.engine.PlaybackInfo
import dev.cadence.app.engine.SearchEngine
import dev.cadence.app.engine.TransportEngine
import dev.cadence.app.service.VoiceService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            binding.swVoiceControl.isChecked = true
            startVoiceService()
        } else {
            binding.swVoiceControl.isChecked = false
            Toast.makeText(this, R.string.mic_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cadence_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_completed", false)) {
            startActivity(Intent(this, dev.cadence.app.ui.OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TransportEngine.init(this)
        SearchEngine.init(this)

        // Handle runtime locale switching (e.g. from intent extra or settings)
        intent.getStringExtra("locale")?.let { langTag ->
            val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(langTag)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
        }

        setupListeners()
        observePlaybackState()
        observeSearchState()
        observeVoiceState()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("cadence_prefs", MODE_PRIVATE)
        binding.swTtsFeedback.isChecked = prefs.getBoolean("tts_enabled", false)
        binding.swTtsFeedback.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("tts_enabled", checked).apply()
        }

        binding.swAutoStartBluetooth.isChecked = prefs.getBoolean("auto_start_bluetooth", true)
        binding.swAutoStartBluetooth.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("auto_start_bluetooth", checked).apply()
        }

        binding.swVibrationFeedback.isChecked = prefs.getBoolean("vibration_enabled", true)
        binding.swVibrationFeedback.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("vibration_enabled", checked).apply()
        }

        binding.swVoiceControl.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startVoiceService()
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                stopVoiceService()
            }
        }

        binding.btnPlayPause.setOnClickListener {
            TransportEngine.togglePlayPause()
        }

        binding.btnNext.setOnClickListener {
            TransportEngine.next()
        }

        binding.btnPrevious.setOnClickListener {
            TransportEngine.previous()
        }

        binding.btnSearchPlay.setOnClickListener {
            val query = binding.etSearchQuery.text.toString().trim()
            if (query.isNotBlank()) {
                SearchEngine.searchAndPlay(query, lifecycleScope)
            }
        }

        binding.etSearchQuery.setOnEditorActionListener { _, _, _ ->
            val query = binding.etSearchQuery.text.toString().trim()
            if (query.isNotBlank()) {
                SearchEngine.searchAndPlay(query, lifecycleScope)
                true
            } else {
                false
            }
        }

        binding.btnPermissionNotification.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        binding.btnFixRestricted.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun observeSearchState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SearchEngine.searchState.collect { state ->
                    binding.tvSearchState.text = when (state) {
                        is SearchEngine.SearchState.Idle -> ""
                        is SearchEngine.SearchState.Searching ->
                            getString(R.string.search_state_searching, state.query)
                        is SearchEngine.SearchState.Playing ->
                            getString(R.string.search_state_playing, state.query)
                        is SearchEngine.SearchState.Failed ->
                            getString(R.string.search_state_failed, state.reason)
                    }
                }
            }
        }
    }

    private fun observePlaybackState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TransportEngine.playbackInfo.collect { info ->
                    renderPlaybackInfo(info)
                }
            }
        }
    }

    private fun renderPlaybackInfo(info: PlaybackInfo) {
        // Target connection status
        if (info.isConnected) {
            binding.tvTargetStatus.text = getString(R.string.target_connected)
        } else {
            binding.tvTargetStatus.text = getString(R.string.target_disconnected)
        }

        // Track title & artist
        binding.tvCurrentTrack.text = if (info.title.isNotBlank()) {
            info.title
        } else {
            getString(R.string.no_track_playing)
        }

        binding.tvCurrentArtist.text = if (info.artist.isNotBlank()) {
            info.artist
        } else {
            getString(R.string.unknown_artist)
        }

        // Playback state label & button
        when (info.playbackState) {
            PlaybackState.STATE_PLAYING -> {
                binding.tvPlaybackStatus.text = getString(R.string.status_playing)
                binding.btnPlayPause.text = getString(R.string.btn_pause)
            }
            PlaybackState.STATE_PAUSED -> {
                binding.tvPlaybackStatus.text = getString(R.string.status_paused)
                binding.btnPlayPause.text = getString(R.string.btn_play)
            }
            PlaybackState.STATE_STOPPED -> {
                binding.tvPlaybackStatus.text = getString(R.string.status_stopped)
                binding.btnPlayPause.text = getString(R.string.btn_play)
            }
            else -> {
                binding.tvPlaybackStatus.text = getString(R.string.status_unknown)
                binding.btnPlayPause.text = getString(R.string.btn_play)
            }
        }
    }

    private fun observeVoiceState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                VoiceService.voiceState.collect { state ->
                    binding.tvVoiceState.text = when (state) {
                        is VoiceService.VoiceState.Idle -> getString(R.string.voice_state_idle)
                        is VoiceService.VoiceState.ListeningWake -> getString(R.string.voice_state_listening_wake)
                        is VoiceService.VoiceState.ListeningCommand -> getString(R.string.voice_state_listening_command)
                        is VoiceService.VoiceState.Processing -> getString(R.string.voice_state_processing)
                    }
                    if (state !is VoiceService.VoiceState.Idle && !binding.swVoiceControl.isChecked) {
                        binding.swVoiceControl.isChecked = true
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                VoiceService.lastTranscript.collect { t ->
                    binding.tvLastTranscript.text = t ?: getString(R.string.transcript_placeholder)
                }
            }
        }
    }

    private fun startVoiceService() {
        val intent = Intent(this, VoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVoiceService() {
        stopService(Intent(this, VoiceService::class.java))
    }

    private fun updatePermissionStatus() {
        val isGranted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (isGranted) {
            binding.tvPermissionStatus.text = getString(R.string.permission_notification_granted)
            binding.btnPermissionNotification.isEnabled = false
            binding.btnFixRestricted.visibility = View.GONE
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                binding.btnFixRestricted.visibility = View.VISIBLE
                binding.tvPermissionStatus.text = getString(R.string.restricted_instructions)
            } else {
                binding.btnFixRestricted.visibility = View.GONE
                binding.tvPermissionStatus.text = getString(R.string.permission_notification_missing)
            }
            binding.btnPermissionNotification.isEnabled = true
        }
    }
}
