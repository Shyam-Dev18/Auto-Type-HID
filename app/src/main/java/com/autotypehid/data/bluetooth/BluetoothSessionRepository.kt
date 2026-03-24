package com.autotypehid.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.autotypehid.domain.model.ConnectionState
import com.autotypehid.domain.model.ScannedDevice
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothSessionRepository(
    private val context: Context
) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val callbackExecutor: Executor = Executors.newSingleThreadExecutor()

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    private var appRegistered = false
    private var pendingHostAddress: String? = null
    private var connectedHost: BluetoothDevice? = null
    private var scanReceiverRegistered = false
    private val discoveredDevices = linkedMapOf<String, ScannedDevice>()

    private val scanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return

                    val address = device.address ?: return
                    val name = device.name ?: "Unknown Device"
                    discoveredDevices[address] = ScannedDevice(name = name, address = address)
                    _devices.value = discoveredDevices.values.toList().sortedBy { it.name.lowercase() }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    if (_connectionState.value == ConnectionState.SCANNING) {
                        _connectionState.value = if (connectedHost != null) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            registerAppIfNeeded()
            connectPendingHostIfReady()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            appRegistered = false
            connectedHost = null
            if (_connectionState.value != ConnectionState.ERROR) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            if (!registered) {
                _connectionState.value = ConnectionState.ERROR
                return
            }
            connectPendingHostIfReady()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _connectionState.value = ConnectionState.CONNECTED
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedHost?.address == device.address) {
                        connectedHost = null
                    }
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                else -> Unit
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!canScan()) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        val btAdapter = adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        registerScanReceiverIfNeeded()

        if (btAdapter.isDiscovering) {
            btAdapter.cancelDiscovery()
        }

        _isScanning.value = true
        _connectionState.value = ConnectionState.SCANNING

        discoveredDevices.clear()
        btAdapter.bondedDevices
            ?.forEach { bonded ->
                discoveredDevices[bonded.address] = ScannedDevice(
                    name = bonded.name ?: "Unknown Device",
                    address = bonded.address
                )
            }
        _devices.value = discoveredDevices.values.toList().sortedBy { it.name.lowercase() }

        val discoveryStarted = btAdapter.startDiscovery()
        if (!discoveryStarted) {
            _isScanning.value = false
            _connectionState.value = ConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter?.takeIf { it.isDiscovering }?.cancelDiscovery()
        _isScanning.value = false
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = if (connectedHost != null) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (!isHidDeviceSupported()) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        if (!hasConnectPermission()) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        val host = findBondedDevice(address)
        if (host == null) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        pendingHostAddress = host.address
        _connectionState.value = ConnectionState.CONNECTING
        ensureProfileProxy()
        connectPendingHostIfReady()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        pendingHostAddress = null
        val host = connectedHost
        val hid = hidDevice
        if (host != null && hid != null) {
            hid.disconnect(host)
        }
        connectedHost = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    fun isHidDeviceSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && adapter != null
    }

    fun isTypingReady(): Boolean {
        return isConnected() && connectedHost != null && hidDevice != null
    }

    @SuppressLint("MissingPermission")
    fun sendCharacter(char: Char): Boolean {
        val key = HidKeyMapper.mapChar(char) ?: return false
        return sendKeyStroke(key)
    }

    @SuppressLint("MissingPermission")
    fun sendBackspace(): Boolean {
        return sendKeyStroke(HidKeyStroke(keyCode = 0x2A))
    }

    fun computeAggressiveDelay(speedMultiplier: Float): Long {
        val safeSpeed = speedMultiplier.coerceIn(0.5f, 2.5f)
        val base = (110f / safeSpeed).toLong()
        val jitter = Random.nextLong(25L, 140L)
        return (base + jitter).coerceAtMost(320L)
    }

    private fun canScan(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasConnectPermission() &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun findBondedDevice(address: String): BluetoothDevice? {
        val bonded = adapter?.bondedDevices?.firstOrNull { it.address == address }
        if (bonded != null) return bonded

        return runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
    }

    private fun registerScanReceiverIfNeeded() {
        if (scanReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(scanReceiver, filter)
        scanReceiverRegistered = true
    }

    private fun ensureProfileProxy() {
        if (hidDevice != null) {
            registerAppIfNeeded()
            return
        }
        adapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    private fun registerAppIfNeeded() {
        val hid = hidDevice ?: return
        if (appRegistered) return

        val descriptor = intArrayOf(
            0x05, 0x01,
            0x09, 0x06,
            0xA1, 0x01,
            0x05, 0x07,
            0x19, 0xE0,
            0x29, 0xE7,
            0x15, 0x00,
            0x25, 0x01,
            0x75, 0x01,
            0x95, 0x08,
            0x81, 0x02,
            0x95, 0x01,
            0x75, 0x08,
            0x81, 0x01,
            0x95, 0x05,
            0x75, 0x01,
            0x05, 0x08,
            0x19, 0x01,
            0x29, 0x05,
            0x91, 0x02,
            0x95, 0x01,
            0x75, 0x03,
            0x91, 0x01,
            0x95, 0x06,
            0x75, 0x08,
            0x15, 0x00,
            0x25, 0x65,
            0x05, 0x07,
            0x19, 0x00,
            0x29, 0x65,
            0x81, 0x00,
            0xC0
        ).map { it.toByte() }.toByteArray()

        val sdp = BluetoothHidDeviceAppSdpSettings(
            "AutoTypeHID",
            "Bluetooth keyboard typing",
            "AutoTypeHID",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            descriptor
        )

        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_GUARANTEED,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        hid.registerApp(sdp, null, qos, callbackExecutor, hidCallback)
    }

    @SuppressLint("MissingPermission")
    private fun connectPendingHostIfReady() {
        val hid = hidDevice ?: return
        if (!appRegistered) return
        val pendingAddress = pendingHostAddress ?: return
        val host = findBondedDevice(pendingAddress)
        if (host == null) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        if (!hid.connect(host)) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
    }

    @SuppressLint("MissingPermission")
    private fun sendKeyStroke(stroke: HidKeyStroke): Boolean {
        val host = connectedHost ?: return false
        val hid = hidDevice ?: return false

        val pressReport = byteArrayOf(
            stroke.modifier,
            0x00,
            stroke.keyCode,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )

        val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        val pressed = hid.sendReport(host, 1, pressReport)
        val released = hid.sendReport(host, 1, releaseReport)
        return pressed && released
    }
}
