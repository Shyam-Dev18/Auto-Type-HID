package com.autotypehid.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autotypehid.presentation.screens.DashboardScreen
import com.autotypehid.presentation.screens.DeviceScanScreen
import com.autotypehid.presentation.screens.PermissionsScreen
import com.autotypehid.presentation.screens.ScriptEditorScreen
import com.autotypehid.presentation.screens.ScriptsListScreen
import com.autotypehid.presentation.screens.SettingsScreen
import com.autotypehid.presentation.screens.SplashScreen
import com.autotypehid.presentation.screens.TypingControlScreen
import com.autotypehid.presentation.state.DashboardUiEvent
import com.autotypehid.presentation.state.DeviceScanUiEvent
import com.autotypehid.presentation.state.PermissionsUiEvent
import com.autotypehid.presentation.state.ScriptsListUiEvent
import com.autotypehid.presentation.state.TypingControlUiEvent
import com.autotypehid.presentation.viewmodel.DashboardViewModel
import com.autotypehid.presentation.viewmodel.DeviceScanViewModel
import com.autotypehid.presentation.viewmodel.PermissionsViewModel
import com.autotypehid.presentation.viewmodel.ScriptEditorViewModel
import com.autotypehid.presentation.viewmodel.ScriptsListViewModel
import com.autotypehid.presentation.viewmodel.SettingsViewModel
import com.autotypehid.presentation.viewmodel.SplashViewModel
import com.autotypehid.presentation.viewmodel.TypingControlViewModel

@Composable
fun AutoTypeNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.INIT
    ) {
        composable(Routes.INIT) {
            val vm: SplashViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(vm) {
                vm.navigation.collect { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.INIT) { inclusive = true }
                    }
                }
            }

            SplashScreen(
                state = uiState,
                onEvent = vm::onEvent
            )
        }

        composable(Routes.PERMISSIONS) {
            val vm: PermissionsViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(vm) {
                vm.navigation.collect { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            }

            PermissionsScreen(
                state = uiState,
                onEvent = vm::onEvent
            )
        }

        composable(Routes.DEVICE_SCAN) {
            val vm: DeviceScanViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            DeviceScanScreen(
                state = uiState,
                onStartScan = { vm.onEvent(DeviceScanUiEvent.OnStartScan) },
                onStopScan = { vm.onEvent(DeviceScanUiEvent.OnStopScan) },
                onConnect = { address -> vm.onEvent(DeviceScanUiEvent.OnConnect(address)) },
                onDisconnect = { vm.onEvent(DeviceScanUiEvent.OnDisconnect) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(vm) {
                vm.navigation.collect { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }

            DashboardScreen(
                state = uiState,
                onScripts = { vm.onEvent(DashboardUiEvent.OnScriptsClick) },
                onSettings = { vm.onEvent(DashboardUiEvent.OnSettingsClick) },
                onTyping = { vm.onEvent(DashboardUiEvent.OnTypingClick) },
                onReconnect = { vm.onEvent(DashboardUiEvent.OnReconnectClick) },
                onAddDevice = { vm.onEvent(DashboardUiEvent.OnManageDeviceClick) },
                onBluetoothIconClick = { vm.onEvent(DashboardUiEvent.OnBluetoothIconClick) },
                onSavedDeviceClick = { address -> vm.onEvent(DashboardUiEvent.OnSavedDeviceClick(address)) },
                onDeleteSavedDeviceClick = { address -> vm.onEvent(DashboardUiEvent.OnDeleteSavedDeviceClick(address)) }
            )
        }

        composable(Routes.SCRIPTS_LIST) {
            val vm: ScriptsListViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(vm) {
                vm.navigation.collect { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }

            ScriptsListScreen(
                state = uiState,
                onCreate = { vm.onEvent(ScriptsListUiEvent.OnCreateNew) },
                onEdit = { id -> vm.onEvent(ScriptsListUiEvent.OnEdit(id)) },
                onDelete = { id -> vm.onEvent(ScriptsListUiEvent.OnDelete(id)) },
                onSelect = { script -> vm.onEvent(ScriptsListUiEvent.OnSelect(script)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SCRIPT_EDITOR_WITH_ARG,
            arguments = listOf(
                navArgument(Routes.SCRIPT_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = "-1"
                }
            )
        ) {
            val vm: ScriptEditorViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(vm) {
                vm.navigation.collect { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.SCRIPT_EDITOR) { inclusive = true }
                    }
                }
            }

            ScriptEditorScreen(
                state = uiState,
                onEvent = vm::onEvent
            )
        }

        composable(Routes.TYPING_CONTROL) {
            val vm: TypingControlViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            TypingControlScreen(
                state = uiState,
                onStart = { vm.onEvent(TypingControlUiEvent.OnStart) },
                onPauseOrResume = { vm.onEvent(TypingControlUiEvent.OnPauseOrResume) },
                onStop = { vm.onEvent(TypingControlUiEvent.OnStop) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel()
            val uiState = vm.uiState.collectAsStateWithLifecycle().value

            SettingsScreen(
                state = uiState,
                onEvent = vm::onEvent,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
