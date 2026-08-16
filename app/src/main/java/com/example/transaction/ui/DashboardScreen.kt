package com.example.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transaction.data.entity.Account
import com.example.transaction.data.entity.TransactionEntity
import java.util.*

@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigateToHistory: () -> Unit) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val selectedAccountId by viewModel.dashAccountId.collectAsState()
    val selectedMonth by viewModel.dashMonth.collectAsState()
    val selectedYear by viewModel.dashYear.collectAsState()
    
    val expense by viewModel.dashExpense.collectAsState(initial = 0.0)
    val lastMonthExpense by viewModel.lastMonthExpense.collectAsState(initial = 0.0)
    val credit by viewModel.dashCredit.collectAsState(initial = 0.0)
    val netBalance by viewModel.dashNetBalance.collectAsState(initial = 0.0)
    val remainingBudget by viewModel.remainingBudget.collectAsState(initial = 0.0)
    val settings by viewModel.settings.collectAsState(initial = null)
    val transactions by viewModel.dashTransactions.collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    if (showEditDialog && transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            onDismiss = {
                showEditDialog = false
                transactionToEdit = null
            },
            onConfirm = { updatedTransaction, renameAll ->
                if (renameAll) {
                    viewModel.renameAllTransactions(
                        transactionToEdit!!.merchant,
                        updatedTransaction.merchant,
                        updatedTransaction.category
                    )
                } else {
                    viewModel.updateTransaction(updatedTransaction)
                }
                showEditDialog = false
                transactionToEdit = null
            }
        )
    }

    val monthName = remember(selectedMonth) { 
        java.text.DateFormatSymbols().months[selectedMonth - 1]
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            // Month & Year Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$monthName $selectedYear",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Dashboard Overview", color = Color.Gray, fontSize = 12.sp)
                }
                MonthYearPicker(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onMonthSelected = { viewModel.setDashMonth(it) },
                    onYearSelected = { viewModel.setDashYear(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Key Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Expense",
                    amount = expense,
                    color = Color.Red,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Credit",
                    amount = credit,
                    color = Color(0xFF4CAF50),
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Net Balance",
                    amount = netBalance,
                    color = if (netBalance >= 0) Color.Blue else Color.Red,
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Remaining",
                    amount = remainingBudget,
                    color = if (remainingBudget >= 0) Color.DarkGray else Color.Red,
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Insight Card
            if (expense > 0 || lastMonthExpense > 0) {
                InsightCard(expense, settings?.monthlyBudget ?: 0.0, lastMonthExpense)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Account Filter Chips
            Text("Filter by Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                item {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { viewModel.setDashAccount(null) },
                        label = { Text("All Accounts") }
                    )
                }
                items(accounts) { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { viewModel.setDashAccount(account.id) },
                        label = { Text(account.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (transactions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions for this selection", color = Color.Gray)
                }
            }
        } else {
            items(transactions.take(10)) { transaction ->
                val dismissState = rememberSwipeToDismissBoxState()

                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                    LaunchedEffect(transaction) {
                        transactionToEdit = transaction
                        showEditDialog = true
                        dismissState.reset()
                    }
                }
                
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
            item {
                if (transactions.size > 10) {
                    TextButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View all in History")
                    }
                }
            }
        }
    }
}

@Composable
fun MonthYearPicker(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit
) {
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }
    
    val months = java.text.DateFormatSymbols().months.filter { it.isNotEmpty() }
    val years = (2020..Calendar.getInstance().get(Calendar.YEAR) + 1).toList().reversed()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { expandedMonth = true }) {
                Text(months[selectedMonth - 1].take(3))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                months.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onMonthSelected(index + 1)
                            expandedMonth = false
                        }
                    )
                }
            }
        }
        Box {
            TextButton(onClick = { expandedYear = true }) {
                Text(selectedYear.toString())
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearSelected(year)
                            expandedYear = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text("₹${String.format(Locale.getDefault(), "%.2f", amount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun InsightCard(expense: Double, budget: Double, lastMonthExpense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Monthly Insight", fontWeight = FontWeight.Bold)
                
                val budgetText = if (budget > 0) {
                    val percent = (expense / budget * 100).toInt()
                    "Used $percent% of budget."
                } else {
                    "Set a budget to track progress."
                }
                
                val comparisonText = when {
                    lastMonthExpense == 0.0 -> ""
                    expense > lastMonthExpense -> " You spent more than last month."
                    expense < lastMonthExpense -> " You spent less than last month!"
                    else -> ""
                }
                
                Text("$budgetText$comparisonText", fontSize = 14.sp)
            }
        }
    }
}
