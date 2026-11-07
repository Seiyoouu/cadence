package dev.cadence.app.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            Log.i(TAG, "Opening deep link for videoId=$videoId on targetPackage=$targetPackage: $uri")
            appContext.startActivity(intent)
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
