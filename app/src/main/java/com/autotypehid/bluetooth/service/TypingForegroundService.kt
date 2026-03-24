package com.autotypehid.bluetooth.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.autotypehid.R
import com.autotypehid.bluetooth.sender.HidReportSender
import com.autotypehid.core.utils.Logger
import com.autotypehid.domain.usecase.ExecuteTypingUseCase
import com.autotypehid.domain.usecase.TypingAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TypingForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "typing_foreground_channel"
        private const val CHANNEL_NAME = "Typing Service"
        private const val NOTIFICATION_ID = 1101
        private const val ACTION_START = "com.autotypehid.action.START_TYPING"
        private const val ACTION_STOP = "com.autotypehid.action.STOP_TYPING"
        private const val TAG = "TypingForegroundService"

        @Volatile
        private var pendingActions: List<TypingAction> = emptyList()

        fun startService(context: Context, actions: List<TypingAction>) {
            pendingActions = actions
            val intent = Intent(context, TypingForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TypingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hidService by lazy { HidService(applicationContext) }
    private val hidReportSender by lazy { HidReportSender(hidService) }
    private val executeTypingUseCase by lazy { ExecuteTypingUseCase(hidReportSender) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification())
                hidService.initializeAndRegister()
                val actions = pendingActions

                serviceScope.launch {
                    executeTypingUseCase.execute(
                        actions = actions,
                        onAction = { action -> Logger.debug(TAG, "Executing action type=${action.type}") },
                        onError = { error -> Logger.error(TAG, "Typing execution error: ${error.message}") }
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        hidService.close()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoType HID")
            .setContentText("AutoType HID is typing...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }
}
