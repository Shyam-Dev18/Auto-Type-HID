package com.autotypehid.bluetooth.sender

import com.autotypehid.core.utils.Logger
import com.autotypehid.bluetooth.service.HidService

class HidReportSender(
    private val hidService: HidService?
) {

    companion object {
        private const val TAG = "HidReportSender"
        private const val SHIFT_MODIFIER: Byte = 0x02
    }

    fun sendKey(keyCode: Byte, shift: Boolean) {
        if (hidService?.isConnected != true) {
            Logger.debug(TAG, "Safe mode key send (not connected): keyCode=$keyCode shift=$shift")
            return
        }
        sendKeyDown(keyCode, shift)
        sendKeyUp(keyCode, shift)
    }

    private fun sendKeyDown(keyCode: Byte, shift: Boolean) {
        val service = hidService
        if (service == null) {
            Logger.debug(TAG, "Safe mode key down (service null): keyCode=$keyCode shift=$shift")
            return
        }
        val modifier = if (shift) SHIFT_MODIFIER else 0x00
        val report = service.buildKeyboardReport(modifier = modifier, keyCode = keyCode)
        val sent = service.sendReport(report)
        Logger.debug(TAG, "Key down report sent=$sent keyCode=$keyCode shift=$shift")
    }

    private fun sendKeyUp(keyCode: Byte, shift: Boolean) {
        val service = hidService
        if (service == null) {
            Logger.debug(TAG, "Safe mode key up (service null): keyCode=$keyCode shift=$shift")
            return
        }
        val report = service.buildEmptyKeyboardReport()
        val sent = service.sendReport(report)
        Logger.debug(TAG, "Key up report sent=$sent")
    }
}
