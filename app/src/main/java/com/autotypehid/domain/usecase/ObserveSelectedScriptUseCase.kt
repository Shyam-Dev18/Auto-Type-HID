package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.ScriptsRepository
import com.autotypehid.domain.model.Script
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSelectedScriptUseCase @Inject constructor(
    private val scriptsRepository: ScriptsRepository
) {
    operator fun invoke(): StateFlow<Script?> = scriptsRepository.selectedScript
}
