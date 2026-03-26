package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        profile: String,
        speed: Float,
        typoProbability: Float,
        wordGapMs: Int,
        jitterPercent: Int,
        themeMode: String
    ) {
        settingsRepository.updateSettings(
            profile = profile,
            speed = speed,
            typoProbability = typoProbability,
            wordGapMs = wordGapMs,
            jitterPercent = jitterPercent,
            themeMode = themeMode
        )
    }
}
