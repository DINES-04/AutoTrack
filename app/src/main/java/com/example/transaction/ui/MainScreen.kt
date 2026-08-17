package com.example.transaction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transaction.data.entity.Account

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "History", Icons.Default.History)
    object Category : Screen("category", "Category", Icons.Default.Category)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showSecurityAlert by remember { mutableStateOf(false) }

    // Check for theoretical vulnerabilities or permission issues
    LaunchedEffect(Unit) {
        // Example: check if SMS permission is granted (simulation for "vulnerability" check)
        // In a real app, this would be more complex.
        showSecurityAlert = true 
    }

    if (showSecurityAlert) {
        AlertDialog(
            onDismissRequest = { showSecurityAlert = false },
            title = { Text("System Check") },
            text = { Text("App is running with SMS detection enabled. Ensure you trust this app with transaction notifications.") },
            confirmButton = {
                TextButton(onClick = { showSecurityAlert = false }) { Text("I Understand") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                actions = {
                    if (currentScreen != Screen.Analytics) {
                        IconButton(onClick = { currentScreen = Screen.Analytics }) {
                            Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Screen.Dashboard,
                    Screen.Transactions,
                    Screen.Category,
                    Screen.Accounts,
                    Screen.Settings
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == Screen.Dashboard || currentScreen == Screen.Transactions) {
                FloatingActionButton(onClick = { showAddTransactionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel, onNavigateToHistory = { currentScreen = Screen.Transactions })
                Screen.Transactions -> TransactionListScreen(viewModel)
                Screen.Category -> CategoryScreen(viewModel)
                Screen.Analytics -> AnalyticsScreen(viewModel)
                Screen.Accounts -> AccountManagementScreen(viewModel)
                Screen.Settings -> SettingsScreen(viewModel)
            }
        }
    }

    if (showAddTransactionDialog) {
        val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
        AddTransactionDialog(
            accounts = accounts,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { amount, type, merchant, accountId, category, note ->
                viewModel.addTransaction(amount, type, merchant, accountId, category, note)
                showAddTransactionDialog = false
            }
        )
    }
}

@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DEBIT") }
    var category by remember { mutableStateOf("Manual") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row {
                    FilterChip(selected = type == "DEBIT", onClick = { type = "DEBIT" }, label = { Text("Debit") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = type == "CREDIT", onClick = { type = "CREDIT" }, label = { Text("Credit") })
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(accounts.find { it.id == selectedAccountId }?.name ?: "Select Account")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && selectedAccountId != null && merchant.isNotBlank()) {
                        onSave(amt, type, merchant, selectedAccountId!!, category, note)
                    }
                },
                enabled = amount.isNotBlank() && selectedAccountId != null && merchant.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
