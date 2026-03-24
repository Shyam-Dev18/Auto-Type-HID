package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository
import javax.inject.Inject

class StartDeviceScanUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke() {
        bluetoothRepository.startScan()
    }
}
