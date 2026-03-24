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

    @Delete
    suspend fun delete(script: ScriptEntity)
}
