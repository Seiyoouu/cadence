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

    private val _isBluetoothMicActive = MutableStateFlow(false)
    val isBluetoothMicActive: StateFlow<Boolean> = _isBluetoothMicActive.asStateFlow()

    private val _activeDeviceName = MutableStateFlow<String?>(null)
    val activeDeviceName: StateFlow<String?> = _activeDeviceName.asStateFlow()

    private var deviceCallback: AudioDeviceCallback? = null
    private var isStarted = false

    fun start(context: Context) {
        if (isStarted) {
            updateRouting(context)
            return
        }
        isStarted = true
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                Log.d(TAG, "Audio devices added: ${addedDevices?.map { it.type }}")
                updateRouting(context)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                Log.d(TAG, "Audio devices removed: ${removedDevices?.map { it.type }}")
                updateRouting(context)
            }
        }

        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
            Log.i(TAG, "AudioDeviceCallback registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register AudioDeviceCallback", e)
        }

        updateRouting(context)
    }

    fun isBluetoothMicPreferred(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BLUETOOTH_MIC, true)
    }

    fun setBluetoothMicPreferred(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BLUETOOTH_MIC, enabled).apply()
        updateRouting(context)
    }

    fun updateRouting(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val preferred = isBluetoothMicPreferred(context)

        if (!preferred) {
            clearRoute(audioManager)
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
                    _isBluetoothMicActive.value = success
                    _activeDeviceName.value = btDevice.productName.toString()
                    Log.i(TAG, "setCommunicationDevice(${btDevice.productName}, type=${btDevice.type}) -> $success")
                } else {
                    clearRoute(audioManager)
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
                    _isBluetoothMicActive.value = true
                    _activeDeviceName.value = "Bluetooth Headset"
                    Log.i(TAG, "Legacy startBluetoothSco activated")
                } else {
                    clearRoute(audioManager)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed in updateRouting", e)
            clearRoute(audioManager)
        }
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
        _isBluetoothMicActive.value = false
        _activeDeviceName.value = null
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
        Log.i(TAG, "BluetoothAudioRouter stopped")
    }
}
