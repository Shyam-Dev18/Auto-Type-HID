package com.autotypehid.presentation.state

import com.autotypehid.domain.model.BluetoothAdapterState
import com.autotypehid.domain.model.ConnectionState
import com.autotypehid.domain.model.Script
import com.autotypehid.domain.model.TypingState

data class SplashUiState(
    val isLoading: Boolean = true
)

sealed interface SplashUiEvent {
    data class OnReady(
        val bluetoothGranted: Boolean,
        val notificationsGranted: Boolean
    ) : SplashUiEvent
}

data class PermissionsUiState(
    val bluetoothGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val canContinue: Boolean = false,
    val error: String? = null
)

sealed interface PermissionsUiEvent {
    data class OnPermissionResult(
        val bluetoothGranted: Boolean,
        val notificationsGranted: Boolean
    ) : PermissionsUiEvent

    data object OnContinue : PermissionsUiEvent
}

data class DeviceScanUiState(
    val devices: List<com.autotypehid.domain.model.ScannedDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val error: String? = null
)

sealed interface DeviceScanUiEvent {
    data object OnStartScan : DeviceScanUiEvent
    data object OnStopScan : DeviceScanUiEvent
    data class OnConnect(val address: String) : DeviceScanUiEvent
    data object OnDisconnect : DeviceScanUiEvent
}

data class DashboardUiState(
    val bluetoothState: BluetoothAdapterState = BluetoothAdapterState.UNAVAILABLE,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectedDeviceName: String = "No active device",
    val selectedScriptName: String = "None",
    val savedDevicesCount: Int = 0,
    val lastConnectedAddress: String? = null
)

sealed interface DashboardUiEvent {
    data object OnScriptsClick : DashboardUiEvent
    data object OnSettingsClick : DashboardUiEvent
    data object OnTypingClick : DashboardUiEvent
    data object OnManageDeviceClick : DashboardUiEvent
    data object OnReconnectClick : DashboardUiEvent
    data object OnBluetoothSettingsClick : DashboardUiEvent
}

data class ScriptsListUiState(
    val isLoading: Boolean = true,
    val scripts: List<Script> = emptyList()
)

sealed interface ScriptsListUiEvent {
    data object OnCreateNew : ScriptsListUiEvent
    data class OnEdit(val scriptId: Int) : ScriptsListUiEvent
    data class OnDelete(val scriptId: Int) : ScriptsListUiEvent
    data class OnSelect(val script: Script) : ScriptsListUiEvent
}

data class ScriptEditorUiState(
    val scriptId: Int? = null,
    val name: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface ScriptEditorUiEvent {
    data class OnNameChange(val value: String) : ScriptEditorUiEvent
    data class OnContentChange(val value: String) : ScriptEditorUiEvent
    data object OnSave : ScriptEditorUiEvent
    data object OnBack : ScriptEditorUiEvent
}

data class TypingControlUiState(
    val selectedScriptName: String = "None",
    val selectedScriptContent: String = "",
    val typingState: TypingState = TypingState.IDLE,
    val progress: Int = 0,
    val speed: Float = 1.0f,
    val typoProbability: Float = 0.18f,
    val bluetoothState: BluetoothAdapterState = BluetoothAdapterState.UNAVAILABLE,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)

sealed interface TypingControlUiEvent {
    data object OnStart : TypingControlUiEvent
    data object OnPauseOrResume : TypingControlUiEvent
    data object OnStop : TypingControlUiEvent
}

data class SettingsUiState(
    val profile: String = "NORMAL",
    val speed: Float = 1.0f,
    val typoProbability: Float = 0.18f,
    val isSaving: Boolean = false
)

sealed interface SettingsUiEvent {
    data class OnProfileChange(val value: String) : SettingsUiEvent
    data class OnSpeedChange(val value: Float) : SettingsUiEvent
    data class OnTypoProbabilityChange(val value: Float) : SettingsUiEvent
    data object OnSave : SettingsUiEvent
}
