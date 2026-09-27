package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ManualEditDialog(
    cellTitle: String,
    initialValue: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var rawText by remember { mutableStateOf(if (initialValue > 0) initialValue.toString() else "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Edit: $cellTitle") },
        text = {
            Column {
                Text(
                    text = "Enter in seconds. Set to 0 to clear.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Time (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_time_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = rawText.toDoubleOrNull() ?: 0.0
                    onSave(parsed)
                },
                modifier = Modifier.testTag("manual_save_confirm")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
