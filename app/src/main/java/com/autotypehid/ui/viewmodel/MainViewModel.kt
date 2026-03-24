package com.autotypehid.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotypehid.bluetooth.manager.HidManager
import com.autotypehid.bluetooth.service.HidService
import com.autotypehid.bluetooth.service.TypingForegroundService
import com.autotypehid.core.utils.BluetoothUtils
import com.autotypehid.core.utils.Logger
import com.autotypehid.data.local.SettingsDataStore
import com.autotypehid.data.model.ScriptEntity
import com.autotypehid.di.AppModule
import com.autotypehid.domain.usecase.ScriptUseCases
import com.autotypehid.domain.usecase.SettingsUseCases
import com.autotypehid.domain.usecase.StartTypingUseCase
import com.autotypehid.domain.usecase.TypingAction
import com.autotypehid.typing.mapper.KeyEvent
import com.autotypehid.humanizer.profile.BehaviorProfile
import com.autotypehid.humanizer.profile.DefaultProfiles
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val selectedProfile: String = "NORMAL",
    val typingSpeedMultiplier: Float = 1.0f,
    val typoProbabilityOverride: Float = -1f,
    val isLoading: Boolean = false
)

class MainViewModel(private val context: Context? = null) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _state = MutableStateFlow("Ready")
    val state: StateFlow<String> = _state
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    private val _typingPreview = MutableStateFlow<List<KeyEvent>>(emptyList())
    val typingPreview: StateFlow<List<KeyEvent>> = _typingPreview

    private val _selectedProfile = MutableStateFlow(DefaultProfiles.NORMAL)
    val selectedProfile: StateFlow<BehaviorProfile> = _selectedProfile

    private val _typingActions = MutableStateFlow<List<TypingAction>>(emptyList())
    val typingActions: StateFlow<List<TypingAction>> = _typingActions

    private val _executionState = MutableStateFlow("Idle")
    val executionState: StateFlow<String> = _executionState

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private val _bluetoothPermissionGranted = MutableStateFlow(false)
    val bluetoothPermissionGranted: StateFlow<Boolean> = _bluetoothPermissionGranted

    private val _scripts = MutableStateFlow<List<ScriptEntity>>(emptyList())
    val scripts: StateFlow<List<ScriptEntity>> = _scripts

    private val _selectedScript = MutableStateFlow<ScriptEntity?>(null)
    val selectedScript: StateFlow<ScriptEntity?> = _selectedScript

    private val _settingsState = MutableStateFlow(SettingsState(isLoading = true))
    val settingsState: StateFlow<SettingsState> = _settingsState

    private val _debugLogs = MutableStateFlow<List<String>>(emptyList())
    val debugLogs: StateFlow<List<String>> = _debugLogs

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _lastExecutedActions = MutableStateFlow<List<String>>(emptyList())
    val lastExecutedActions: StateFlow<List<String>> = _lastExecutedActions
    
    private val hidManager: HidManager? = context?.let { HidManager(it) }
    private val hidService: HidService? = context?.let { HidService(it) }
    private val startTypingUseCase: StartTypingUseCase = StartTypingUseCase()
    private val scriptUseCases: ScriptUseCases? = context?.let { AppModule.provideScriptUseCasesFallback(it) }
    private val settingsUseCases: SettingsUseCases? = context?.let { SettingsUseCases(SettingsDataStore(it)) }

    init {
        hidService?.initialize()
        loadScripts()
        loadSettings()
        pushDebugLog("ViewModel initialized")
    }
    
    fun checkBluetooth() {
        val isAvailable = context?.let { BluetoothUtils.isBluetoothEnabled(it) } == true
        _state.value = if (isAvailable) "Bluetooth Available" else "Bluetooth Not Available"
        pushDebugLog("Bluetooth check: available=$isAvailable")
    }
    
    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun loadScripts() {
        val useCases = scriptUseCases ?: return
        viewModelScope.launch {
            useCases.getScripts().collect { loaded ->
                _scripts.value = loaded
            }
        }
    }

    fun addScript(name: String, content: String) {
        val useCases = scriptUseCases ?: return
        viewModelScope.launch {
            useCases.addScript(name, content)
            pushDebugLog("Script save requested: name=${name.take(24)}")
        }
    }

    fun selectScript(script: ScriptEntity) {
        _selectedScript.value = script
        _inputText.value = script.content
        pushDebugLog("Script selected: id=${script.id}")
    }

    fun selectProfile(profile: BehaviorProfile) {
        _selectedProfile.value = profile
    }

    fun selectProfileByName(name: String) {
        val profile = when (name) {
            "FAST" -> DefaultProfiles.FAST
            "SLOW" -> DefaultProfiles.SLOW
            else -> DefaultProfiles.NORMAL
        }
        _selectedProfile.value = profile
        _settingsState.value = _settingsState.value.copy(selectedProfile = name)
    }

    fun loadSettings() {
        val useCases = settingsUseCases ?: run {
            _settingsState.value = SettingsState(isLoading = false)
            return
        }
        viewModelScope.launch {
            combine(
                useCases.getProfile(),
                useCases.getTypingSpeed(),
                useCases.getTypoProbability()
            ) { profileName, speed, typo ->
                SettingsState(
                    selectedProfile = profileName,
                    typingSpeedMultiplier = speed,
                    typoProbabilityOverride = typo,
                    isLoading = false
                )
            }.collect { settings ->
                _settingsState.value = settings
                selectProfileByName(settings.selectedProfile)
            }
        }
    }

    fun saveSettings(speed: Float, typo: Float) {
        val useCases = settingsUseCases ?: return
        val selectedName = _settingsState.value.selectedProfile
        viewModelScope.launch {
            useCases.saveProfile(selectedName)
            useCases.saveTypingSpeed(speed)
            useCases.saveTypoProbability(typo)
        }
    }

    fun onBluetoothPermissionResult(granted: Boolean) {
        _bluetoothPermissionGranted.value = granted
        _state.value = if (granted) {
            "Bluetooth permission granted"
        } else {
            "Bluetooth permission required"
        }
    }

    fun initializeHid() {
        if (!_bluetoothPermissionGranted.value) {
            _state.value = "Bluetooth permission required"
            _lastError.value = "Bluetooth permission required"
            pushDebugLog("Initialize HID blocked: permission missing")
            return
        }
        val manager = hidManager
        val service = hidService
        if (manager == null || service == null) {
            _state.value = "HID unavailable"
            _lastError.value = "HID unavailable"
            pushDebugLog("Initialize HID failed: manager/service null")
            return
        }
        val initialized = manager.registerHidService(service)
        _state.value = if (initialized) "HID initialized" else "HID initialize failed"
        pushDebugLog("Initialize HID result=$initialized")
    }

    fun connectHid() {
        if (!_bluetoothPermissionGranted.value) {
            _state.value = "Bluetooth permission required"
            _lastError.value = "Bluetooth permission required"
            pushDebugLog("Connect HID blocked: permission missing")
            return
        }
        val manager = hidManager
        val service = hidService
        if (manager == null || service == null) {
            _connectionState.value = "Disconnected"
            _state.value = "HID unavailable"
            _lastError.value = "HID unavailable"
            pushDebugLog("Connect HID failed: manager/service null")
            return
        }
        val connected = manager.connectToHost(service)
        _connectionState.value = if (connected || service.isConnected) "Connected" else "Disconnected"
        _state.value = if (connected) "Connect requested" else "Connect failed"
        pushDebugLog("Connect HID result=$connected")
    }
    
    fun prepareTyping() {
        val base = _selectedProfile.value
        val settings = _settingsState.value
        val effectiveProfile = base.copy(
            minKeyDelayMs = (base.minKeyDelayMs * settings.typingSpeedMultiplier).toLong().coerceAtLeast(1L),
            maxKeyDelayMs = (base.maxKeyDelayMs * settings.typingSpeedMultiplier).toLong().coerceAtLeast(1L),
            wordPauseMinMs = (base.wordPauseMinMs * settings.typingSpeedMultiplier).toLong().coerceAtLeast(1L),
            wordPauseMaxMs = (base.wordPauseMaxMs * settings.typingSpeedMultiplier).toLong().coerceAtLeast(1L),
            sentencePauseMinMs = (base.sentencePauseMinMs * settings.typingSpeedMultiplier).toLong().coerceAtLeast(1L),
            sentencePauseMaxMs = (base.sentencePauseMaxMs * settings.typingSpeedMultiplier).toLong().coerceAtLeast(1L),
            typoProbability = if (settings.typoProbabilityOverride >= 0f) {
                settings.typoProbabilityOverride.coerceIn(0f, 1f)
            } else {
                base.typoProbability
            }
        )

        val keyEvents = startTypingUseCase.prepare(_inputText.value)
        val actions = startTypingUseCase.prepare(_inputText.value, effectiveProfile)
        _typingPreview.value = keyEvents
        _typingActions.value = actions
        _state.value = "Prepared: ${actions.size} actions"
        _lastExecutedActions.value = actions.takeLast(10).map { action ->
            when (action.type) {
                com.autotypehid.domain.usecase.TypingActionType.KEY -> {
                    val keyCode = action.keyEvent?.keyCode ?: 0
                    "KEY code=$keyCode"
                }

                com.autotypehid.domain.usecase.TypingActionType.DELAY -> {
                    val delay = action.delayMs ?: 0L
                    "DELAY ${delay}ms"
                }
            }
        }
        pushDebugLog("Prepared typing actions=${actions.size}")
    }

    fun startTypingService() {
        val appContext = context ?: return
        if (_selectedScript.value == null) {
            _executionState.value = "Select a script first"
            _lastError.value = "Selected script required"
            pushDebugLog("Start typing blocked: selected script missing")
            return
        }
        if (_typingActions.value.isEmpty()) {
            _executionState.value = "No actions to type"
            _lastError.value = "No actions to type"
            pushDebugLog("Start typing ignored: no actions")
            return
        }
        _executionState.value = "Typing..."
        TypingForegroundService.startService(appContext, _typingActions.value)
        _connectionState.value = if (hidService?.isConnected == true) "Connected" else "Disconnected"
        pushDebugLog("Foreground typing started with ${_typingActions.value.size} actions")
    }

    fun stopTypingService() {
        val appContext = context ?: return
        TypingForegroundService.stopService(appContext)
        _executionState.value = "Stopped"
        pushDebugLog("Foreground typing stopped")
    }

    private fun pushDebugLog(message: String) {
        val entry = "${System.currentTimeMillis()}: $message"
        _debugLogs.value = (_debugLogs.value + entry).takeLast(200)
        Logger.debug(TAG, message)
    }
}
