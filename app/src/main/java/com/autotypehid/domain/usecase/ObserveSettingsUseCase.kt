package com.autotypehid.domain.usecase

import com.autotypehid.data.repository.SettingsRepository
import com.autotypehid.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.observeSettings()
}
