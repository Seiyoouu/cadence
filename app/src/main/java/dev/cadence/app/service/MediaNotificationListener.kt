package dev.cadence.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.cadence.app.engine.TransportEngine

class MediaNotificationListener : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private val componentName by lazy { ComponentName(this, MediaNotificationListener::class.java) }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active media sessions changed: count=${controllers?.size ?: 0}")
        inspectControllers(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MediaNotificationListener created")
        TransportEngine.init(this)
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected successfully")
        try {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val controllers = sessionManager?.getActiveSessions(componentName)
            inspectControllers(controllers)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting active sessions: Notification Access not granted?", e)
        }

        // Also inspect existing active notifications
        try {
            val activeNotifications = activeNotifications
            activeNotifications?.forEach { sbn ->
                inspectNotification(sbn)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error inspecting active notifications", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected")
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing active sessions changed listener", e)
        }
        TransportEngine.detachController()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { inspectNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            if (it.packageName == TransportEngine.TARGET_PACKAGE) {
                Log.d(TAG, "Target package notification removed: ${it.packageName}")
                // Check if session is still alive via session manager before detaching
                try {
                    val controllers = sessionManager?.getActiveSessions(componentName)
                    val stillHasSession = controllers?.any { c -> c.packageName == TransportEngine.TARGET_PACKAGE } == true
                    if (!stillHasSession) {
                        TransportEngine.detachController()
                    }
                } catch (e: Exception) {
                    TransportEngine.detachController()
                }
            }
        }
    }

    private fun inspectControllers(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            Log.d(TAG, "No active media controllers found")
            return
        }

        val targetController = controllers.firstOrNull { it.packageName == TransportEngine.TARGET_PACKAGE }
        if (targetController != null) {
            Log.i(TAG, "Found target MediaController for ${TransportEngine.TARGET_PACKAGE}")
            TransportEngine.attachController(targetController)
        } else {
            Log.d(TAG, "Target package ${TransportEngine.TARGET_PACKAGE} not found among active sessions")
        }
    }

    private fun inspectNotification(sbn: StatusBarNotification) {
        if (sbn.packageName != TransportEngine.TARGET_PACKAGE) {
            return
        }

        Log.d(TAG, "Processing notification from target package: ${sbn.packageName}")
        val extras = sbn.notification.extras ?: return

        @Suppress("DEPRECATION")
        val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
        }

        if (token != null) {
            Log.i(TAG, "Extracted MediaSession.Token from notification of ${sbn.packageName}")
            val controller = MediaController(this, token)
            TransportEngine.attachController(controller)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MediaNotificationListener destroyed")
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            // Ignored
        }
        TransportEngine.detachController()
    }

    companion object {
        private const val TAG = "MediaNotificationListener"
    }
}
