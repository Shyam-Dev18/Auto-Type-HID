package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository

class ObserveConnectedDeviceUseCase(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke() = bluetoothRepository.connectedDevice
}
