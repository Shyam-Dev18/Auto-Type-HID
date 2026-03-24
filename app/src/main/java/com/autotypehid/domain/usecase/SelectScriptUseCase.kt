package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.ScriptsRepository
import com.autotypehid.domain.model.Script
import javax.inject.Inject

class SelectScriptUseCase @Inject constructor(
    private val scriptsRepository: ScriptsRepository
) {
    operator fun invoke(script: Script) {
        scriptsRepository.selectScript(script)
    }
}
