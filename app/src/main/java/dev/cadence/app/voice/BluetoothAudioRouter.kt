package dev.cadence.app.voice

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BluetoothAudioRouter {
    private const val TAG = "BluetoothAudioRouter"
    private const val PREFS_NAME = "cadence_prefs"
    private const val KEY_BLUETOOTH_MIC = "bluetooth_mic_enabled"

    private val _isBluetoothHeadsetConnected = MutableStateFlow(false)
    val isBluetoothHeadsetConnected: StateFlow<Boolean> = _isBluetoothHeadsetConnected.asStateFlow()

    private val _isScoActive = MutableStateFlow(false)
    val isScoActive: StateFlow<Boolean> = _isScoActive.asStateFlow()

    private val _activeDeviceName = MutableStateFlow<String?>(null)
    val activeDeviceName: StateFlow<String?> = _activeDeviceName.asStateFlow()

    private var deviceCallback: AudioDeviceCallback? = null
    private var isStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(context: Context) {
        if (isStarted) {
            checkConnectedDevices(context)
            return
        }
        isStarted = true
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        // Always ensure we start in MODE_NORMAL for pristine A2DP HD stereo music playback
        clearRoute(audioManager)

        deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                checkConnectedDevices(context)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                checkConnectedDevices(context)
            }
        }

        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
            Log.i(TAG, "AudioDeviceCallback registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register AudioDeviceCallback", e)
        }

        checkConnectedDevices(context)
    }

    fun isBluetoothMicPreferred(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BLUETOOTH_MIC, true)
    }

    fun setBluetoothMicPreferred(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BLUETOOTH_MIC, enabled).apply()
        checkConnectedDevices(context)
    }

    fun checkConnectedDevices(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val available = audioManager.availableCommunicationDevices
                val btDevice = available.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_HEARING_AID
                }
                _isBluetoothHeadsetConnected.value = btDevice != null
                _activeDeviceName.value = btDevice?.productName?.toString()
            } else {
                val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val hasBtInput = inputs.any {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                _isBluetoothHeadsetConnected.value = hasBtInput
                _activeDeviceName.value = if (hasBtInput) "Bluetooth Headset" else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query audio devices", e)
        }
    }

    /**
     * Dynamically acquires Bluetooth SCO / Communication device for the short voice command window.
     * Music is already paused by VoiceService, so this only routes voice while the user speaks.
     */
    fun acquireScoForCommand(context: Context, onReady: () -> Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: run {
            onReady()
            return
        }

        if (!isBluetoothMicPreferred(context)) {
            Log.d(TAG, "Bluetooth mic preferred is false -> staying on phone mic")
            onReady()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val available = audioManager.availableCommunicationDevices
                val btDevice = available.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_HEARING_AID
                }

                if (btDevice != null) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    val success = audioManager.setCommunicationDevice(btDevice)
                    _isScoActive.value = success
                    Log.i(TAG, "On-demand setCommunicationDevice(${btDevice.productName}) -> $success")
                    // Allow 350ms for hardware SCO channel handshake
                    mainHandler.postDelayed({ onReady() }, 350)
                    return
                }
            } else {
                val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val hasBtInput = inputs.any {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }

                if (hasBtInput) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                    _isScoActive.value = true
                    Log.i(TAG, "On-demand legacy startBluetoothSco activated")
                    mainHandler.postDelayed({ onReady() }, 400)
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed in acquireScoForCommand", e)
        }

        // Fallback: invoke onReady immediately on phone mic
        onReady()
    }

    /**
     * Releases Bluetooth SCO immediately when speech recognition finishes,
     * restoring AudioManager.MODE_NORMAL so music resumes in 100% pristine A2DP HD Stereo.
     */
    fun releaseSco(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        clearRoute(audioManager)
        Log.i(TAG, "On-demand SCO released -> restored MODE_NORMAL for A2DP music")
    }

    private fun clearRoute(audioManager: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear route", e)
        }
        _isScoActive.value = false
    }

    fun stop(context: Context) {
        if (!isStarted) return
        isStarted = false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        deviceCallback?.let {
            try {
                audioManager.unregisterAudioDeviceCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister AudioDeviceCallback", e)
            }
        }
        deviceCallback = null

        clearRoute(audioManager)
        _isBluetoothHeadsetConnected.value = false
        _activeDeviceName.value = null
        Log.i(TAG, "BluetoothAudioRouter stopped")
    }
}
