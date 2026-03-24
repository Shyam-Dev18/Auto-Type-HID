package com.autotypehid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.autotypehid.data.model.ScriptEntity

@Database(
    entities = [ScriptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
}
