package com.example.transaction.sms

import com.example.transaction.classifier.MerchantClassifier
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val merchant: String,
    val category: String = MerchantClassifier.OTHER
)

object SmsParser {
    // Enhanced Patterns for UPI, GPay, and Bank transactions
    // Matches: Rs 100, Rs. 100, INR 100, ₹100, Paid 100, Sent 100, 100 debited
    private val amountPattern = Pattern.compile("(?i)(?:(?:rs|inr|amt|vpa|sum of|amounting to|debited by|credited by|paid|sent|transfer of|payment of|₹|\\$)\\.?\\s*([\\d,]+\\.?\\d*))|(?:([\\d,]+\\.?\\d*)\\s*(?:rs|inr|₹|debited|spent))")
    private val typeDebitPattern = Pattern.compile("(?i)(spent|debited|paid|withdrawal|transaction|txn|sent|transfer to|payment to|payment of|used on)")
    private val typeCreditPattern = Pattern.compile("(?i)(credited|received|deposit|added|refunded|transfer from|money received)")

    fun parse(message: String): ParsedTransaction? {
        val amountMatcher = amountPattern.matcher(message)
        if (!amountMatcher.find()) return null
        
        // Handle both prefix (group 1) and suffix (group 2) cases
        val amountStr = (amountMatcher.group(1) ?: amountMatcher.group(2))?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        val isDebit = typeDebitPattern.matcher(message).find()
        val isCredit = typeCreditPattern.matcher(message).find()
        
        // If it looks like a transaction but type isn't clear, default to DEBIT for "to" messages
        val type = when {
            isCredit -> "CREDIT"
            isDebit -> "DEBIT"
            message.contains(" to ", ignoreCase = true) -> "DEBIT"
            message.contains(" from ", ignoreCase = true) -> "CREDIT"
            else -> return null
        }

        // Professional Merchant Extraction
        val merchant = MerchantExtractor.extract(message)

        // Category Auto-Detection
        val classification = MerchantClassifier.classify(merchant, message)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            category = classification.category
        )
    }
}
