package com.autotypehid.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    LaunchedEffect(Unit) {
        onEvent(SplashUiEvent.OnReady)
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
                Text("Initializing AutoType HID")
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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            result[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                result[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }

        onEvent(PermissionsUiEvent.OnPermissionResult(bluetoothGranted, notificationsGranted))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Permissions") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Grant required permissions before scanning devices.")

            Button(onClick = {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                } else {
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(permissions.toTypedArray())
            }) {
                Text("Request Permissions")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScanScreen(
    state: DeviceScanUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Device Scan") }) }) { padding ->
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
    onTyping: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Dashboard") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Connection: ${state.connectionState}")
                    Text("Selected script: ${state.selectedScriptName}")
                }
            }

            Button(onClick = onTyping, enabled = state.connectionState == ConnectionState.CONNECTED) {
                Text("Start Typing")
            }
            Button(onClick = onScripts) { Text("Scripts") }
            Button(onClick = onSettings) { Text("Settings") }
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
            Text("State: ${state.typingState}")
            LinearProgressIndicator(progress = state.progress / 100f, modifier = Modifier.fillMaxWidth())
            Text("Progress: ${state.progress}%")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = state.selectedScriptContent.isNotBlank()) { Text("Start") }
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            Text("Profile")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NORMAL", "FAST", "SLOW").forEach { profile ->
                    TextButton(onClick = { onEvent(SettingsUiEvent.OnProfileChange(profile)) }) {
                        Text(if (state.profile == profile) "$profile *" else profile)
                    }
                }
            }

            Text("Typing Speed: ${"%.2f".format(state.speed)}")
            Slider(
                value = state.speed,
                valueRange = 0.5f..2.0f,
                onValueChange = { onEvent(SettingsUiEvent.OnSpeedChange(it)) }
            )

            Text("Typo Probability: ${"%.2f".format(state.typoProbability)}")
            Slider(
                value = state.typoProbability,
                valueRange = 0f..0.3f,
                onValueChange = { onEvent(SettingsUiEvent.OnTypoProbabilityChange(it)) }
            )

            Button(onClick = { onEvent(SettingsUiEvent.OnSave) }, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving..." else "Save")
            }
        }
    }
}
