package com.autotypehid.domain.usecase

import android.content.Context
import com.autotypehid.core.managers.TypingForegroundService

class ControlTypingUseCase(
    private val context: Context
) {
    fun start(content: String, speed: Float, typoProbability: Float) {
        TypingForegroundService.start(context, content, speed, typoProbability)
    }

    fun pause() {
        TypingForegroundService.pause(context)
    }

    fun resume() {
        TypingForegroundService.resume(context)
    }

    fun stop() {
        TypingForegroundService.stop(context)
    }
}
