package dev.cadence.app.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SearchEngine {
    private const val TAG = "SearchEngine"
    private lateinit var appContext: Context

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    sealed class SearchState {
        data object Idle : SearchState()
        data class Searching(val query: String) : SearchState()
        data class Playing(val query: String) : SearchState()
        data class Failed(val query: String, val reason: String) : SearchState()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun searchAndPlay(query: String, scope: CoroutineScope) {
        _searchState.value = SearchState.Searching(query)
        scope.launch {
            val result = YtSearchApi.searchFirst(query)
            if (result == null) {
                Log.w(TAG, "Search returned no video ID for '$query'")
                _searchState.value = SearchState.Failed(query, "Song not found")
                return@launch
            }
            playViaDeepLink(result.videoId, result.title)
        }
    }

    private fun playViaDeepLink(videoId: String, title: String) {
        val targetPackage = MorphePackageResolver.getPreferredPackage(appContext)
        val uri = Uri.parse("https://music.youtube.com/watch?v=$videoId")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(targetPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // Wake screen briefly if device was locked in rider's pocket
        try {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            val screenWake = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or android.os.PowerManager.ON_AFTER_RELEASE,
                "Cadence:SearchPlayWake"
            )
            screenWake?.acquire(2500L)
        } catch (e: Exception) {
            Log.w(TAG, "Screen wake failed", e)
        }

        try {
            Log.i(TAG, "Opening deep link for videoId=$videoId on targetPackage=$targetPackage: $uri")

            val creatorOptions = if (Build.VERSION.SDK_INT >= 34) {
                android.app.ActivityOptions.makeBasic().apply {
                    setPendingIntentCreatorBackgroundActivityStartMode(
                        android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
            } else {
                android.app.ActivityOptions.makeBasic()
            }

            val senderOptions = if (Build.VERSION.SDK_INT >= 34) {
                android.app.ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
            } else {
                android.app.ActivityOptions.makeBasic()
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                appContext,
                1002,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                creatorOptions.toBundle()
            )

            // 1. Direct startActivity
            try {
                appContext.startActivity(intent)
                Log.i(TAG, "Launched directly via startActivity")
            } catch (e: Exception) {
                Log.w(TAG, "startActivity failed, falling back to PendingIntent", e)
            }

            // 2. PendingIntent with Creator + Sender BAL allowances
            try {
                pendingIntent.send(appContext, 0, null, null, null, null, senderOptions.toBundle())
                Log.i(TAG, "Deep link sent via PendingIntent.send() with Creator+Sender BAL")
            } catch (e: Exception) {
                Log.w(TAG, "PendingIntent send failed", e)
            }

            // 3. Status Notification so rider can also tap to launch if phone is unlocked
            launchViaNotificationFullScreen(pendingIntent, title)

            _searchState.value = SearchState.Playing(title)

            // Trigger play if music session hasn't started after opening deep link
            Handler(Looper.getMainLooper()).postDelayed({
                if (!TransportEngine.playbackInfo.value.isPlaying) {
                    Log.d(TAG, "Ensuring playback started via TransportEngine")
                    TransportEngine.play()
                }
            }, 1500)
        } catch (e: Exception) {
            Log.e(TAG, "Deep link launch failed", e)
            _searchState.value = SearchState.Failed(title, "Gagal buka YT Music")
        }
    }

    private fun launchViaNotificationFullScreen(pendingIntent: android.app.PendingIntent, title: String) {
        try {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val channelId = "cadence_play_launch"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Cadence Music Launcher",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Music launcher notifications"
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }

            val notif = androidx.core.app.NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(dev.cadence.app.R.mipmap.ic_launcher)
                .setContentTitle("Memutar Musik")
                .setContentText(title)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setTimeoutAfter(3000)
                .build()

            val notifId = 8888
            nm.notify(notifId, notif)
            Log.i(TAG, "High-priority FullScreenIntent notification dispatched for '$title'")

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    nm.cancel(notifId)
                } catch (e: Exception) {
                    // Ignored
                }
            }, 3000)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via Notification FullScreenIntent", e)
        }
    }

    private fun fallbackToIntentSearch(query: String) {
        val targetPackage = MorphePackageResolver.getPreferredPackage(appContext)
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            setPackage(targetPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
            _searchState.value = SearchState.Playing(query)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback intent search failed for '$query'", e)
            _searchState.value = SearchState.Failed(query, "Pencarian gagal")
        }
    }
}
