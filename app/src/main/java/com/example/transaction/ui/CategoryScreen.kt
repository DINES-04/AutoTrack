package com.example.transaction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transaction.classifier.MerchantClassifier

@Composable
fun CategoryScreen(viewModel: MainViewModel) {
    val transactions by viewModel.historyTransactions.collectAsState(initial = emptyList())
    
    val categoryCounts = remember(transactions) {
        transactions.groupBy { it.category }.mapValues { it.value.size }
    }
    
    val builtInCategories = MerchantClassifier.getAllBuiltInCategories()
    val customCategories = remember(transactions) {
        transactions.map { it.category }
            .distinct()
            .filter { it !in builtInCategories }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Categories", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Text("Built-in", style = MaterialTheme.typography.titleMedium)
            }
            items(builtInCategories) { category ->
                CategoryCard(category, categoryCounts[category] ?: 0)
            }
            
            if (customCategories.isNotEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Text("Custom", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                }
                items(customCategories) { category ->
                    CategoryCard(category, categoryCounts[category] ?: 0)
                }
            }
        }
        
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Custom Category")
        }
    }

    if (showAddDialog) {
        var newCategory by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    // Manual entry doesn't persist unless a transaction uses it, 
                    // but we can simulate adding by just closing for now.
                    showAddDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CategoryCard(name: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                getCategoryIcon(name),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "$count Transactions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
