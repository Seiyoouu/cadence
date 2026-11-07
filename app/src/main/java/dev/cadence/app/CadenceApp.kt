package dev.cadence.app

import android.app.Application
import dev.cadence.app.engine.SearchEngine
import dev.cadence.app.engine.TransportEngine

class CadenceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TransportEngine.init(this)
        SearchEngine.init(this)
    }
}
