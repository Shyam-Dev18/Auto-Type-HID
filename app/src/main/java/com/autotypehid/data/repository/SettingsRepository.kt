package com.autotypehid.data.repository

import com.autotypehid.data.local.SettingsDataStore
import com.autotypehid.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) {
    fun observeSettings(): Flow<AppSettings> {
        return combine(
            dataStore.selectedProfile,
            dataStore.typingSpeedMultiplier,
            dataStore.typoProbabilityOverride
        ) { profile, speed, typo ->
            AppSettings(
                profile = profile,
                speed = speed,
                typoProbability = if (typo < 0f) 0.18f else typo
            )
        }
    }

    suspend fun updateSettings(profile: String, speed: Float, typoProbability: Float) {
        dataStore.saveProfile(profile)
        dataStore.saveTypingSpeed(speed)
        dataStore.saveTypoProbability(typoProbability)
    }
}
