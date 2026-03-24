package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.ScriptsRepository
import javax.inject.Inject

class DeleteScriptUseCase @Inject constructor(
    private val scriptsRepository: ScriptsRepository
) {
    suspend operator fun invoke(scriptId: Int) {
        scriptsRepository.deleteScript(scriptId)
    }
}
