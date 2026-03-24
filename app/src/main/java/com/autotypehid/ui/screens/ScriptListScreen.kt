package com.autotypehid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotypehid.data.model.ScriptEntity

@Composable
fun ScriptListScreen(
    scripts: List<ScriptEntity>,
    onSelect: (ScriptEntity) -> Unit
) {
    if (scripts.isEmpty()) {
        Text(text = "No scripts saved")
        return
    }

    LazyColumn {
        items(scripts, key = { it.id }) { script ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(script) }
                    .padding(vertical = 10.dp)
            ) {
                Text(text = script.name)
                Text(text = script.content.take(60))
            }
        }
    }
}
