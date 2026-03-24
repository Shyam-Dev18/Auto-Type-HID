package com.autotypehid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotypehid.ui.viewmodel.SettingsState

@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    onSelectProfile: (String) -> Unit,
    onSave: (speed: Float, typo: Float) -> Unit
) {
    val speedValue = remember(settingsState.typingSpeedMultiplier) {
        mutableFloatStateOf(settingsState.typingSpeedMultiplier)
    }
    val typoValue = remember(settingsState.typoProbabilityOverride) {
        mutableFloatStateOf(if (settingsState.typoProbabilityOverride < 0f) 0.08f else settingsState.typoProbabilityOverride)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = "Settings")
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSelectProfile("NORMAL") }) {
                Text(if (settingsState.selectedProfile == "NORMAL") "NORMAL *" else "NORMAL")
            }
            OutlinedButton(onClick = { onSelectProfile("FAST") }) {
                Text(if (settingsState.selectedProfile == "FAST") "FAST *" else "FAST")
            }
            OutlinedButton(onClick = { onSelectProfile("SLOW") }) {
                Text(if (settingsState.selectedProfile == "SLOW") "SLOW *" else "SLOW")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Typing Speed: ${"%.2f".format(speedValue.floatValue)}x")
        Slider(
            value = speedValue.floatValue,
            onValueChange = { speedValue.floatValue = it },
            valueRange = 0.5f..2.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Typo Probability: ${"%.2f".format(typoValue.floatValue)}")
        Slider(
            value = typoValue.floatValue,
            onValueChange = { typoValue.floatValue = it },
            valueRange = 0.0f..1.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { onSave(speedValue.floatValue, typoValue.floatValue) }) {
            Text("Save")
        }

        if (settingsState.isLoading) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}
