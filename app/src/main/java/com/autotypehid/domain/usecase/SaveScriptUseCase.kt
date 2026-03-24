package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.ScriptsRepository
import javax.inject.Inject

class SaveScriptUseCase @Inject constructor(
    private val scriptsRepository: ScriptsRepository
) {
    suspend operator fun invoke(scriptId: Int?, name: String, content: String) {
        scriptsRepository.saveScript(scriptId, name, content)
    }
}