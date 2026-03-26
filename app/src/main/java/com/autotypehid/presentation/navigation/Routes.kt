package com.autotypehid.presentation.navigation

object Routes {
    const val INIT = "init"
    const val PERMISSIONS = "permissions"
    const val DEVICE_SCAN = "device_scan"
    const val DASHBOARD = "dashboard"
    const val SCRIPTS_LIST = "scripts_list"
    const val SCRIPT_EDITOR = "script_editor"
    const val TYPING_CONTROL = "typing_control"
    const val SETTINGS = "settings"

    const val SCRIPT_ID_ARG = "scriptId"
    const val SCRIPT_EDITOR_WITH_ARG = "$SCRIPT_EDITOR?$SCRIPT_ID_ARG={$SCRIPT_ID_ARG}"

    fun scriptEditor(scriptId: Int?): String {
        val safeId = scriptId ?: -1
        return "$SCRIPT_EDITOR?$SCRIPT_ID_ARG=$safeId"
    }
}
