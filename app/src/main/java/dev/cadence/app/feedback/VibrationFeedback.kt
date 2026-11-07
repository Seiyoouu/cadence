package dev.cadence.app.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationFeedback {

    private fun getVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun shortTap(context: Context) {
        vibrate(context, longArrayOf(0, 50))
    }

    fun longTap(context: Context) {
        vibrate(context, longArrayOf(0, 150))
    }

    fun doubleTap(context: Context) {
        vibrate(context, longArrayOf(0, 50, 100, 50))
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vib = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Silently ignore or log if vibrator is unavailable
        }
    }
}
