package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository
import javax.inject.Inject

class ConnectDeviceUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke(address: String) {
        bluetoothRepository.connect(address)
    }
}
