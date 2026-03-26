package com.autotypehid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.autotypehid.core.managers.AppContainer
import com.autotypehid.presentation.navigation.AutoTypeNavGraph

@Composable
fun AppRoot() {
    val settings = AppContainer.observeSettingsUseCase().collectAsState(initial = null).value

    val darkTheme = when (settings?.themeMode ?: "SYSTEM") {
        "LIGHT" -> false
        "DARK" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        AutoTypeNavGraph()
    }
}
