package com.autotypehid.core.utils

import android.util.Log

object Logger {

    fun debug(tag: String, message: String) {
        runCatching { Log.d(tag, message) }
    }

    fun error(tag: String, message: String) {
        runCatching { Log.e(tag, message) }
    }
}
