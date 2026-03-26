package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository

class ObserveBluetoothStateUseCase(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke() = bluetoothRepository.bluetoothState
}
