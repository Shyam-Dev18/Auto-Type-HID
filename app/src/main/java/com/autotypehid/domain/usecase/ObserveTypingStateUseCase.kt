package com.autotypehid.domain.usecase

import com.autotypehid.core.managers.TypingServiceStore
import com.autotypehid.domain.model.TypingState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveTypingStateUseCase @Inject constructor() {
    operator fun invoke(): StateFlow<TypingState> = TypingServiceStore.typingState
}
