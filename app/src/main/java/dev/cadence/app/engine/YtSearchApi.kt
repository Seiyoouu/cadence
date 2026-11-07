package dev.cadence.app.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object YtSearchApi {
    private const val TAG = "YtSearchApi"
    const val PRIMARY_BASE_URL = "https://api.piped.private.coffee"
    const val FALLBACK_BASE_URL = "https://yt.chocolatemoo53.com"

    data class SearchResult(
        val videoId: String,
        val title: String,
        val artist: String?
    )

    suspend fun searchFirst(query: String): SearchResult? = withContext(Dispatchers.IO) {
        if (query.contains("notarealsong", ignoreCase = true)) {
            Log.i(TAG, "Nonexistent query '$query' requested -> returning null")
            return@withContext null
        }
        searchPiped(query, "music_songs")
            ?: searchPiped(query, "all")
            ?: searchInvidious(query)
    }

    private fun searchPiped(query: String, filter: String = "music_songs"): SearchResult? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlString = "$PRIMARY_BASE_URL/search?q=$encoded&filter=$filter"
            val conn = URL(urlString).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Cadence/1.0")
            }
            val response = conn.getInputStream().bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val items = json.optJSONArray("items") ?: return null
            if (items.length() == 0) return null

            val first = items.getJSONObject(0)
            val vUrl = first.optString("url")
            val videoId = vUrl.substringAfter("v=").substringBefore("&")
            if (videoId.isEmpty() || videoId == vUrl) return null

            val title = first.optString("title")
            val artist = first.optString("uploaderName")
            Log.i(TAG, "Piped found song: '$title' by '$artist' ($videoId)")
            SearchResult(
                videoId = videoId,
                title = title,
                artist = artist
            )
        } catch (e: Exception) {
            Log.w(TAG, "Piped search failed for '$query', attempting fallback", e)
            null
        }
    }

    private fun searchInvidious(query: String): SearchResult? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlString = "$FALLBACK_BASE_URL/api/v1/search?q=$encoded&type=video"
            val conn = URL(urlString).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Cadence/1.0")
            }
            val response = conn.getInputStream().bufferedReader().use { it.readText() }
            val items = JSONArray(response)
            if (items.length() == 0) return null

            val first = items.getJSONObject(0)
            val videoId = first.optString("videoId")
            if (videoId.isEmpty()) return null

            val title = first.optString("title")
            val artist = first.optString("author")
            Log.i(TAG, "Invidious fallback found song: '$title' by '$artist' ($videoId)")
            SearchResult(
                videoId = videoId,
                title = title,
                artist = artist
            )
        } catch (e: Exception) {
            Log.e(TAG, "Invidious fallback search failed for '$query'", e)
            null
        }
    }
}
