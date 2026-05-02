package com.example.transaction.sms

import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val merchant: String
)

object SmsParser {
    // Basic regex for common transaction formats: "Spent Rs.500 at Merchant", "Credited Rs.1000", etc.
    private val amountPattern = Pattern.compile("(?i)(?:rs|inr|amt)\\.?\\s*([\\d,]+\\.?\\d*)")
    private val typeDebitPattern = Pattern.compile("(?i)(spent|debited|paid|transaction|withdrawal)")
    private val typeCreditPattern = Pattern.compile("(?i)(credited|received|deposit)")

    fun parse(message: String): ParsedTransaction? {
        val amountMatcher = amountPattern.matcher(message)
        if (!amountMatcher.find()) return null
        
        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        val isDebit = typeDebitPattern.matcher(message).find()
        val isCredit = typeCreditPattern.matcher(message).find()

        val type = when {
            isDebit -> "DEBIT"
            isCredit -> "CREDIT"
            else -> "DEBIT" // Default to debit
        }

        // Simple merchant extraction logic (can be improved)
        val merchant = when {
            message.contains("at", ignoreCase = true) -> {
                message.substringAfter("at", "").substringBefore(".").trim()
            }
            message.contains("to", ignoreCase = true) -> {
                message.substringAfter("to", "").substringBefore(".").trim()
            }
            else -> "Unknown"
        }

        return ParsedTransaction(amount, type, merchant.takeIf { it.isNotEmpty() } ?: "Unknown")
    }
}
