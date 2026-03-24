package com.autotypehid.domain.usecase

import com.autotypehid.data.model.ScriptEntity
import com.autotypehid.data.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow

class ScriptUseCases(
    private val repository: ScriptRepository
) {

    suspend fun addScript(name: String, content: String) {
        repository.addScript(name, content)
    }

    fun getScripts(): Flow<List<ScriptEntity>> {
        return repository.getScripts()
    }

    suspend fun deleteScript(script: ScriptEntity) {
        repository.deleteScript(script)
    }
}
