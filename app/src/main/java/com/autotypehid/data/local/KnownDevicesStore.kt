package com.autotypehid.data.local

import android.content.Context
import com.autotypehid.domain.model.ScannedDevice

class KnownDevicesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readKnownDevices(): List<ScannedDevice> {
        val raw = prefs.getString(KEY_KNOWN_DEVICES, "").orEmpty()
        if (raw.isBlank()) return emptyList()

        return raw.split("\n")
            .mapNotNull { row ->
                val parts = row.split("\t")
                if (parts.size != 2) return@mapNotNull null
                val name = parts[0].trim()
                val address = parts[1].trim()
                if (name.isBlank() || address.isBlank()) return@mapNotNull null
                ScannedDevice(name = name, address = address)
            }
    }

    fun readLastConnectedAddress(): String? {
        return prefs.getString(KEY_LAST_CONNECTED_ADDRESS, null)
    }

    fun saveConnection(device: ScannedDevice) {
        val existing = readKnownDevices().associateBy { it.address }.toMutableMap()
        existing[device.address] = device

        val encoded = existing.values
            .sortedBy { it.name.lowercase() }
            .joinToString("\n") { known -> "${known.name}\t${known.address}" }

        prefs.edit()
            .putString(KEY_KNOWN_DEVICES, encoded)
            .putString(KEY_LAST_CONNECTED_ADDRESS, device.address)
            .apply()
    }

    fun removeDevice(address: String) {
        val existing = readKnownDevices()
            .filterNot { it.address == address }
            .sortedBy { it.name.lowercase() }
        val encoded = existing.joinToString("\n") { known -> "${known.name}\t${known.address}" }

        val editor = prefs.edit().putString(KEY_KNOWN_DEVICES, encoded)
        if (readLastConnectedAddress() == address) {
            editor.remove(KEY_LAST_CONNECTED_ADDRESS)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "known_devices"
        private const val KEY_KNOWN_DEVICES = "known_devices"
        private const val KEY_LAST_CONNECTED_ADDRESS = "last_connected_address"
    }
}