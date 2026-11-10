package dev.cadence.app.service

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class RiderModeService : BroadcastReceiver() {

    companion object {
        private const val TAG = "RiderModeService"
        private const val PREFS = "cadence_prefs"
        private const val KEY_AUTO_START = "auto_start_bluetooth"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED &&
            action != BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
        ) return

        val state = intent.getIntExtra(
            BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED
        )
        @Suppress("DEPRECATION")
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        Log.i(TAG, "Bluetooth state changed: action=$action state=$state device=${device?.name ?: "unknown"}")

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val autoStart = prefs.getBoolean(KEY_AUTO_START, true)

        when (state) {
            BluetoothProfile.STATE_CONNECTED -> {
                dev.cadence.app.voice.BluetoothAudioRouter.checkConnectedDevices(context)
                if (autoStart) {
                    Log.i(TAG, "Bluetooth connected -> starting VoiceService")
                    startVoiceService(context)
                } else {
                    Log.i(TAG, "Bluetooth connected but auto_start_bluetooth is false")
                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                dev.cadence.app.voice.BluetoothAudioRouter.checkConnectedDevices(context)
                Log.i(TAG, "Bluetooth disconnected -> stopping VoiceService")
                stopVoiceService(context)
            }
        }
    }

    private fun startVoiceService(context: Context) {
        val intent = Intent(context, VoiceService::class.java).apply {
            putExtra("from_bluetooth", true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopVoiceService(context: Context) {
        context.stopService(Intent(context, VoiceService::class.java))
    }
}
