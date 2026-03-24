package com.autotypehid.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autotypehid.data.model.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: ScriptEntity)

    @Query("SELECT * FROM scripts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ScriptEntity?

    @Query("UPDATE scripts SET name = :name, content = :content WHERE id = :id")
    suspend fun update(id: Int, name: String, content: String)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Delete
    suspend fun delete(script: ScriptEntity)
}
