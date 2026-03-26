package com.autotypehid.core.managers

import android.content.Context
import androidx.room.Room
import com.autotypehid.data.bluetooth.BluetoothSessionRepository
import com.autotypehid.data.local.AppDatabase
import com.autotypehid.data.local.SettingsDataStore
import com.autotypehid.data.repository.ScriptRepository
import com.autotypehid.data.repository.ScriptsRepository
import com.autotypehid.data.repository.SettingsRepository
import com.autotypehid.domain.usecase.ConnectDeviceUseCase
import com.autotypehid.domain.usecase.ControlTypingUseCase
import com.autotypehid.domain.usecase.DeleteScriptUseCase
import com.autotypehid.domain.usecase.DisconnectDeviceUseCase
import com.autotypehid.domain.usecase.LoadScriptUseCase
import com.autotypehid.domain.usecase.ObserveBluetoothDevicesUseCase
import com.autotypehid.domain.usecase.ObserveBluetoothStateUseCase
import com.autotypehid.domain.usecase.ObserveConnectionStateUseCase
import com.autotypehid.domain.usecase.ObserveConnectedDeviceUseCase
import com.autotypehid.domain.usecase.ObserveIsScanningUseCase
import com.autotypehid.domain.usecase.ObserveLastConnectedAddressUseCase
import com.autotypehid.domain.usecase.ObserveSavedDevicesUseCase
import com.autotypehid.domain.usecase.ObserveScriptsUseCase
import com.autotypehid.domain.usecase.ObserveSelectedScriptUseCase
import com.autotypehid.domain.usecase.ObserveSettingsUseCase
import com.autotypehid.domain.usecase.ObserveTypingProgressUseCase
import com.autotypehid.domain.usecase.ObserveTypingStateUseCase
import com.autotypehid.domain.usecase.OpenBluetoothSettingsUseCase
import com.autotypehid.domain.usecase.ReconnectLastDeviceUseCase
import com.autotypehid.domain.usecase.SaveScriptUseCase
import com.autotypehid.domain.usecase.SelectScriptUseCase
import com.autotypehid.domain.usecase.StartDeviceScanUseCase
import com.autotypehid.domain.usecase.StopDeviceScanUseCase
import com.autotypehid.domain.usecase.UpdateSettingsUseCase

object AppContainer {
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "autotype_scripts.db"
        ).fallbackToDestructiveMigration().build()
    }

    private val scriptRepository by lazy {
        ScriptRepository(database.scriptDao())
    }

    private val settingsDataStore by lazy {
        SettingsDataStore(appContext)
    }

    private val scriptsRepository by lazy {
        ScriptsRepository(scriptRepository)
    }

    private val settingsRepository by lazy {
        SettingsRepository(settingsDataStore)
    }

    private val bluetoothSessionRepository by lazy {
        BluetoothSessionRepository(appContext)
    }

    val bluetoothRepository: BluetoothSessionRepository
        get() = bluetoothSessionRepository

    val observeScriptsUseCase by lazy { ObserveScriptsUseCase(scriptsRepository) }
    val saveScriptUseCase by lazy { SaveScriptUseCase(scriptsRepository) }
    val deleteScriptUseCase by lazy { DeleteScriptUseCase(scriptsRepository) }
    val loadScriptUseCase by lazy { LoadScriptUseCase(scriptsRepository) }
    val selectScriptUseCase by lazy { SelectScriptUseCase(scriptsRepository) }
    val observeSelectedScriptUseCase by lazy { ObserveSelectedScriptUseCase(scriptsRepository) }

    val observeSettingsUseCase by lazy { ObserveSettingsUseCase(settingsRepository) }
    val updateSettingsUseCase by lazy { UpdateSettingsUseCase(settingsRepository) }

    val observeBluetoothDevicesUseCase by lazy { ObserveBluetoothDevicesUseCase(bluetoothSessionRepository) }
    val observeBluetoothStateUseCase by lazy { ObserveBluetoothStateUseCase(bluetoothSessionRepository) }
    val observeConnectionStateUseCase by lazy { ObserveConnectionStateUseCase(bluetoothSessionRepository) }
    val observeConnectedDeviceUseCase by lazy { ObserveConnectedDeviceUseCase(bluetoothSessionRepository) }
    val observeIsScanningUseCase by lazy { ObserveIsScanningUseCase(bluetoothSessionRepository) }
    val observeSavedDevicesUseCase by lazy { ObserveSavedDevicesUseCase(bluetoothSessionRepository) }
    val observeLastConnectedAddressUseCase by lazy { ObserveLastConnectedAddressUseCase(bluetoothSessionRepository) }
    val startDeviceScanUseCase by lazy { StartDeviceScanUseCase(bluetoothSessionRepository) }
    val stopDeviceScanUseCase by lazy { StopDeviceScanUseCase(bluetoothSessionRepository) }
    val connectDeviceUseCase by lazy { ConnectDeviceUseCase(bluetoothSessionRepository) }
    val disconnectDeviceUseCase by lazy { DisconnectDeviceUseCase(bluetoothSessionRepository) }
    val reconnectLastDeviceUseCase by lazy { ReconnectLastDeviceUseCase(bluetoothSessionRepository) }
    val openBluetoothSettingsUseCase by lazy { OpenBluetoothSettingsUseCase(bluetoothSessionRepository) }

    val observeTypingStateUseCase by lazy { ObserveTypingStateUseCase() }
    val observeTypingProgressUseCase by lazy { ObserveTypingProgressUseCase() }
    val controlTypingUseCase by lazy { ControlTypingUseCase(appContext) }
}
