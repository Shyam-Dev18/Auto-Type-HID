package com.autotypehid.domain.usecase

import com.autotypehid.bluetooth.sender.HidReportSender
import com.autotypehid.core.utils.Logger
import kotlinx.coroutines.delay

class ExecuteTypingUseCase(
    private val hidReportSender: HidReportSender
) {

    companion object {
        private const val TAG = "ExecuteTypingUseCase"
    }

    suspend fun execute(
        actions: List<TypingAction>,
        onAction: ((TypingAction) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        try {
            actions.forEach { action ->
                onAction?.invoke(action)
                when (action.type) {
                    TypingActionType.KEY -> {
                        val keyEvent = action.keyEvent
                        if (keyEvent != null) {
                            hidReportSender.sendKey(
                                keyCode = keyEvent.keyCode,
                                shift = keyEvent.requiresShift
                            )
                        }
                    }

                    TypingActionType.DELAY -> {
                        val delayMs = action.delayMs ?: 0L
                        if (delayMs > 0L) {
                            delay(delayMs)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.error(TAG, "Execution failed: ${t.message}")
            onError?.invoke(t)
        }
    }
}