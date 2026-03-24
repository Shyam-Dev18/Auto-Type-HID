package com.autotypehid.core.managers

import com.autotypehid.domain.model.TypingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TypingServiceStore {
    private val _typingState = MutableStateFlow(TypingState.IDLE)
    val typingState: StateFlow<TypingState> = _typingState.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    fun setState(state: TypingState) {
        _typingState.value = state
    }

    fun setProgress(value: Int) {
        _progress.value = value.coerceIn(0, 100)
    }
}
