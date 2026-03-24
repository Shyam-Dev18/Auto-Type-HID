package com.autotypehid.domain.usecase

import com.autotypehid.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow

class SettingsUseCases(
    private val settingsDataStore: SettingsDataStore
) {

    suspend fun saveProfile(profile: String) {
        settingsDataStore.saveProfile(profile)
    }

    fun getProfile(): Flow<String> {
        return settingsDataStore.selectedProfile
    }

    suspend fun saveTypingSpeed(value: Float) {
        settingsDataStore.saveTypingSpeed(value)
    }

    fun getTypingSpeed(): Flow<Float> {
        return settingsDataStore.typingSpeedMultiplier
    }

    suspend fun saveTypoProbability(value: Float) {
        settingsDataStore.saveTypoProbability(value)
    }

    fun getTypoProbability(): Flow<Float> {
        return settingsDataStore.typoProbabilityOverride
    }
}
