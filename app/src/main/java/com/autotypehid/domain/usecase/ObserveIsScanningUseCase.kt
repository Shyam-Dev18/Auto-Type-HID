package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveIsScanningUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke(): StateFlow<Boolean> = bluetoothRepository.isScanning
}
