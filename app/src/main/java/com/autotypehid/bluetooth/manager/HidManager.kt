package com.autotypehid.bluetooth.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.autotypehid.bluetooth.service.HidService

class HidManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun registerHidService(hidService: HidService): Boolean {
        return hidService.initializeAndRegister()
    }

    @SuppressLint("MissingPermission")
    fun connectToHost(hidService: HidService): Boolean {
        val host = findPreferredHost() ?: return false
        return hidService.connect(host)
    }

    @SuppressLint("MissingPermission")
    private fun findPreferredHost(): BluetoothDevice? {
        val adapter = bluetoothAdapter ?: return null
        val bonded = adapter.bondedDevices ?: return null
        return bonded.firstOrNull()
    }
}
