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
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.autotypehid.data.local.KnownDevicesStore
import com.autotypehid.domain.model.BluetoothAdapterState
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

    private val _bluetoothState = MutableStateFlow(readBluetoothAdapterState())
    val bluetoothState: StateFlow<BluetoothAdapterState> = _bluetoothState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<ScannedDevice?>(null)
    val connectedDevice: StateFlow<ScannedDevice?> = _connectedDevice.asStateFlow()

    private val knownDevicesStore = KnownDevicesStore(context)
    private val _savedDevices = MutableStateFlow(knownDevicesStore.readKnownDevices())
    val savedDevices: StateFlow<List<ScannedDevice>> = _savedDevices.asStateFlow()

    private val _lastConnectedAddress = MutableStateFlow(knownDevicesStore.readLastConnectedAddress())
    val lastConnectedAddress: StateFlow<String?> = _lastConnectedAddress.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    private var appRegistered = false
    private var pendingHostAddress: String? = null
    private var connectedHost: BluetoothDevice? = null
    private var scanReceiverRegistered = false
    private var bluetoothStateReceiverRegistered = false
    private val discoveredDevices = linkedMapOf<String, ScannedDevice>()

    init {
        registerBluetoothStateReceiverIfNeeded()
    }

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

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return

            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            _bluetoothState.value = mapAdapterState(state)

            if (_bluetoothState.value == BluetoothAdapterState.OFF || _bluetoothState.value == BluetoothAdapterState.UNAVAILABLE) {
                connectedHost = null
                _connectedDevice.value = null
                _isScanning.value = false
                _connectionState.value = ConnectionState.DISCONNECTED
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
                    val connected = ScannedDevice(
                        name = device.name ?: "Unknown Device",
                        address = device.address
                    )
                    _connectedDevice.value = connected
                    knownDevicesStore.saveConnection(connected)
                    _savedDevices.value = knownDevicesStore.readKnownDevices()
                    _lastConnectedAddress.value = knownDevicesStore.readLastConnectedAddress()
                    _connectionState.value = ConnectionState.CONNECTED
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedHost?.address == device.address) {
                        connectedHost = null
                    }
                    _connectedDevice.value = null
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
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun reconnectLastDevice() {
        val address = _lastConnectedAddress.value
        if (address.isNullOrBlank()) {
            _connectionState.value = ConnectionState.ERROR
            return
        }
        connect(address)
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun requestEnableBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun handleBluetoothIconAction() {
        when (_bluetoothState.value) {
            BluetoothAdapterState.OFF,
            BluetoothAdapterState.TURNING_OFF,
            BluetoothAdapterState.UNAVAILABLE -> requestEnableBluetooth()

            BluetoothAdapterState.ON,
            BluetoothAdapterState.TURNING_ON -> openBluetoothSettings()
        }
    }

    fun removeSavedDevice(address: String) {
        knownDevicesStore.removeDevice(address)
        _savedDevices.value = knownDevicesStore.readKnownDevices()
        _lastConnectedAddress.value = knownDevicesStore.readLastConnectedAddress()

        if (_connectedDevice.value?.address == address) {
            disconnect()
        }
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

    fun computeAggressiveDelay(
        speedMultiplier: Float,
        wordGapMs: Int,
        jitterPercent: Int,
        isWordGap: Boolean
    ): Long {
        val safeSpeed = speedMultiplier.coerceIn(0.5f, 2.5f)
        val base = (110f / safeSpeed).toLong()
        val wordGap = if (isWordGap) wordGapMs.coerceIn(0, 1000).toLong() else 0L
        val raw = (base + wordGap).coerceAtLeast(20L)
        val jitterRatio = jitterPercent.coerceIn(0, 80) / 100f
        val jitterRange = (raw * jitterRatio).toLong().coerceAtLeast(1L)
        val randomDelta = Random.nextLong(-jitterRange, jitterRange + 1L)
        return (raw + randomDelta).coerceIn(15L, 2000L)
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

    private fun registerBluetoothStateReceiverIfNeeded() {
        if (bluetoothStateReceiverRegistered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
        bluetoothStateReceiverRegistered = true
    }

    private fun readBluetoothAdapterState(): BluetoothAdapterState {
        val btAdapter = adapter ?: return BluetoothAdapterState.UNAVAILABLE
        return mapAdapterState(btAdapter.state)
    }

    private fun mapAdapterState(state: Int): BluetoothAdapterState {
        return when (state) {
            BluetoothAdapter.STATE_ON -> BluetoothAdapterState.ON
            BluetoothAdapter.STATE_OFF -> BluetoothAdapterState.OFF
            BluetoothAdapter.STATE_TURNING_ON -> BluetoothAdapterState.TURNING_ON
            BluetoothAdapter.STATE_TURNING_OFF -> BluetoothAdapterState.TURNING_OFF
            else -> BluetoothAdapterState.UNAVAILABLE
        }
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
            "ZEBRONICS 76R4 Keyboard",
            "Boot Keyboard HID",
            "ZEBRONICS",
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
