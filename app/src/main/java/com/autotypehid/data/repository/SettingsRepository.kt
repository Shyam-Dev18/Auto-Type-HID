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

    private data class CoreSettings(
        val profile: String,
        val speed: Float,
        val typoProbability: Float
    )

    fun observeSettings(): Flow<AppSettings> {
        val core = combine(
            dataStore.selectedProfile,
            dataStore.typingSpeedMultiplier,
            dataStore.typoProbabilityOverride
        ) { profile, speed, typo ->
            CoreSettings(
                profile = profile,
                speed = speed,
                typoProbability = if (typo < 0f) 0.18f else typo
            )
        }

        return combine(
            core,
            dataStore.wordGapMs,
            dataStore.jitterPercent,
            dataStore.themeMode
        ) { coreSettings, wordGapMs, jitterPercent, themeMode ->
            AppSettings(
                profile = coreSettings.profile,
                speed = coreSettings.speed,
                typoProbability = coreSettings.typoProbability,
                wordGapMs = wordGapMs,
                jitterPercent = jitterPercent,
                themeMode = themeMode
            )
        }
    }

    suspend fun updateSettings(
        profile: String,
        speed: Float,
        typoProbability: Float,
        wordGapMs: Int,
        jitterPercent: Int,
        themeMode: String
    ) {
        dataStore.saveProfile(profile)
        dataStore.saveTypingSpeed(speed)
        dataStore.saveTypoProbability(typoProbability)
        dataStore.saveWordGapMs(wordGapMs)
        dataStore.saveJitterPercent(jitterPercent)
        dataStore.saveThemeMode(themeMode)
    }
}
