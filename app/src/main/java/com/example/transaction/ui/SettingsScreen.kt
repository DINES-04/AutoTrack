package com.example.transaction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState(initial = null)
    var budgetInput by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        budgetInput = settings?.monthlyBudget?.toString() ?: ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Monthly Budget", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) budgetInput = it },
                modifier = Modifier.weight(1f),
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.updateBudget(budgetInput.toDoubleOrNull() ?: 0.0) }) {
                Text("Set")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Danger Zone", color = Color.Red, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset All Data", color = Color.White)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Reset All Data?") },
            text = { Text("This will delete all transactions and accounts. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetData()
                    showResetDialog = false
                }) { Text("Confirm Reset", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
