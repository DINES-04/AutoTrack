package com.example.transaction.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transaction.data.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(viewModel: MainViewModel) {
    val transactions by viewModel.historyTransactions.collectAsState(initial = emptyList())
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val selectedAccountId by viewModel.histAccountId.collectAsState()
    val selectedMonth by viewModel.histMonth.collectAsState()
    val selectedYear by viewModel.histYear.collectAsState()
    
    val groupedTransactions = remember(transactions) {
        transactions.groupBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
            
            when {
                isSameDay(cal, today) -> "Today"
                isSameDay(cal, yesterday) -> "Yesterday"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filters Header
        Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    MonthYearPicker(
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        onMonthSelected = { viewModel.setHistMonth(it) },
                        onYearSelected = { viewModel.setHistYear(it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { viewModel.setHistAccount(null) },
                            label = { Text("All Accounts") },
                            leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    items(accounts) { account ->
                        FilterChip(
                            selected = selectedAccountId == account.id,
                            onClick = { viewModel.setHistAccount(account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No transactions found", color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text("Try changing filters", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                groupedTransactions.forEach { (date, items) ->
                    stickyHeader {
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                            Text(
                                text = date,
                                modifier = Modifier.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(items, key = { it.id }) { transaction ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            LaunchedEffect(transaction) {
                                viewModel.deleteTransaction(transaction)
                                dismissState.reset()
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                    SwipeToDismissBoxValue.StartToEnd -> Color.Blue.copy(alpha = 0.8f)
                                    else -> Color.Transparent
                                }
                                val alignment = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    else -> Alignment.Center
                                }
                                val icon = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                    else -> null
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    if (icon != null) {
                                        Icon(icon, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        ) {
                            TransactionItem(transaction = transaction)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
