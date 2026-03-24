package com.autotypehid.domain.usecase

import com.autotypehid.data.bluetooth.BluetoothSessionRepository
import com.autotypehid.domain.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothSessionRepository
) {
    operator fun invoke(): StateFlow<ConnectionState> = bluetoothRepository.connectionState
}
