package com.autotypehid.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScriptEditorScreen(
    onSave: (name: String, content: String) -> Unit
) {
    val name = remember { mutableStateOf("") }
    val content = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Script Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = content.value,
            onValueChange = { content.value = it },
            label = { Text("Script Content") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(top = 8.dp)
        )

        Button(
            onClick = {
                onSave(name.value, content.value)
                if (name.value.isNotBlank() && content.value.isNotBlank()) {
                    name.value = ""
                    content.value = ""
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Save")
        }
    }
}
