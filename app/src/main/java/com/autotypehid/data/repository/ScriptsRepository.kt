package com.autotypehid.data.repository

import com.autotypehid.domain.model.Script
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptsRepository @Inject constructor(
    private val legacyRepository: ScriptRepository
) {
    private val _selectedScript = MutableStateFlow<Script?>(null)
    val selectedScript = _selectedScript.asStateFlow()

    fun observeScripts(): Flow<List<Script>> {
        return legacyRepository.getScripts().map { entities ->
            entities.map { entity ->
                Script(
                    id = entity.id,
                    name = entity.name,
                    content = entity.content,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun loadScript(scriptId: Int): Script? {
        val entity = legacyRepository.getScriptById(scriptId) ?: return null
        return Script(
            id = entity.id,
            name = entity.name,
            content = entity.content,
            createdAt = entity.createdAt
        )
    }

    suspend fun saveScript(scriptId: Int?, name: String, content: String) {
        legacyRepository.upsertScript(scriptId, name, content)
    }

    suspend fun deleteScript(scriptId: Int) {
        legacyRepository.deleteScriptById(scriptId)
    }

    fun selectScript(script: Script) {
        _selectedScript.value = script
    }
}
