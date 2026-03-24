package com.autotypehid.core.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.autotypehid.R
import com.autotypehid.domain.model.TypingState
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TypingForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var typingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Typing in progress"))
                val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f).coerceAtLeast(0.2f)
                val typoProbability = intent.getFloatExtra(EXTRA_TYPO_PROBABILITY, 0.18f).coerceIn(0f, 0.35f)
                startTyping(content, speed, typoProbability)
            }
            ACTION_PAUSE -> TypingServiceStore.setState(TypingState.PAUSED)
            ACTION_RESUME -> TypingServiceStore.setState(TypingState.RUNNING)
            ACTION_STOP -> stopTypingAndSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        typingJob?.cancel()
        scope.cancel()
    }

    private fun startTyping(content: String, speed: Float, typoProbability: Float) {
        typingJob?.cancel()
        typingJob = scope.launch {
            if (content.isBlank()) {
                TypingServiceStore.setState(TypingState.ERROR)
                stopTypingAndSelf()
                return@launch
            }

            if (!AppContainer.bluetoothRepository.isTypingReady()) {
                TypingServiceStore.setState(TypingState.ERROR)
                stopTypingAndSelf()
                return@launch
            }

            TypingServiceStore.setProgress(0)
            TypingServiceStore.setState(TypingState.RUNNING)
            val total = content.length

            content.forEachIndexed { index, char ->
                while (isActive && TypingServiceStore.typingState.value == TypingState.PAUSED) {
                    delay(100)
                }
                if (!isActive || TypingServiceStore.typingState.value != TypingState.RUNNING) {
                    return@launch
                }

                maybeInjectTypo(char, typoProbability, speed)

                val sent = AppContainer.bluetoothRepository.sendCharacter(char)
                if (!sent) {
                    TypingServiceStore.setState(TypingState.ERROR)
                    stopTypingAndSelf()
                    return@launch
                }

                delay(AppContainer.bluetoothRepository.computeAggressiveDelay(speed))
                TypingServiceStore.setProgress(((index + 1) * 100) / total)
            }

            TypingServiceStore.setState(TypingState.COMPLETED)
            delay(200)
            stopTypingAndSelf()
        }
    }

    private suspend fun maybeInjectTypo(currentChar: Char, typoProbability: Float, speed: Float) {
        if (!currentChar.isLetter()) return
        if (Random.nextFloat() > typoProbability) return

        val typoChar = if (currentChar.isLowerCase()) {
            ('a'..'z').random()
        } else {
            ('A'..'Z').random()
        }

        val typoSent = AppContainer.bluetoothRepository.sendCharacter(typoChar)
        if (!typoSent) return

        delay(AppContainer.bluetoothRepository.computeAggressiveDelay(speed))
        AppContainer.bluetoothRepository.sendBackspace()
        delay((AppContainer.bluetoothRepository.computeAggressiveDelay(speed) / 2).coerceAtLeast(25L))
    }

    private fun stopTypingAndSelf() {
        typingJob?.cancel()
        TypingServiceStore.setState(TypingState.IDLE)
        TypingServiceStore.setProgress(0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(content: String): Notification {
        createChannelIfNeeded()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AutoType HID")
            .setContentText(content)
            .setOngoing(true)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Typing Service",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "typing_service_channel"
        private const val NOTIFICATION_ID = 991
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_SPEED = "extra_speed"
        private const val EXTRA_TYPO_PROBABILITY = "extra_typo_probability"

        const val ACTION_START = "com.autotypehid.typing.START"
        const val ACTION_PAUSE = "com.autotypehid.typing.PAUSE"
        const val ACTION_RESUME = "com.autotypehid.typing.RESUME"
        const val ACTION_STOP = "com.autotypehid.typing.STOP"

        fun start(context: Context, content: String, speed: Float, typoProbability: Float) {
            val intent = Intent(context, TypingForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_CONTENT, content)
                .putExtra(EXTRA_SPEED, speed)
                .putExtra(EXTRA_TYPO_PROBABILITY, typoProbability)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, TypingForegroundService::class.java).setAction(ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(Intent(context, TypingForegroundService::class.java).setAction(ACTION_RESUME))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TypingForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
