package com.termuxai.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.termuxai.app.R
import com.termuxai.app.TermuxApplication
import com.termuxai.app.core.AiState
import com.termuxai.app.core.network.TermuxEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull

/**
 * Hosts the [com.termuxai.app.core.wakeword.WakeWordListener] for the
 * lifetime of the app. Per spec: the microphone is opened exactly once, in
 * [onCreate], and never stopped/restarted on every wake cycle — only the
 * whole service stopping (user disables the assistant, or process death)
 * tears the mic tap down.
 *
 * On detection, this service:
 *  1. flips [com.termuxai.app.core.AiStateHolder] to LISTENING so the UI
 *     (dashboard avatar) updates immediately,
 *  2. notifies the Termux server over the existing WebSocket connection, and
 *  3. arms a timeout that falls back to SLEEPING if Termux never replies.
 *
 * The actual speech capture (hearing what the user says) and TTS playback
 * (speaking Termux's replies) are both owned by
 * [com.termuxai.app.core.AssistantOrchestrator], which lives at the
 * process/container scope so that sequencing keeps working regardless of
 * which service is currently alive, and so mic capture and TTS playback can
 * never run at the same time.
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        private const val NOTIFICATION_CHANNEL_ID = "wake_word_engine"
        private const val NOTIFICATION_ID = 1002
        private const val GREETING_TIMEOUT_MS = 6_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, WakeWordService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)

    private val app: TermuxApplication by lazy { application as TermuxApplication }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            // Exactly one start() call for the entire process lifetime.
            app.container.wakeWordListener.start()
        } catch (e: Exception) {
            // Synchronous failure modes (rare with Vosk, since model
            // loading is async) -- surfaced to the notification rather than
            // silently pretending to listen.
            updateNotificationError(e.message ?: "Wake word engine failed to start")
        }

        // Vosk's model loading is async, so most real failures (missing
        // model assets, corrupt model, mic in use) arrive here instead of
        // via the try/catch above.
        app.container.wakeWordListener.errors
            .onEach { message -> updateNotificationError(message) }
            .launchIn(serviceScope)

        app.container.wakeWordListener.detections
            .onEach { onWakeWordDetected() }
            .launchIn(serviceScope)
    }

    private fun onWakeWordDetected() {
        val isConnectedToTermux =
            app.container.termuxRepository.connectionState.value is com.termuxai.app.core.network.ConnectionState.Connected

        if (!isConnectedToTermux) {
            // Termux isn't reachable right now -- don't wait on a greeting
            // that will never come. Open the mic directly so local-only
            // commands (time/date/battery/torch) still work.
            app.container.assistantOrchestrator.listenDirectly()
            return
        }

        app.container.aiStateHolder.set(AiState.LISTENING)
        serviceScope.launch {
            app.container.termuxRepository.send(
                TermuxEnvelope(type = "wakeword.detected", payload = JsonNull)
            )
        }

        // Real speech capture is triggered by AssistantOrchestrator, only
        // after Termux's greeting has actually finished playing via TTS —
        // never here, and never concurrently with that playback (see
        // AssistantOrchestrator.kt for why).

        // Safety timeout: if Termux never replies (server not running, not
        // connected), don't leave the assistant stuck in LISTENING forever.
        serviceScope.launch {
            delay(GREETING_TIMEOUT_MS)
            if (app.container.aiStateHolder.state.value == AiState.LISTENING) {
                Log.w(TAG, "No greeting from Termux within ${GREETING_TIMEOUT_MS}ms — going back to sleep")
                app.container.aiStateHolder.set(AiState.SLEEPING)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_text))
            .setSmallIcon(R.drawable.ic_connection_status)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun updateNotificationError(message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_connection_status)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.wake_word_channel_name),
            NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        // Only real teardown path: process/service actually stopping.
        app.container.wakeWordListener.stop()
        app.container.speechToTextEngine.stop()
        serviceScope.cancel()
        super.onDestroy()
    }
}
