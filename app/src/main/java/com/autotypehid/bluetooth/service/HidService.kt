package com.autotypehid.bluetooth.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.autotypehid.core.utils.Logger
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HidService(private val context: Context) {

    companion object {
        private const val TAG = "HidService"
        private const val REPORT_ID_KEYBOARD = 1
        private const val SDP_NAME = "AutoType HID Keyboard"
        private const val SDP_DESCRIPTION = "AutoType HID"
        private const val SDP_PROVIDER = "AutoType"
        private const val SUBCLASS = BluetoothHidDevice.SUBCLASS1_COMBO
    }

    private val bluetoothManager: BluetoothManager? = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var lastKnownHost: BluetoothDevice? = null
    private var reconnectAttempts: Int = 0
    private val maxReconnectAttempts: Int = 1

    var isConnected: Boolean = false
        private set

    var isInitialized: Boolean = false
        private set

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            bluetoothHidDevice = proxy as? BluetoothHidDevice
            isInitialized = bluetoothHidDevice != null
            Logger.debug(TAG, "HID profile proxy connected: initialized=$isInitialized")
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            bluetoothHidDevice = null
            connectedHost = null
            isConnected = false
            isInitialized = false
            Logger.debug(TAG, "HID profile proxy disconnected")
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val connected = state == BluetoothProfile.STATE_CONNECTED
            isConnected = connected
            connectedHost = if (connected) device else null
            if (connected) {
                lastKnownHost = device
                reconnectAttempts = 0
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                attemptReconnect(device ?: lastKnownHost)
            }
            Logger.debug(TAG, "HID connection state changed: state=$state connected=$connected")
        }
    }

    fun initialize(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false
        return true
    }

    @SuppressLint("MissingPermission")
    fun initializeAndRegister(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false

        val proxyRequested = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        if (!proxyRequested) {
            Logger.error(TAG, "Failed to request HID profile proxy")
            return false
        }

        val hidDevice = bluetoothHidDevice
        if (hidDevice == null) {
            Logger.debug(TAG, "HID proxy requested; awaiting connection callback")
            return true
        }

        return registerApp(hidDevice)
    }

    @SuppressLint("MissingPermission")
    fun ensureRegistered(): Boolean {
        val hidDevice = bluetoothHidDevice ?: return false
        return registerApp(hidDevice)
    }

    @SuppressLint("MissingPermission")
    private fun registerApp(hidDevice: BluetoothHidDevice): Boolean {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            SDP_NAME,
            SDP_DESCRIPTION,
            SDP_PROVIDER,
            SUBCLASS,
            KEYBOARD_HID_DESCRIPTOR
        )

        val inQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        val registered = hidDevice.registerApp(sdp, inQos, outQos, executor, hidCallback)
        isInitialized = registered
        Logger.debug(TAG, "registerApp result=$registered")
        return registered
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice?): Boolean {
        val host = device ?: return false
        val hidDevice = bluetoothHidDevice ?: return false
        lastKnownHost = host
        val result = hidDevice.connect(host)
        if (result) reconnectAttempts = 0
        Logger.debug(TAG, "connect result=$result")
        return result
    }

    @SuppressLint("MissingPermission")
    private fun attemptReconnect(device: BluetoothDevice?) {
        val host = device ?: return
        val hidDevice = bluetoothHidDevice ?: return
        if (reconnectAttempts >= maxReconnectAttempts) return
        reconnectAttempts += 1
        val result = hidDevice.connect(host)
        Logger.debug(TAG, "reconnect attempt=$reconnectAttempts result=$result")
    }

    @SuppressLint("MissingPermission")
    fun sendReport(report: ByteArray): Boolean {
        val hidDevice = bluetoothHidDevice ?: return false
        val host = connectedHost ?: return false
        return hidDevice.sendReport(host, REPORT_ID_KEYBOARD, report)
    }

    fun buildKeyboardReport(modifier: Byte, keyCode: Byte): ByteArray {
        return byteArrayOf(
            modifier,
            0x00,
            keyCode,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
    }

    fun buildEmptyKeyboardReport(): ByteArray {
        return ByteArray(8)
    }

    @SuppressLint("MissingPermission")
    fun close() {
        val adapter = bluetoothAdapter ?: return
        val hidDevice = bluetoothHidDevice
        if (hidDevice != null) {
            runCatching { hidDevice.unregisterApp() }
            adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        }
        bluetoothHidDevice = null
        connectedHost = null
        lastKnownHost = null
        reconnectAttempts = 0
        isConnected = false
        isInitialized = false
    }

    private val KEYBOARD_HID_DESCRIPTOR = intArrayOf(
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
}
