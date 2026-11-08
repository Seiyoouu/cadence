package dev.cadence.app.voice

import android.content.Context
import android.util.Log
import org.vosk.Model
import java.io.File

object VoskModelHolder {
    private const val TAG = "VoskModelHolder"

    @Volatile
    private var cachedModel: Model? = null

    @Synchronized
    fun getModel(context: Context): Model {
        cachedModel?.let { return it }

        Log.i(TAG, "Initializing and caching VOSK Kaldi model in memory...")
        val modelDir: File = ModelExtractor.extractIfNeeded(context.applicationContext, "en")
        val model = Model(modelDir.absolutePath)
        cachedModel = model
        Log.i(TAG, "VOSK Kaldi model loaded successfully into memory cache")
        return model
    }
}
