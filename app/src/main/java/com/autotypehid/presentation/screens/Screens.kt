package com.autotypehid.presentation.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Settings
import com.autotypehid.domain.model.BluetoothAdapterState
import com.autotypehid.domain.model.ConnectionState
import com.autotypehid.domain.model.Script
import com.autotypehid.domain.model.TypingState
import com.autotypehid.presentation.state.DashboardUiState
import com.autotypehid.presentation.state.DeviceScanUiState
import com.autotypehid.presentation.state.PermissionsUiEvent
import com.autotypehid.presentation.state.PermissionsUiState
import com.autotypehid.presentation.state.ScriptEditorUiEvent
import com.autotypehid.presentation.state.ScriptEditorUiState
import com.autotypehid.presentation.state.ScriptsListUiState
import com.autotypehid.presentation.state.SettingsUiEvent
import com.autotypehid.presentation.state.SettingsUiState
import com.autotypehid.presentation.state.SplashUiEvent
import com.autotypehid.presentation.state.SplashUiState
import com.autotypehid.presentation.state.TypingControlUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashScreen(
    state: SplashUiState,
    onEvent: (SplashUiEvent) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onEvent(
            SplashUiEvent.OnReady(
                bluetoothGranted = isBluetoothPermissionGranted(context),
                notificationsGranted = isNotificationPermissionGranted(context)
            )
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Splash") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Initializing Auto Type HID")
            } else {
                Text("Ready")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onEvent: (PermissionsUiEvent) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val bluetoothGranted = isBluetoothPermissionGranted(context)
        val notificationsGranted = isNotificationPermissionGranted(context)

        onEvent(PermissionsUiEvent.OnPermissionResult(bluetoothGranted, notificationsGranted))
    }

    LaunchedEffect(Unit) {
        onEvent(
            PermissionsUiEvent.OnPermissionResult(
                bluetoothGranted = isBluetoothPermissionGranted(context),
                notificationsGranted = isNotificationPermissionGranted(context)
            )
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Permissions") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Auto Type HID needs Bluetooth and Location permissions to discover and connect to HID hosts.")
            Text("Location is required by Android for Bluetooth scanning and is not used for location tracking.")

            Button(onClick = {
                val pendingPermissions = buildMissingPermissions(context)
                if (pendingPermissions.isEmpty()) {
                    onEvent(
                        PermissionsUiEvent.OnPermissionResult(
                            bluetoothGranted = true,
                            notificationsGranted = true
                        )
                    )
                } else {
                    launcher.launch(pendingPermissions.toTypedArray())
                }
            }) {
                Text("Grant Permissions")
            }

            Text("Bluetooth: ${if (state.bluetoothGranted) "Granted" else "Missing"}")
            Text("Notifications: ${if (state.notificationsGranted) "Granted" else "Missing"}")

            state.error?.let { Text(it) }

            Button(onClick = { onEvent(PermissionsUiEvent.OnContinue) }, enabled = state.canContinue) {
                Text("Continue")
            }
        }
    }
}

