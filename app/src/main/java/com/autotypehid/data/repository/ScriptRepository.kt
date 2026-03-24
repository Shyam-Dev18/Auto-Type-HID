package com.autotypehid.data.repository

import com.autotypehid.data.local.ScriptDao
import com.autotypehid.data.model.ScriptEntity
import kotlinx.coroutines.flow.Flow

class ScriptRepository(
    private val scriptDao: ScriptDao
) {

    suspend fun addScript(name: String, content: String) {
        if (name.isBlank() || content.isBlank()) return
        scriptDao.insert(
            ScriptEntity(
                name = name.trim(),
                content = content,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun getScripts(): Flow<List<ScriptEntity>> {
        return scriptDao.getAll()
    }

    suspend fun getScriptById(id: Int): ScriptEntity? {
        return scriptDao.getById(id)
    }

    suspend fun upsertScript(id: Int?, name: String, content: String) {
        if (name.isBlank() || content.isBlank()) return
        val cleanName = name.trim()
        if (id == null || id <= 0) {
            addScript(cleanName, content)
            return
        }
        scriptDao.update(id, cleanName, content)
    }

    suspend fun deleteScript(script: ScriptEntity) {
        scriptDao.delete(script)
    }

    suspend fun deleteScriptById(id: Int) {
        scriptDao.deleteById(id)
    }
}
