package com.autotypehid.core.utils

import android.bluetooth.BluetoothManager
import android.content.Context

object BluetoothUtils {
    
    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter
        return bluetoothAdapter?.isEnabled == true
    }
}