private fun buildMissingPermissions(context: android.content.Context): List<String> {
    val required = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        required.add(Manifest.permission.BLUETOOTH_CONNECT)
        required.add(Manifest.permission.BLUETOOTH_SCAN)
        required.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    required.add(Manifest.permission.ACCESS_FINE_LOCATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        required.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    return required.filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun isBluetoothPermissionGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScanScreen(
    state: DeviceScanUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Scan") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartScan, enabled = !state.isScanning) { Text("Start Scan") }
                Button(onClick = onStopScan, enabled = state.isScanning) { Text("Stop") }
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }

            Text("Connection: ${state.connectionState}")

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.devices, key = { it.address }) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnect(device.address) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(device.name)
                            Text(device.address)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onScripts: () -> Unit,
    onSettings: () -> Unit,
    onTyping: () -> Unit,
    onReconnect: () -> Unit,
    onAddDevice: () -> Unit,
    onBluetoothIconClick: () -> Unit,
    onSavedDeviceClick: (String) -> Unit,
    onDeleteSavedDeviceClick: (String) -> Unit
) {
    val statusAlpha = animateFloatAsState(
        targetValue = if (state.connectionState == ConnectionState.CONNECTED) 1f else 0.85f,
        animationSpec = tween(durationMillis = 350),
        label = "homeStatusAlpha"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDevice) {
                Icon(Icons.Default.Add, contentDescription = "Add device")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onBluetoothIconClick) {
                        val iconRes = if (state.bluetoothState == BluetoothAdapterState.ON) {
                            android.R.drawable.stat_sys_data_bluetooth
                        } else {
                            android.R.drawable.stat_notify_error
                        }
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Bluetooth"
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReconnect, enabled = state.lastConnectedAddress != null) {
                    Text("Reconnect")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(statusAlpha.value),
                shape = CardDefaults.shape
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Bluetooth: ${state.bluetoothState}")
                    Text("Connection: ${state.connectionState}")
                    Text("Device: ${state.connectedDeviceName}")
                    Text("Last device: ${state.lastConnectedAddress ?: "None"}")
                    Text("Selected script: ${state.selectedScriptName}")
                }
            }

            Text(
                text = if (state.connectionState == ConnectionState.CONNECTED) {
                    "Connected and ready to type"
                } else {
                    "Disconnected. Select a saved device or add a new one."
                }
            )

            Text("Use Scripts to choose content, then Start Typing to begin.")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onScripts
                ) {
                    Text("Scripts")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onTyping,
                    enabled = state.connectionState == ConnectionState.CONNECTED
                ) {
                    Text("Start Typing")
                }
            }

            if (state.bluetoothState != BluetoothAdapterState.ON) {
                Text("Bluetooth is OFF or unavailable. Turn it ON to reconnect.")
            }

            if (state.connectionState != ConnectionState.CONNECTED) {
                Text("Disconnected. Reconnect to resume typing.")
            }

            Text("Saved Devices")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.savedDevices, key = { it.address }) { device ->
                    val status = when {
                        state.connectionState == ConnectionState.CONNECTED && state.connectedDeviceAddress == device.address -> "Connected"
                        state.pendingDeviceAddress == device.address && state.connectionState == ConnectionState.CONNECTING -> "Connecting"
                        state.failedDeviceAddress == device.address -> "Failed"
                        else -> "Idle"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSavedDeviceClick(device.address) },
                        shape = CardDefaults.shape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(device.name)
                                Text(status)
                            }
                            FilledTonalIconButton(onClick = { onDeleteSavedDeviceClick(device.address) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete saved device")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsListScreen(
    state: ScriptsListUiState,
    onCreate: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onSelect: (Script) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scripts") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Text("+")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.scripts, key = { it.id }) { script ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(script.name)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onSelect(script) }) { Text("Select") }
                                TextButton(onClick = { onEdit(script.id) }) { Text("Edit") }
                                TextButton(onClick = { onDelete(script.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    state: ScriptEditorUiState,
    onEvent: (ScriptEditorUiEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Script Editor") },
                navigationIcon = { TextButton(onClick = { onEvent(ScriptEditorUiEvent.OnBack) }) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(ScriptEditorUiEvent.OnNameChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") }
            )

            OutlinedTextField(
                value = state.content,
                onValueChange = { onEvent(ScriptEditorUiEvent.OnContentChange(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Content") }
            )

            state.error?.let { Text(it) }

            Button(onClick = { onEvent(ScriptEditorUiEvent.OnSave) }, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving..." else "Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingControlScreen(
    state: TypingControlUiState,
    onStart: () -> Unit,
    onPauseOrResume: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Typing Control") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Script: ${state.selectedScriptName}")
            Text("Bluetooth: ${state.bluetoothState}")
            Text("Connection: ${state.connectionState}")
            Text("State: ${state.typingState}")
            LinearProgressIndicator(progress = state.progress / 100f, modifier = Modifier.fillMaxWidth())
            Text("Progress: ${state.progress}%")

            if (state.connectionState != ConnectionState.CONNECTED) {
                Text("Disconnected. Reconnect from Home before starting typing.")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    enabled = state.selectedScriptContent.isNotBlank() && state.connectionState == ConnectionState.CONNECTED
                ) { Text("Start") }
                Button(
                    onClick = onPauseOrResume,
                    enabled = state.typingState == TypingState.RUNNING || state.typingState == TypingState.PAUSED
                ) {
                    Text(if (state.typingState == TypingState.PAUSED) "Resume" else "Pause")
                }
                Button(onClick = onStop) { Text("Stop") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    if (state.showInfoDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.OnDismissHelp) },
            title = { Text("Auto Type HID") },
            text = { Text("Bluetooth HID typing app. Contact support: dev@autotypehid.app") },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.OnDismissHelp) }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = { onEvent(SettingsUiEvent.OnHelpClick) }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_help),
                            contentDescription = "Help"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Typing Behavior")
            Text("Typing Speed: ${"%.2f".format(state.speed)}")
            Slider(
                value = state.speed,
                valueRange = 0.5f..2.0f,
                onValueChange = { onEvent(SettingsUiEvent.OnSpeedChange(it)) }
            )

            Text("Typo Probability: ${"%.2f".format(state.typoProbability)}")
            Slider(
                value = state.typoProbability,
                valueRange = 0f..0.35f,
                onValueChange = { onEvent(SettingsUiEvent.OnTypoProbabilityChange(it)) }
            )

            Text("Word Gap Delay: ${state.wordGapMs} ms")
            Slider(
                value = state.wordGapMs.toFloat(),
                valueRange = 0f..300f,
                onValueChange = { onEvent(SettingsUiEvent.OnWordGapChange(it)) }
            )

            Text("Jitter Randomization: ${state.jitterPercent}%")
            Slider(
                value = state.jitterPercent.toFloat(),
                valueRange = 0f..80f,
                onValueChange = { onEvent(SettingsUiEvent.OnJitterChange(it)) }
            )

            Text("Profiles")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NORMAL", "FAST", "SLOW").forEach { profile ->
                    TextButton(onClick = { onEvent(SettingsUiEvent.OnProfileChange(profile)) }) {
                        Text(if (state.profile == profile) "$profile *" else profile)
                    }
                }
            }

            if (state.profile == "CUSTOM") {
                Text("Current profile: CUSTOM")
            }

            Text("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                    TextButton(onClick = { onEvent(SettingsUiEvent.OnThemeModeChange(mode)) }) {
                        Text(if (state.themeMode == mode) "$mode *" else mode)
                    }
                }
            }

            Button(onClick = { onEvent(SettingsUiEvent.OnSave) }, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving..." else "Save")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("App Info")
                    Text("Auto Type HID")
                    Text("Bluetooth HID typing with script-based automation.")
                    Text("Version: 1.1.0")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Developer")
                    Text("Name: AutoType Team")
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:dev@autotypehid.app")
                            putExtra(Intent.EXTRA_SUBJECT, "Auto Type HID Support")
                        }
                        context.startActivity(intent)
                    }) {
                        Text("dev@autotypehid.app")
                    }
                }
            }
        }
    }
}
