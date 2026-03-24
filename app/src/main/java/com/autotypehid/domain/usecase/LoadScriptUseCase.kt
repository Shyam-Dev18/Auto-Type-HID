package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.ScriptsRepository
import com.autotypehid.domain.model.Script
import javax.inject.Inject

class LoadScriptUseCase @Inject constructor(
    private val scriptsRepository: ScriptsRepository
) {
    suspend operator fun invoke(scriptId: Int): Script? {
        return scriptsRepository.loadScript(scriptId)
    }
}
