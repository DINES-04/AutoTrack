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
    private val typeDebitPattern = Pattern.compile("(?i)(spent|debited|paid|withdrawal|withdrawn|txn|sent|transfer to|payment to|payment of|used on)")
    private val typeCreditPattern = Pattern.compile("(?i)(credited|received|deposit|added|refunded|refund|transfer from|money received|salary)")

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
        val candidates = NumericContextResolver.resolve(message)
        val bestAmountCandidate = NumericContextResolver.selectTransactionAmount(message, candidates)
        val amount = bestAmountCandidate?.normalizedValue

        val isDebit = typeDebitPattern.matcher(message).find()
        val isCredit = typeCreditPattern.matcher(message).find()
        
        var typeConfidence = 0.0
        // Contextual Resolution: If both "debited" and "added" are present, it's usually a DEBIT from the source account.
        val type = when {
            message.contains("debited", ignoreCase = true) && message.contains("added", ignoreCase = true) -> {
                typeConfidence = 0.85
                "DEBIT"
            }
            isCredit && !isDebit -> {
                typeConfidence = 0.98
                "CREDIT"
            }
            isDebit && !isCredit -> {
                typeConfidence = 0.98
                "DEBIT"
            }
            isCredit && isDebit -> {
                val amountIndex = bestAmountCandidate?.start ?: -1
                if (amountIndex != -1) {
                    val debitIndex = message.lowercase().lastIndexOf("debited", amountIndex)
                    val creditIndex = message.lowercase().lastIndexOf("credited", amountIndex)
                    typeConfidence = 0.80
                    if (debitIndex > creditIndex) "DEBIT" else "CREDIT"
                } else {
                    typeConfidence = 0.50
                    "DEBIT"
                }
            }
            message.contains(" to ", ignoreCase = true) -> {
                typeConfidence = 0.70
                "DEBIT"
            }
            message.contains(" from ", ignoreCase = true) -> {
                typeConfidence = 0.70
                "CREDIT"
            }
            else -> {
                typeConfidence = 0.40
                "DEBIT"
            }
        }

        val merchant = MerchantExtractor.extract(message)
        val classification = MerchantClassifier.classify(merchant, message)
        val bank = extractBank(message)
        
        val accountCand = candidates.find { it.type == CandidateType.ACCOUNT_IDENTIFIER }
        val account = accountCand?.rawValue
        val refCand = candidates.find { it.type == CandidateType.REFERENCE_NUMBER }
        val reference = refCand?.rawValue
        val dateCand = candidates.find { it.type == CandidateType.DATE }
        val extractedDate = dateCand?.rawValue

        val merchantConfidence = when {
            merchant == "Unknown" -> 0.0
            merchant == "Salary" || merchant == "Refund" -> 0.95
            message.contains(Regex("(by|at|to|from|on|for)\\s+$merchant", RegexOption.IGNORE_CASE)) -> 0.90
            else -> 0.60
        }

        return TransactionExtractionResult(
            amount = amount,
            amountConfidence = bestAmountCandidate?.confidence ?: 0.0,
            amountMethod = if (amount != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            merchant = if (merchant != "Unknown") merchant else null,
            merchantConfidence = merchantConfidence,
            merchantMethod = if (merchant != "Unknown") ExtractionMethod.MERCHANT_EXTRACTOR else ExtractionMethod.NONE,
            
            transactionType = type,
            transactionTypeConfidence = typeConfidence,
            transactionTypeMethod = ExtractionMethod.CONTEXT_RULE,
            
            bank = bank,
            bankConfidence = if (bank != null) 0.90 else 0.0,
            bankMethod = if (bank != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            accountIdentifier = account,
            accountIdentifierConfidence = accountCand?.confidence ?: 0.0,
            accountIdentifierMethod = if (account != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            referenceNumber = reference,
            referenceNumberConfidence = refCand?.confidence ?: 0.0,
            referenceNumberMethod = if (reference != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            timestamp = null,
            timestampConfidence = dateCand?.confidence ?: 0.0,
            timestampMethod = if (extractedDate != null) ExtractionMethod.REGEX else ExtractionMethod.NONE,
            
            category = classification.category,
            categoryConfidence = classification.confidence,
            categoryMethod = when (classification.method) {
                "known_merchant" -> ExtractionMethod.KNOWN_MERCHANT
                "keyword_classifier" -> ExtractionMethod.KEYWORD_CLASSIFIER
                "user_mapping" -> ExtractionMethod.USER_MAPPING
                else -> ExtractionMethod.NONE
            }
        )
    }

    private fun extractBank(message: String): String? {
        val trimmed = message.trim()
        val lastDashIndex = trimmed.lastIndexOf('-')
        if (lastDashIndex != -1 && lastDashIndex < trimmed.length - 1) {
            val potentialBank = trimmed.substring(lastDashIndex + 1).trim()
            if (potentialBank.lowercase().contains("bank") || potentialBank.length in 3..20) {
                return potentialBank.substringBefore(".").trim()
            }
        }
        return null
    }
}
