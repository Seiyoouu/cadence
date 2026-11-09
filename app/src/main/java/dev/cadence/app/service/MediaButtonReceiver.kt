package dev.cadence.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import dev.cadence.app.engine.TransportEngine

class MediaButtonReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MediaButtonReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra("test_search")) {
            val query = intent.getStringExtra("test_search") ?: ""
            Log.i(TAG, "Test search received in background: '$query'")
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
            dev.cadence.app.engine.SearchEngine.searchAndPlay(query, scope)
            return
        }

        @Suppress("DEPRECATION")
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        val keyCode = when {
            event != null && event.action == KeyEvent.ACTION_DOWN -> event.keyCode
            intent.hasExtra("key_code") -> intent.getIntExtra("key_code", -1)
            else -> -1
        }
        if (keyCode == -1) return
        Log.i(TAG, "Media button received: keyCode=$keyCode")

        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                Log.i(TAG, "MediaButton: NEXT triggered")
                TransportEngine.next()
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                Log.i(TAG, "MediaButton: PREVIOUS triggered")
                TransportEngine.previous()
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                Log.i(TAG, "MediaButton: PLAY_PAUSE triggered")
                TransportEngine.togglePlayPause()
            }
        }
    }
}
