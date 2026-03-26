package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository

class RemoveSavedDeviceUseCase(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke(address: String) = bluetoothRepository.removeSavedDevice(address)
}
