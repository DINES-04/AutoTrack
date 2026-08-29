package com.example.transaction.sms

import com.example.transaction.classifier.MerchantClassifier
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val merchant: String,
    val category: String = MerchantClassifier.OTHER,
    val bank: String? = null
)

object SmsParser {
    // Patterns that strongly indicate a transaction amount
    private val currencyPrefixPattern = Pattern.compile("(?i)(?:rs|inr|amt|₹|\\$)\\.?\\s*([\\d,]+\\.?\\d*)")
    private val currencySuffixPattern = Pattern.compile("(?i)([\\d,]+\\.?\\d*)\\s*(?:rs|inr|₹)")
    
    // Patterns for specific numeric identifiers that should NOT be amounts
    private val accountPattern = Pattern.compile("(?i)(?:a/c|acc|account|acct)\\s*(?:no|num)?\\s*[*xX]*(\\d{4,})")
    private val referencePattern = Pattern.compile("(?i)(?:rrn|ref|utr|txn|id)[:\\s]*(\\d{8,})")
    private val phonePattern = Pattern.compile("(?:\\+91|0)?\\d{10}")
    private val bankNamePattern = Pattern.compile("(?i)(?:-|\\s)([a-zA-Z]+\\sBank)$")

    private val typeDebitPattern = Pattern.compile("(?i)(spent|debited|paid|withdrawal|txn|sent|transfer to|payment to|payment of|used on)")
    private val typeCreditPattern = Pattern.compile("(?i)(credited|received|deposit|added|refunded|transfer from|money received)")

    fun parse(message: String): ParsedTransaction? {
        val amount = extractBestAmount(message) ?: return null

        val isDebit = typeDebitPattern.matcher(message).find()
        val isCredit = typeCreditPattern.matcher(message).find()
        
        // Contextual Resolution: If both "debited" and "added" (or credited) are present,
        // we check the proximity to "A/c debited". In Indian banking, "A/c debited"
        // followed by "added to wallet/UPI Lite" usually means a DEBIT from the user's account.
        val type = when {
            message.contains("debited", ignoreCase = true) && message.contains("added", ignoreCase = true) -> "DEBIT"
            isCredit && !isDebit -> "CREDIT"
            isDebit && !isCredit -> "DEBIT"
            isCredit && isDebit -> {
                // Heuristic: If both, check which keyword is closer to the amount
                val amountIndex = message.indexOf(amount.toString())
                val debitIndex = message.lowercase().lastIndexOf("debited", amountIndex)
                val creditIndex = message.lowercase().lastIndexOf("credited", amountIndex)
                if (debitIndex > creditIndex) "DEBIT" else "CREDIT"
            }
            message.contains(" to ", ignoreCase = true) -> "DEBIT"
            message.contains(" from ", ignoreCase = true) -> "CREDIT"
            else -> "DEBIT" // Default to DEBIT for ambiguous financial messages
        }

        val merchant = MerchantExtractor.extract(message)
        val classification = MerchantClassifier.classify(merchant, message)

        val bankMatcher = bankNamePattern.matcher(message.trim())
        val bank = if (bankMatcher.find()) bankMatcher.group(1) else null

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            category = classification.category,
            bank = bank
        )
    }

    private fun extractBestAmount(message: String): Double? {
        // 1. Find all currency-prefixed candidates
        val prefixMatcher = currencyPrefixPattern.matcher(message)
        val candidates = mutableListOf<Double>()
        while (prefixMatcher.find()) {
            prefixMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let { candidates.add(it) }
        }

        if (candidates.isNotEmpty()) {
            // If multiple, usually the first one with currency is the transaction amount
            // (Subsequent ones might be balances)
            return candidates.first()
        }

        // 2. Try suffix
        val suffixMatcher = currencySuffixPattern.matcher(message)
        if (suffixMatcher.find()) {
            suffixMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }
        }

        // 3. Fallback: Identify numbers followed by "debited" or "credited" 
        // BUT verify they are not account numbers or references
        val genericNumberPattern = Pattern.compile("([\\d,]+\\.?\\d*)\\s*(?:debited|spent|credited|received)", Pattern.CASE_INSENSITIVE)
        val genericMatcher = genericNumberPattern.matcher(message)
        if (genericMatcher.find()) {
            val numStr = genericMatcher.group(1)?.replace(",", "") ?: ""
            val num = numStr.toDoubleOrNull()
            if (num != null && !isAccountOrRef(message, numStr, genericMatcher.start())) {
                return num
            }
        }

        return null
    }

    private fun isAccountOrRef(message: String, numStr: String, pos: Int): Boolean {
        // Check if this number matches any non-amount pattern in the message
        val accountMatcher = accountPattern.matcher(message)
        while (accountMatcher.find()) {
            if (accountMatcher.group(1) == numStr) return true
        }
        
        val refMatcher = referencePattern.matcher(message)
        while (refMatcher.find()) {
            if (refMatcher.group(1) == numStr) return true
        }
        
        val phoneMatcher = phonePattern.matcher(message)
        while (phoneMatcher.find()) {
            if (phoneMatcher.group() == numStr) return true
        }

        // Also check if it's preceded by "A/c *"
        val prefix = message.substring(0, pos).trimEnd()
        if (prefix.endsWith("*") || prefix.lowercase().endsWith("a/c")) return true

        return false
    }
}
