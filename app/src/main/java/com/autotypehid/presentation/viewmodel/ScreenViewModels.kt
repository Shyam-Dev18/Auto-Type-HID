package com.autotypehid.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotypehid.core.managers.AppContainer
import com.autotypehid.domain.model.ConnectionState
import com.autotypehid.domain.model.TypingState
import com.autotypehid.presentation.navigation.Routes
import com.autotypehid.presentation.state.DashboardUiEvent
import com.autotypehid.presentation.state.DashboardUiState
import com.autotypehid.presentation.state.DeviceScanUiEvent
import com.autotypehid.presentation.state.DeviceScanUiState
import com.autotypehid.presentation.state.PermissionsUiEvent
import com.autotypehid.presentation.state.PermissionsUiState
import com.autotypehid.presentation.state.ScriptEditorUiEvent
import com.autotypehid.presentation.state.ScriptEditorUiState
import com.autotypehid.presentation.state.ScriptsListUiEvent
import com.autotypehid.presentation.state.ScriptsListUiState
import com.autotypehid.presentation.state.SettingsUiEvent
import com.autotypehid.presentation.state.SettingsUiState
import com.autotypehid.presentation.state.SplashUiEvent
import com.autotypehid.presentation.state.SplashUiState
import com.autotypehid.presentation.state.TypingControlUiEvent
import com.autotypehid.presentation.state.TypingControlUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<String>()
    val navigation: SharedFlow<String> = _navigation.asSharedFlow()

    fun onEvent(event: SplashUiEvent) {
        if (event is SplashUiEvent.OnReady) {
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(isLoading = false) }

                if (event.bluetoothGranted && event.notificationsGranted) {
                    _navigation.emit(Routes.DASHBOARD)
                } else {
                    _navigation.emit(Routes.PERMISSIONS)
                }
            }
        }
    }
}

class PermissionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<String>()
    val navigation: SharedFlow<String> = _navigation.asSharedFlow()

    fun onEvent(event: PermissionsUiEvent) {
        when (event) {
            is PermissionsUiEvent.OnPermissionResult -> {
                val canContinue = event.bluetoothGranted && event.notificationsGranted
                _uiState.update {
                    it.copy(
                        bluetoothGranted = event.bluetoothGranted,
                        notificationsGranted = event.notificationsGranted,
                        canContinue = canContinue,
                        error = if (canContinue) null else "Required permissions are missing"
                    )
                }

                if (canContinue) {
                    viewModelScope.launch { _navigation.emit(Routes.DASHBOARD) }
                }
            }

            PermissionsUiEvent.OnContinue -> {
                if (_uiState.value.canContinue) {
                    viewModelScope.launch { _navigation.emit(Routes.DASHBOARD) }
                } else {
                    _uiState.update { it.copy(error = "Grant required permissions to continue") }
                }
            }
        }
    }
}

class DeviceScanViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceScanUiState())
    val uiState: StateFlow<DeviceScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.observeBluetoothDevicesUseCase().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeIsScanningUseCase().collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeConnectionStateUseCase().collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    fun onEvent(event: DeviceScanUiEvent) {
        when (event) {
            DeviceScanUiEvent.OnStartScan -> AppContainer.startDeviceScanUseCase()
            DeviceScanUiEvent.OnStopScan -> AppContainer.stopDeviceScanUseCase()
            DeviceScanUiEvent.OnDisconnect -> AppContainer.disconnectDeviceUseCase()
            is DeviceScanUiEvent.OnConnect -> AppContainer.connectDeviceUseCase(event.address)
        }
    }
}

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<String>()
    val navigation: SharedFlow<String> = _navigation.asSharedFlow()

    init {
        viewModelScope.launch {
            AppContainer.observeBluetoothStateUseCase().collect { state ->
                _uiState.update { it.copy(bluetoothState = state) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeConnectionStateUseCase().collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeConnectedDeviceUseCase().collect { device ->
                _uiState.update {
                    it.copy(connectedDeviceName = device?.name ?: "No active device")
                }
            }
        }
        viewModelScope.launch {
            AppContainer.observeSavedDevicesUseCase().collect { devices ->
                _uiState.update { it.copy(savedDevicesCount = devices.size) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeLastConnectedAddressUseCase().collect { address ->
                _uiState.update { it.copy(lastConnectedAddress = address) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeSelectedScriptUseCase().collect { script ->
                _uiState.update { it.copy(selectedScriptName = script?.name ?: "None") }
            }
        }
    }

    fun onEvent(event: DashboardUiEvent) {
        viewModelScope.launch {
            when (event) {
                DashboardUiEvent.OnScriptsClick -> _navigation.emit(Routes.SCRIPTS_LIST)
                DashboardUiEvent.OnSettingsClick -> _navigation.emit(Routes.SETTINGS)
                DashboardUiEvent.OnTypingClick -> _navigation.emit(Routes.TYPING_CONTROL)
                DashboardUiEvent.OnManageDeviceClick -> _navigation.emit(Routes.DEVICE_SCAN)
                DashboardUiEvent.OnReconnectClick -> AppContainer.reconnectLastDeviceUseCase()
                DashboardUiEvent.OnBluetoothSettingsClick -> AppContainer.openBluetoothSettingsUseCase()
            }
        }
    }
}

class ScriptsListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScriptsListUiState())
    val uiState: StateFlow<ScriptsListUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<String>()
    val navigation: SharedFlow<String> = _navigation.asSharedFlow()

    init {
        viewModelScope.launch {
            AppContainer.observeScriptsUseCase().collect { scripts ->
                _uiState.update { it.copy(isLoading = false, scripts = scripts) }
            }
        }
    }

    fun onEvent(event: ScriptsListUiEvent) {
        when (event) {
            ScriptsListUiEvent.OnCreateNew -> {
                viewModelScope.launch { _navigation.emit(Routes.scriptEditor(null)) }
            }
            is ScriptsListUiEvent.OnDelete -> {
                viewModelScope.launch { AppContainer.deleteScriptUseCase(event.scriptId) }
            }
            is ScriptsListUiEvent.OnEdit -> {
                viewModelScope.launch { _navigation.emit(Routes.scriptEditor(event.scriptId)) }
            }
            is ScriptsListUiEvent.OnSelect -> {
                AppContainer.selectScriptUseCase(event.script)
            }
        }
    }
}

class ScriptEditorViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScriptEditorUiState())
    val uiState: StateFlow<ScriptEditorUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<String>()
    val navigation: SharedFlow<String> = _navigation.asSharedFlow()

    init {
        val scriptId = savedStateHandle.get<String>(Routes.SCRIPT_ID_ARG)?.toIntOrNull()
            ?: savedStateHandle.get<Int>(Routes.SCRIPT_ID_ARG)
            ?: -1

        if (scriptId > 0) {
            viewModelScope.launch {
                val script = AppContainer.loadScriptUseCase(scriptId)
                if (script != null) {
                    _uiState.update {
                        it.copy(
                            scriptId = script.id,
                            name = script.name,
                            content = script.content
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ScriptEditorUiEvent) {
        when (event) {
            ScriptEditorUiEvent.OnBack -> {
                viewModelScope.launch { _navigation.emit(Routes.SCRIPTS_LIST) }
            }
            is ScriptEditorUiEvent.OnContentChange -> {
                _uiState.update { it.copy(content = event.value) }
            }
            is ScriptEditorUiEvent.OnNameChange -> {
                _uiState.update { it.copy(name = event.value) }
            }
            ScriptEditorUiEvent.OnSave -> {
                val current = _uiState.value
                if (current.name.isBlank() || current.content.isBlank()) {
                    _uiState.update { it.copy(error = "Name and content are required") }
                    return
                }

                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true, error = null) }
                    AppContainer.saveScriptUseCase(current.scriptId, current.name, current.content)
                    _uiState.update { it.copy(isSaving = false) }
                    _navigation.emit(Routes.SCRIPTS_LIST)
                }
            }
        }
    }
}

class TypingControlViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TypingControlUiState())
    val uiState: StateFlow<TypingControlUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.observeSelectedScriptUseCase().collect { script ->
                _uiState.update {
                    it.copy(
                        selectedScriptName = script?.name ?: "None",
                        selectedScriptContent = script?.content.orEmpty()
                    )
                }
            }
        }
        viewModelScope.launch {
            AppContainer.observeSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        speed = settings.speed,
                        typoProbability = settings.typoProbability.coerceIn(0f, 0.35f)
                    )
                }
            }
        }
        viewModelScope.launch {
            AppContainer.observeTypingStateUseCase().collect { typingState ->
                _uiState.update { it.copy(typingState = typingState) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeTypingProgressUseCase().collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeBluetoothStateUseCase().collect { bluetoothState ->
                _uiState.update { it.copy(bluetoothState = bluetoothState) }
            }
        }
        viewModelScope.launch {
            AppContainer.observeConnectionStateUseCase().collect { connectionState ->
                _uiState.update { it.copy(connectionState = connectionState) }
                if (connectionState != ConnectionState.CONNECTED && _uiState.value.typingState != TypingState.IDLE) {
                    AppContainer.controlTypingUseCase.stop()
                }
            }
        }
    }

    fun onEvent(event: TypingControlUiEvent) {
        when (event) {
            TypingControlUiEvent.OnStart -> {
                val state = _uiState.value
                if (state.connectionState != ConnectionState.CONNECTED) return
                AppContainer.controlTypingUseCase.start(
                    state.selectedScriptContent,
                    state.speed,
                    state.typoProbability
                )
            }
            TypingControlUiEvent.OnPauseOrResume -> {
                when (_uiState.value.typingState) {
                    TypingState.RUNNING -> AppContainer.controlTypingUseCase.pause()
                    TypingState.PAUSED -> AppContainer.controlTypingUseCase.resume()
                    else -> Unit
                }
            }
            TypingControlUiEvent.OnStop -> AppContainer.controlTypingUseCase.stop()
        }
    }
}

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppContainer.observeSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        profile = settings.profile,
                        speed = settings.speed,
                        typoProbability = settings.typoProbability
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.OnProfileChange -> _uiState.update { it.copy(profile = event.value) }
            is SettingsUiEvent.OnSpeedChange -> _uiState.update { it.copy(speed = event.value) }
            is SettingsUiEvent.OnTypoProbabilityChange -> _uiState.update { it.copy(typoProbability = event.value) }
            SettingsUiEvent.OnSave -> {
                val state = _uiState.value
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    AppContainer.updateSettingsUseCase(state.profile, state.speed, state.typoProbability)
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }
}
