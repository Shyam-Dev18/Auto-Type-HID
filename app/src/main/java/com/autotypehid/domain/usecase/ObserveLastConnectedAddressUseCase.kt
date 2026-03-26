package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository

class ObserveLastConnectedAddressUseCase(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke() = bluetoothRepository.lastConnectedAddress
}
