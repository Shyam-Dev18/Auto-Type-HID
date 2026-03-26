package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository

class HandleBluetoothIconActionUseCase(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke() = bluetoothRepository.handleBluetoothIconAction()
}

