package com.example.transaction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transaction.data.entity.TransactionEntity
import com.example.transaction.classifier.MerchantClassifier
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: TransactionEntity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(transaction.category),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(
                        text = transaction.category,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "${if (transaction.type == "DEBIT") "-" else "+"} ₹${transaction.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (transaction.type == "DEBIT") Color.Red else Color(0xFF4CAF50)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, Boolean) -> Unit
) {
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var category by remember { mutableStateOf(transaction.category) }
    var note by remember { mutableStateOf(transaction.note) }
    var renameAll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = renameAll, onCheckedChange = { renameAll = it })
                    Text(
                        "Update all transactions from \"${transaction.merchant}\"",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(transaction.copy(merchant = merchant, category = category, note = note), renameAll)
                }
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

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        MerchantClassifier.SHOPPING -> Icons.Default.ShoppingBag
        MerchantClassifier.FOOD_DINING -> Icons.Default.Restaurant
        MerchantClassifier.TRAVEL_TRANSPORT -> Icons.Default.DirectionsCar
        MerchantClassifier.FUEL -> Icons.Default.LocalGasStation
        MerchantClassifier.BILLS_UTILITIES -> Icons.Default.Receipt
        MerchantClassifier.ENTERTAINMENT -> Icons.Default.Movie
        MerchantClassifier.HEALTH -> Icons.Default.MedicalServices
        MerchantClassifier.EDUCATION -> Icons.Default.School
        MerchantClassifier.INVESTMENT -> Icons.Default.TrendingUp
        MerchantClassifier.BANKING_FINANCE -> Icons.Default.AccountBalance
        MerchantClassifier.TRANSFER -> Icons.Default.SyncAlt
        MerchantClassifier.INCOME -> Icons.Default.Payments
        MerchantClassifier.INSURANCE -> Icons.Default.Security
        MerchantClassifier.SUBSCRIPTION -> Icons.Default.Subscriptions
        MerchantClassifier.OTHER -> Icons.Default.Category
        "Manual" -> Icons.Default.Edit
        else -> Icons.Default.Payment
    }
}
