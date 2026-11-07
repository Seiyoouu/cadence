package dev.cadence.app

import android.content.Intent
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.cadence.app.databinding.ActivityMainBinding
import dev.cadence.app.engine.PlaybackInfo
import dev.cadence.app.engine.SearchEngine
import dev.cadence.app.engine.TransportEngine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupListeners() {
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

    private fun updatePermissionStatus() {
        val isGranted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (isGranted) {
            binding.tvPermissionStatus.text = getString(R.string.permission_notification_granted)
            binding.btnPermissionNotification.isEnabled = false
        } else {
            binding.tvPermissionStatus.text = getString(R.string.permission_notification_missing)
            binding.btnPermissionNotification.isEnabled = true
        }
    }
}
