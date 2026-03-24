package com.autotypehid.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DebugScreen(
    connectionState: String,
    lastActions: List<String>,
    lastError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = "Debug")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Connection: $connectionState")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Last Error: ${lastError ?: "None"}")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Last 10 Actions")

        LazyColumn {
            items(lastActions) { item ->
                Text(text = item)
            }
        }
    }
}
