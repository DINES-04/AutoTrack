package com.example.transaction.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transaction.classifier.MerchantClassifier
import java.util.*

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val categoryData by viewModel.categoryWiseSpending.collectAsState(initial = emptyMap())
    val merchantData by viewModel.merchantWiseSpending.collectAsState(initial = emptyMap())
    val accountData by viewModel.accountWiseSpending.collectAsState(initial = emptyMap())
    
    val selectedMonth by viewModel.histMonth.collectAsState()
    val selectedYear by viewModel.histYear.collectAsState()

    val monthName = remember(selectedMonth) { 
        java.text.DateFormatSymbols().months[selectedMonth - 1]
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Spending Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$monthName $selectedYear", color = Color.Gray, fontSize = 12.sp)
                }
                MonthYearPicker(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onMonthSelected = { viewModel.setHistMonth(it) },
                    onYearSelected = { viewModel.setHistYear(it) }
                )
            }
        }

        item {
            if (merchantData.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Top Merchants (Description)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // We'll take top 5 merchants for the pie chart to keep it clean
                        val sortedMerchants = merchantData.toList().sortedByDescending { it.second }.take(5)
                        val othersAmount = merchantData.values.sum() - sortedMerchants.sumOf { it.second }
                        val pieData = if (othersAmount > 0) sortedMerchants + ("Others" to othersAmount) else sortedMerchants
                        
                        PieChart(data = pieData.toMap())
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Legend
                        pieData.forEachIndexed { index, (merchant, amount) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(getDistinctColor(index), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(merchant, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("₹${String.format(Locale.getDefault(), "%.2f", amount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (categoryData.isEmpty()) {
                        Text("No data for this period", color = Color.Gray, modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally))
                    } else {
                        val total = categoryData.values.sum()
                        categoryData.toList().sortedByDescending { it.second }.forEach { (category, amount) ->
                            AnalyticsItem(category, amount, total, getCategoryColor(category))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Account-wise Split", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (accountData.isEmpty()) {
                        Text("No data for this period", color = Color.Gray, modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally))
                    } else {
                        val total = accountData.values.sum()
                        accountData.toList().sortedByDescending { it.second }.forEach { (account, amount) ->
                            AnalyticsItem(account, amount, total, MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(data: Map<String, Double>) {
    val total = data.values.sum().toFloat()
    if (total == 0f) return

    var startAngle = -90f
    
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(modifier = Modifier.size(200.dp)) {
            data.toList().forEachIndexed { index, (_, amount) ->
                val sweepAngle = (amount.toFloat() / total) * 360f
                drawArc(
                    color = getDistinctColor(index),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt),
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", color = Color.Gray, fontSize = 12.sp)
            Text("₹${String.format(Locale.getDefault(), "%.0f", total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun AnalyticsItem(label: String, amount: Double, total: Double, color: Color) {
    val percentage = if (total > 0) (amount / total).toFloat() else 0f
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("₹${String.format(Locale.getDefault(), "%.2f", amount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            strokeCap = StrokeCap.Round,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        MerchantClassifier.SHOPPING -> Color(0xFFE91E63)
        MerchantClassifier.FOOD_DINING -> Color(0xFFFF9800)
        MerchantClassifier.TRAVEL_TRANSPORT -> Color(0xFF03A9F4)
        MerchantClassifier.FUEL -> Color(0xFFFFC107)
        MerchantClassifier.BILLS_UTILITIES -> Color(0xFF673AB7)
        MerchantClassifier.ENTERTAINMENT -> Color(0xFF9C27B0)
        MerchantClassifier.HEALTH -> Color(0xFFF44336)
        MerchantClassifier.EDUCATION -> Color(0xFF3F51B5)
        MerchantClassifier.INVESTMENT -> Color(0xFF4CAF50)
        MerchantClassifier.BANKING_FINANCE -> Color(0xFF607D8B)
        MerchantClassifier.TRANSFER -> Color(0xFF00BCD4)
        MerchantClassifier.INCOME -> Color(0xFF4CAF50)
        MerchantClassifier.INSURANCE -> Color(0xFF009688)
        MerchantClassifier.SUBSCRIPTION -> Color(0xFF795548)
        MerchantClassifier.OTHER -> Color(0xFF9E9E9E)
        "Manual" -> Color(0xFF8BC34A)
        else -> Color(0xFF607D8B)
    }
}

fun getDistinctColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFF4CAF50), 
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B)
    )
    return colors[index % colors.size]
}
