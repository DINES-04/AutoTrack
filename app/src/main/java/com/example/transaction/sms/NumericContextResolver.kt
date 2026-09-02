package com.example.transaction.sms

import java.util.regex.Pattern

object NumericContextResolver {
    private val currencyPrefixPattern = Pattern.compile("(?i)(?:rs|inr|amt|₹|\\$)\\.?\\s*([\\d,]+\\.?\\d*)")
    private val currencySuffixPattern = Pattern.compile("(?i)([\\d,]+\\.?\\d*)\\s*(?:rs|inr|₹)")
    
    private val accountPattern = Pattern.compile("(?i)(?:a/c|acc|account|acct)\\s*(?:no|num|ending)?\\s*[*xX-]*(\\d{3,})")
    private val referencePattern = Pattern.compile("(?i)(?:rrn|ref|utr|txn|id|reference|transaction id)[:\\s.]*(?:no|num)?[:\\s.-]*([a-zA-Z0-9]{8,})")
    private val phonePattern = Pattern.compile("(?:\\+91|0)?(?:[6-9]\\d{9}|1800\\d{6,7})")
    private val shortCodePattern = Pattern.compile("\\b\\d{5,6}\\b")
    
    private val datePattern = Pattern.compile("(?i)(?:on|date)[:\\s]*(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{1,2}\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s*\\d{2,4})")
    private val timePattern = Pattern.compile("(?i)(\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:am|pm)?\\b)")
    
    private val balancePattern = Pattern.compile("(?i)(?:bal|balance|avl|available|limit|min bal)\\s*(?:is|at|of)?[:\\s]*(?:rs|inr|₹)?\\s*([\\d,]+\\.?\\d*)")
    
    private val otpPattern = Pattern.compile("(?i)(?:otp|code|vcode)[:\\s-]*(\\d{4,8})")
    
    private val genericNumberPattern = Pattern.compile("\\b([\\d,]+\\.\\d{2})\\b")

    fun resolve(message: String): List<NumericCandidate> {
        val candidates = mutableListOf<NumericCandidate>()

        // 1. Identify Balances (explicitly labeled amounts to exclude them from transaction amounts)
        findAll(message, balancePattern, CandidateType.BALANCE, candidates, group = 1, confidence = 0.95)

        // 2. Identify Transaction Amounts (labeled with currency - HIGH confidence)
        findAll(message, currencyPrefixPattern, CandidateType.TRANSACTION_AMOUNT, candidates, group = 1, confidence = 0.98)
        findAll(message, currencySuffixPattern, CandidateType.TRANSACTION_AMOUNT, candidates, group = 1, confidence = 0.98)

        // 3. Identify Dates and Times
        findAll(message, datePattern, CandidateType.DATE, candidates, group = 1, confidence = 0.90)
        findAll(message, timePattern, CandidateType.TIME, candidates, group = 1, confidence = 0.90)

        // 4. Identify Account Numbers
        findAll(message, accountPattern, CandidateType.ACCOUNT_IDENTIFIER, candidates, group = 1, confidence = 0.95)

        // 5. Identify Reference Numbers
        findAll(message, referencePattern, CandidateType.REFERENCE_NUMBER, candidates, group = 1, confidence = 0.95)

        // 6. Identify Phone Numbers and Short Codes
        findAll(message, phonePattern, CandidateType.PHONE_NUMBER, candidates, confidence = 0.90)
        findAll(message, shortCodePattern, CandidateType.PHONE_NUMBER, candidates, confidence = 0.85)

        // 7. Identify OTPs
        findAll(message, otpPattern, CandidateType.OTP, candidates, group = 1, confidence = 0.95)
        
        // 8. Identify Generic amount-like numbers (MEDIUM/LOW confidence)
        findAll(message, genericNumberPattern, CandidateType.TRANSACTION_AMOUNT, candidates, confidence = 0.60)

        return candidates
    }

    private fun findAll(
        message: String,
        pattern: Pattern,
        type: CandidateType,
        list: MutableList<NumericCandidate>,
        group: Int = 0,
        confidence: Double = 0.10
    ) {
        val matcher = pattern.matcher(message)
        while (matcher.find()) {
            val raw = matcher.group(group) ?: continue
            val start = matcher.start(group)
            val end = matcher.end(group)
            
            // Check if this range is already covered by a higher priority candidate
            if (list.any { it.start < end && start < it.end }) continue

            val normalized = if (type == CandidateType.TRANSACTION_AMOUNT || type == CandidateType.BALANCE) {
                raw.replace(",", "").toDoubleOrNull()
            } else null

            list.add(NumericCandidate(
                rawValue = raw,
                normalizedValue = normalized,
                start = start,
                end = end,
                type = type,
                confidence = confidence,
                context = message.substring(
                    (start - 20).coerceAtLeast(0),
                    (end + 20).coerceAtMost(message.length)
                )
            ))
        }
    }

    fun selectTransactionAmount(message: String, candidates: List<NumericCandidate>): NumericCandidate? {
        // Preference:
        // 1. Explicitly labeled TRANSACTION_AMOUNT that is NOT covered by other types
        // 2. If multiple TRANSACTION_AMOUNT, pick the one that is NOT near "balance", "available", etc.
        
        val amountCandidates = candidates.filter { it.type == CandidateType.TRANSACTION_AMOUNT }
        
        if (amountCandidates.isEmpty()) return null
        if (amountCandidates.size == 1) return amountCandidates.first()

        // Distinguish from balance
        val filtered = amountCandidates.filter { cand ->
            val lowerContext = cand.context.lowercase()
            !lowerContext.contains("bal") && 
            !lowerContext.contains("balance") && 
            !lowerContext.contains("available") && 
            !lowerContext.contains("avl") &&
            !lowerContext.contains("limit")
        }

        if (filtered.isNotEmpty()) return filtered.first()

        // Fallback: first one usually is the transaction amount in many formats
        return amountCandidates.first()
    }
}
