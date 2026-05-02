package com.example.transaction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transaction.data.entity.Account

@Composable
fun AccountManagementScreen(viewModel: MainViewModel) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Manage Accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts) { account ->
                    AccountItem(
                        account = account,
                        onEdit = { editingAccount = account },
                        onToggle = { viewModel.updateAccount(account.copy(isActive = !account.isActive)) },
                        onDelete = { viewModel.deleteAccount(account) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, keyword ->
                viewModel.addAccount(name, keyword)
                showAddDialog = false
            }
        )
    }

    if (editingAccount != null) {
        EditAccountDialog(
            account = editingAccount!!,
            onDismiss = { editingAccount = null },
            onSave = { updatedAccount ->
                viewModel.updateAccount(updatedAccount)
                editingAccount = null
            }
        )
    }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Bank Keyword (e.g. HDFC)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && keyword.isNotBlank()) onSave(name, keyword) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AccountItem(
    account: Account,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Keyword: ${account.bankKeyword}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = account.isActive, onCheckedChange = { onToggle() })
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
            }
        }
    }
}

@Composable
fun EditAccountDialog(account: Account, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var name by remember { mutableStateOf(account.name) }
    var keyword by remember { mutableStateOf(account.bankKeyword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Account") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") })
                OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Bank Keyword") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(account.copy(name = name, bankKeyword = keyword)) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
