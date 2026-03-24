package com.autotypehid.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autotypehid.core.utils.PermissionManager
import com.autotypehid.domain.usecase.TypingActionType
import com.autotypehid.humanizer.profile.DefaultProfiles
import com.autotypehid.ui.screens.DebugScreen
import com.autotypehid.ui.screens.ScriptEditorScreen
import com.autotypehid.ui.screens.ScriptListScreen
import com.autotypehid.ui.screens.SettingsScreen
import com.autotypehid.ui.viewmodel.MainViewModel

private enum class AppScreen {
    LIST,
    EDITOR,
    CONTROL,
    SETTINGS,
    DEBUG
}

@Composable
fun AppRoot(viewModel: MainViewModel? = null) {
    val context = LocalContext.current
    val state = viewModel?.state?.collectAsState()?.value ?: "Ready"
    val executionState = viewModel?.executionState?.collectAsState()?.value ?: "Idle"
    val connectionState = viewModel?.connectionState?.collectAsState()?.value ?: "Disconnected"
    val bluetoothPermissionGranted = viewModel?.bluetoothPermissionGranted?.collectAsState()?.value ?: false
    val scripts = viewModel?.scripts?.collectAsState()?.value ?: emptyList()
    val selectedScript = viewModel?.selectedScript?.collectAsState()?.value
    val inputText = viewModel?.inputText?.collectAsState()?.value ?: ""
    val typingPreview = viewModel?.typingPreview?.collectAsState()?.value ?: emptyList()
    val selectedProfile = viewModel?.selectedProfile?.collectAsState()?.value ?: DefaultProfiles.NORMAL
    val settingsState = viewModel?.settingsState?.collectAsState()?.value
    val debugLogs = viewModel?.debugLogs?.collectAsState()?.value ?: emptyList()
    val lastError = viewModel?.lastError?.collectAsState()?.value
    val lastExecutedActions = viewModel?.lastExecutedActions?.collectAsState()?.value ?: emptyList()
    val typingActions = viewModel?.typingActions?.collectAsState()?.value ?: emptyList()
    val keyCount = typingActions.count { it.type == TypingActionType.KEY }
    val delayCount = typingActions.count { it.type == TypingActionType.DELAY }
    var currentScreen by remember { mutableStateOf(AppScreen.CONTROL) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        viewModel?.onBluetoothPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val granted = PermissionManager.hasPermissions(context)
        viewModel?.onBluetoothPermissionResult(granted)
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "AutoType HID - Setup Phase")
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { currentScreen = AppScreen.LIST }) {
                    Text("LIST")
                }
                OutlinedButton(onClick = { currentScreen = AppScreen.EDITOR }) {
                    Text("EDITOR")
                }
                OutlinedButton(onClick = { currentScreen = AppScreen.CONTROL }) {
                    Text("CONTROL")
                }
                OutlinedButton(onClick = { currentScreen = AppScreen.SETTINGS }) {
                    Text("SETTINGS")
                }
                OutlinedButton(onClick = { currentScreen = AppScreen.DEBUG }) {
                    Text("DEBUG")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (currentScreen) {
                AppScreen.LIST -> {
                    ScriptListScreen(
                        scripts = scripts,
                        onSelect = {
                            viewModel?.selectScript(it)
                            currentScreen = AppScreen.CONTROL
                        }
                    )
                }

                AppScreen.EDITOR -> {
                    ScriptEditorScreen(
                        onSave = { name, content ->
                            viewModel?.addScript(name, content)
                            currentScreen = AppScreen.LIST
                        }
                    )
                }

                AppScreen.CONTROL -> {
                    if (selectedScript != null) {
                        Text(text = "Selected: ${selectedScript.name}")
                    } else {
                        Text(text = "Selected: None")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        viewModel?.startTypingService()
                    }) {
                        Text("Start Typing")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        viewModel?.stopTypingService()
                    }) {
                        Text("Stop Typing")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    controlContent(
                        bluetoothPermissionGranted = bluetoothPermissionGranted,
                        state = state,
                        connectionState = connectionState,
                        executionState = executionState,
                        selectedProfile = selectedProfile,
                        inputText = inputText,
                        typingPreview = typingPreview,
                        typingActions = typingActions,
                        keyCount = keyCount,
                        delayCount = delayCount,
                        onPermissionRequest = {
                            PermissionManager.requestPermissions { permissions ->
                                if (permissions.isEmpty()) {
                                    viewModel?.onBluetoothPermissionResult(true)
                                } else {
                                    permissionLauncher.launch(permissions)
                                }
                            }
                        },
                        onCheckBluetooth = { viewModel?.checkBluetooth() },
                        onInitializeHid = { viewModel?.initializeHid() },
                        onConnectHid = { viewModel?.connectHid() },
                        onSelectNormal = { viewModel?.selectProfile(DefaultProfiles.NORMAL) },
                        onSelectFast = { viewModel?.selectProfile(DefaultProfiles.FAST) },
                        onSelectSlow = { viewModel?.selectProfile(DefaultProfiles.SLOW) },
                        onUpdateInput = { viewModel?.updateInput(it) },
                        onPrepareTyping = { viewModel?.prepareTyping() },
                        onStartTyping = { viewModel?.startTypingService() }
                    )
                }

                AppScreen.SETTINGS -> {
                    val state = settingsState
                    if (state == null) {
                        CircularProgressIndicator()
                    } else {
                        SettingsScreen(
                            settingsState = state,
                            onSelectProfile = { profile -> viewModel?.selectProfileByName(profile) },
                            onSave = { speed, typo ->
                                viewModel?.saveSettings(speed, typo)
                            }
                        )
                    }
                }

                AppScreen.DEBUG -> {
                    DebugScreen(
                        connectionState = connectionState,
                        lastActions = if (lastExecutedActions.isEmpty()) debugLogs.takeLast(10) else lastExecutedActions,
                        lastError = lastError
                    )
                }
            }
        }
    }
}

