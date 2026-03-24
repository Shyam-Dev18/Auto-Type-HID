package com.autotypehid.domain.usecase

import com.autotypehid.core.managers.TypingServiceStore
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveTypingProgressUseCase @Inject constructor() {
    operator fun invoke(): StateFlow<Int> = TypingServiceStore.progress
}
