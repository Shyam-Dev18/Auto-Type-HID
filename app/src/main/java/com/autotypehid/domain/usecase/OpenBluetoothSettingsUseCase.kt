package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository

class OpenBluetoothSettingsUseCase(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke() = bluetoothRepository.openBluetoothSettings()
}
