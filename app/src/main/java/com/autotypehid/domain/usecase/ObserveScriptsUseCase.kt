package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.ScriptsRepository
import com.autotypehid.domain.model.Script
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveScriptsUseCase @Inject constructor(
    private val scriptsRepository: ScriptsRepository
) {
    operator fun invoke(): Flow<List<Script>> = scriptsRepository.observeScripts()
}