@Composable
private fun controlContent(
    bluetoothPermissionGranted: Boolean,
    state: String,
    connectionState: String,
    executionState: String,
    selectedProfile: com.autotypehid.humanizer.profile.BehaviorProfile,
    inputText: String,
    typingPreview: List<com.autotypehid.typing.mapper.KeyEvent>,
    typingActions: List<com.autotypehid.domain.usecase.TypingAction>,
    keyCount: Int,
    delayCount: Int,
    onPermissionRequest: () -> Unit,
    onCheckBluetooth: () -> Unit,
    onInitializeHid: () -> Unit,
    onConnectHid: () -> Unit,
    onSelectNormal: () -> Unit,
    onSelectFast: () -> Unit,
    onSelectSlow: () -> Unit,
    onUpdateInput: (String) -> Unit,
    onPrepareTyping: () -> Unit,
    onStartTyping: () -> Unit
) {
    Button(onClick = onCheckBluetooth) {
        Text("Check Bluetooth")
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(onClick = onPermissionRequest) {
        Text(if (bluetoothPermissionGranted) "Bluetooth Permission Granted" else "Request Bluetooth Permission")
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onInitializeHid) {
            Text("Initialize HID")
        }
        Button(onClick = onConnectHid) {
            Text("Connect")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = state)
    Text(text = "Connection: $connectionState")

    Spacer(modifier = Modifier.height(24.dp))

    Text(text = "Profile")
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onSelectNormal) {
            Text(if (selectedProfile == DefaultProfiles.NORMAL) "NORMAL *" else "NORMAL")
        }
        OutlinedButton(onClick = onSelectFast) {
            Text(if (selectedProfile == DefaultProfiles.FAST) "FAST *" else "FAST")
        }
        OutlinedButton(onClick = onSelectSlow) {
            Text(if (selectedProfile == DefaultProfiles.SLOW) "SLOW *" else "SLOW")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = inputText,
        onValueChange = onUpdateInput,
        label = { Text("Enter text") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(onClick = onPrepareTyping) {
        Text("Prepare Typing")
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(onClick = onStartTyping) {
        Text("Start Typing")
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Execution: $executionState")

    Spacer(modifier = Modifier.height(16.dp))

    if (typingPreview.isNotEmpty()) {
        Text(text = "Generated: ${typingPreview.size} key events")
        Spacer(modifier = Modifier.height(8.dp))

        val previewText = typingPreview.take(5).joinToString(", ") {
            "K:${it.keyCode}(shift:${it.requiresShift})"
        }
        Text(text = "First 5: $previewText")

        if (typingPreview.size > 5) {
            Text(text = "... and ${typingPreview.size - 5} more")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Total actions: ${typingActions.size}")
    Text(text = "Key actions: $keyCount")
    Text(text = "Delay actions: $delayCount")
}
