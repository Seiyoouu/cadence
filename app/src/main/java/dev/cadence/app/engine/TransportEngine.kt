package dev.cadence.app.engine

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackInfo(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
    val playbackState: Int = PlaybackState.STATE_NONE
)

object TransportEngine {
    private const val TAG = "TransportEngine"
    const val TARGET_PACKAGE = "com.google.android.apps.youtube.music"

    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfo: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()

    @Volatile
    private var activeController: MediaController? = null

    private var appContext: Context? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackInfo(state, activeController?.metadata)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updatePlaybackInfo(activeController?.playbackState, metadata)
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "MediaSession destroyed for $TARGET_PACKAGE")
            detachController()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    fun attachController(controller: MediaController) {
        if (controller.packageName != TARGET_PACKAGE) {
            Log.d(TAG, "Ignoring non-target package session: ${controller.packageName}")
            return
        }

        if (activeController?.sessionToken == controller.sessionToken) {
            // Already attached
            updatePlaybackInfo(controller.playbackState, controller.metadata)
            return
        }

        detachController()

        Log.i(TAG, "Attaching MediaController for $TARGET_PACKAGE")
        activeController = controller
        controller.registerCallback(controllerCallback)
        updatePlaybackInfo(controller.playbackState, controller.metadata)
    }

    @Synchronized
    fun detachController() {
        activeController?.let {
            try {
                it.unregisterCallback(controllerCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering controller callback", e)
            }
        }
        activeController = null
        _playbackInfo.value = _playbackInfo.value.copy(
            isConnected = false,
            isPlaying = false,
            playbackState = PlaybackState.STATE_NONE
        )
    }

    private fun updatePlaybackInfo(state: PlaybackState?, metadata: MediaMetadata?) {
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.description?.title?.toString()
            ?: _playbackInfo.value.title

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.description?.subtitle?.toString()
            ?: _playbackInfo.value.artist

        _playbackInfo.value = PlaybackInfo(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            isConnected = activeController != null,
            playbackState = state?.state ?: PlaybackState.STATE_NONE
        )
        Log.d(TAG, "Updated playback info: title='$title', artist='$artist', isPlaying=$isPlaying")
    }

    fun play() {
        val controller = activeController
        if (controller != null) {
            Log.i(TAG, "Executing play via MediaController")
            controller.transportControls.play()
        } else {
            Log.w(TAG, "MediaController is null. Fallback to dispatchMediaKeyEvent(PLAY)")
            dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    fun pause() {
        val controller = activeController
        if (controller != null) {
            Log.i(TAG, "Executing pause via MediaController")
            controller.transportControls.pause()
        } else {
            Log.w(TAG, "MediaController is null. Fallback to dispatchMediaKeyEvent(PAUSE)")
            dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
        }
    }

    fun togglePlayPause() {
        val controller = activeController
        if (controller != null) {
            val isPlaying = _playbackInfo.value.isPlaying
            Log.i(TAG, "Toggling play/pause via MediaController (currently isPlaying=$isPlaying)")
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } else {
            Log.w(TAG, "MediaController is null. Fallback to dispatchMediaKeyEvent(PLAY_PAUSE)")
            dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    fun next() {
        val controller = activeController
        if (controller != null) {
            Log.i(TAG, "Executing next via MediaController")
            controller.transportControls.skipToNext()
        } else {
            Log.w(TAG, "MediaController is null. Fallback to dispatchMediaKeyEvent(NEXT)")
            dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun previous() {
        val controller = activeController
        if (controller != null) {
            Log.i(TAG, "Executing previous via MediaController")
            controller.transportControls.skipToPrevious()
        } else {
            Log.w(TAG, "MediaController is null. Fallback to dispatchMediaKeyEvent(PREVIOUS)")
            dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    private fun dispatchMediaKeyEvent(keyCode: Int) {
        val context = appContext ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}
