package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(profile: String, speed: Float, typoProbability: Float) {
        settingsRepository.updateSettings(profile, speed, typoProbability)
    }
}
