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
    private val datePattern = Pattern.compile("(?i)(?:on|date)[:\\s]*(\\d{1,2}[-/](?:\\d{1,2}|[a-z]{3,9})[-/]\\d{2,4})")

    private val typeDebitPattern = Pattern.compile("(?i)(spent|debited|paid|withdrawal|txn|sent|transfer to|payment to|payment of|used on)")
    private val typeCreditPattern = Pattern.compile("(?i)(credited|received|deposit|added|refunded|transfer from|money received)")

    fun parse(message: String): ParsedTransaction? {
        val result = parseToResult(message)
        val amount = result.amount ?: return null

        return ParsedTransaction(
            amount = amount,
            type = result.transactionType ?: "DEBIT",
            merchant = result.merchant ?: "Unknown",
            category = result.category ?: MerchantClassifier.OTHER,
            bank = result.bank
        )
    }

    /**
     * Parses an SMS message into a structured [TransactionExtractionResult].
     * This is the foundation for the offline transaction intelligence layer.
     */
    fun parseToResult(message: String): TransactionExtractionResult {
        val amount = extractBestAmount(message)

        val isDebit = typeDebitPattern.matcher(message).find()
        val isCredit = typeCreditPattern.matcher(message).find()
        
        // Contextual Resolution: If both "debited" and "added" are present, it's usually a DEBIT from the source account.
        val type = when {
            message.contains("debited", ignoreCase = true) && message.contains("added", ignoreCase = true) -> "DEBIT"
            isCredit && !isDebit -> "CREDIT"
            isDebit && !isCredit -> "DEBIT"
            isCredit && isDebit -> {
                val amountIndex = amount?.let { message.indexOf(it.toString()) } ?: -1
                if (amountIndex != -1) {
                    val debitIndex = message.lowercase().lastIndexOf("debited", amountIndex)
                    val creditIndex = message.lowercase().lastIndexOf("credited", amountIndex)
                    if (debitIndex > creditIndex) "DEBIT" else "CREDIT"
                } else "DEBIT"
            }
            message.contains(" to ", ignoreCase = true) -> "DEBIT"
            message.contains(" from ", ignoreCase = true) -> "CREDIT"
            else -> "DEBIT"
        }

        val merchant = MerchantExtractor.extract(message)
        val classification = MerchantClassifier.classify(merchant, message)
        val bank = extractBank(message)
        
        val accountMatcher = accountPattern.matcher(message)
        val account = if (accountMatcher.find()) accountMatcher.group(1) else null
        
        val refMatcher = referencePattern.matcher(message)
        val reference = if (refMatcher.find()) refMatcher.group(1) else null
        
        val dateMatcher = datePattern.matcher(message)
        val extractedDate = if (dateMatcher.find()) dateMatcher.group(1) else null

        return TransactionExtractionResult(
            amount = amount,
            amountConfidence = if (amount != null) 0.98 else 0.0,
            amountMethod = if (amount != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            merchant = if (merchant != "Unknown") merchant else null,
            merchantConfidence = if (merchant != "Unknown") 0.90 else 0.0,
            merchantMethod = if (merchant != "Unknown") ExtractionMethod.MERCHANT_EXTRACTOR else ExtractionMethod.NONE,
            
            transactionType = type,
            transactionTypeConfidence = 0.95,
            transactionTypeMethod = ExtractionMethod.CONTEXT_RULE,
            
            bank = bank,
            bankConfidence = if (bank != null) 0.90 else 0.0,
            bankMethod = if (bank != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            accountIdentifier = account,
            accountIdentifierConfidence = if (account != null) 0.95 else 0.0,
            accountIdentifierMethod = if (account != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            referenceNumber = reference,
            referenceNumberConfidence = if (reference != null) 0.95 else 0.0,
            referenceNumberMethod = if (reference != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            timestamp = null, // In this phase, we don't convert extractedDate to timestamp here
            timestampConfidence = if (extractedDate != null) 0.80 else 0.0,
            timestampMethod = if (extractedDate != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            category = classification.category,
            categoryConfidence = classification.confidence,
            categoryMethod = when (classification.method) {
                "known_merchant" -> ExtractionMethod.KNOWN_MERCHANT
                "keyword_classifier" -> ExtractionMethod.KEYWORD_CLASSIFIER
                else -> ExtractionMethod.NONE
            }
        )
    }

    private fun extractBank(message: String): String? {
        val trimmed = message.trim()
        // Common Indian bank SMS footer: "-BankName" or " - BankName"
        val lastDashIndex = trimmed.lastIndexOf('-')
        if (lastDashIndex != -1 && lastDashIndex < trimmed.length - 1) {
            val potentialBank = trimmed.substring(lastDashIndex + 1).trim()
            if (potentialBank.lowercase().contains("bank") || potentialBank.length in 3..20) {
                return potentialBank.substringBefore(".").trim()
            }
        }
        return null
    }

    private fun extractBestAmount(message: String): Double? {
        val prefixMatcher = currencyPrefixPattern.matcher(message)
        val candidates = mutableListOf<Double>()
        while (prefixMatcher.find()) {
            prefixMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let { candidates.add(it) }
        }

        if (candidates.isNotEmpty()) return candidates.first()

        val suffixMatcher = currencySuffixPattern.matcher(message)
        if (suffixMatcher.find()) {
            suffixMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }
        }

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

        val prefix = message.substring(0, pos).trimEnd()
        if (prefix.endsWith("*") || prefix.lowercase().endsWith("a/c")) return true

        return false
    }
}
