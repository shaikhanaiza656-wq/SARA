package com.termuxai.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.termuxai.app.R
import com.termuxai.app.TermuxApplication
import com.termuxai.app.core.network.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground service whose only job is to (a) keep the process alive so the
 * persistent WebSocket survives the Activity being backgrounded, and (b)
 * mirror the real connection state into a persistent notification. It does
 * NOT own its own copy of connection logic — it drives the process-scoped
 * [com.termuxai.app.core.network.TermuxWebSocketClient] living in
 * [TermuxApplication.container].
 */
class TermuxConnectionService : Service() {

    companion object {
        const val ACTION_START = "com.termuxai.app.action.START_CONNECTION"
        const val ACTION_STOP = "com.termuxai.app.action.STOP_CONNECTION"
        private const val NOTIFICATION_CHANNEL_ID = "termux_connection_status"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, TermuxConnectionService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TermuxConnectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)
    private var observerJob: Job? = null

    private val app: TermuxApplication by lazy { application as TermuxApplication }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                app.container.termuxRepository.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startAsForeground()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification(ConnectionState.Connecting)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        app.container.termuxRepository.connect()

        observerJob?.cancel()
        observerJob = app.container.termuxRepository.connectionState
            .onEach { state -> updateNotification(state) }
            .launchIn(serviceScope)
    }

    private fun updateNotification(state: ConnectionState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: ConnectionState): Notification {
        val statusText = when (state) {
            is ConnectionState.Connected -> getString(R.string.connection_status_connected)
            is ConnectionState.Connecting -> getString(R.string.connection_status_connecting)
            is ConnectionState.Reconnecting -> getString(
                R.string.connection_status_reconnecting, state.attempt
            )
            is ConnectionState.Error -> getString(R.string.connection_status_error)
            is ConnectionState.Disconnected -> getString(R.string.connection_status_disconnected)
        }

        val stopIntent = Intent(this, TermuxConnectionService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_connection_status)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.action_disconnect), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.connection_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
