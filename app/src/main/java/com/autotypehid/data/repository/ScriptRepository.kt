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

    suspend fun deleteScript(script: ScriptEntity) {
        scriptDao.delete(script)
    }
}
