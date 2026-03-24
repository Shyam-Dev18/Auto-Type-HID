package com.autotypehid.di

import android.content.Context
import androidx.room.Room
import com.autotypehid.data.local.AppDatabase
import com.autotypehid.data.local.ScriptDao
import com.autotypehid.data.repository.ScriptRepository
import com.autotypehid.domain.usecase.ScriptUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

	private var dbInstance: AppDatabase? = null

	@Provides
	@Singleton
	fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
		return getOrCreateDatabase(context)
	}

	@Provides
	@Singleton
	fun provideScriptDao(database: AppDatabase): ScriptDao {
		return database.scriptDao()
	}

	@Provides
	@Singleton
	fun provideScriptRepository(scriptDao: ScriptDao): ScriptRepository {
		return ScriptRepository(scriptDao)
	}

	@Provides
	@Singleton
	fun provideScriptUseCases(repository: ScriptRepository): ScriptUseCases {
		return ScriptUseCases(repository)
	}

	fun provideScriptUseCasesFallback(context: Context): ScriptUseCases {
		val database = getOrCreateDatabase(context)
		return ScriptUseCases(ScriptRepository(database.scriptDao()))
	}

	private fun getOrCreateDatabase(context: Context): AppDatabase {
		val current = dbInstance
		if (current != null) return current

		val created = Room.databaseBuilder(
			context.applicationContext,
			AppDatabase::class.java,
			"autotype_scripts.db"
		).fallbackToDestructiveMigration().build()

		dbInstance = created
		return created
	}
}
