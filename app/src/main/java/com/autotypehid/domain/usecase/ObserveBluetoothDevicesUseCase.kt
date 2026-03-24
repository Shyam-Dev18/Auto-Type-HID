package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository
import com.autotypehid.domain.model.ScannedDevice
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveBluetoothDevicesUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke(): StateFlow<List<ScannedDevice>> = bluetoothRepository.devices
}
