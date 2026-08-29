package com.example.transaction.sms

import java.util.regex.Pattern

object MerchantExtractor {
    private val vpaPattern = Pattern.compile("(?i)([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9.\\-_]+)")
    
    // Technical keywords that should NOT be considered as merchants
    private val technicalKeywords = listOf(
        "rs", "inr", "₹", "amt", "amount", "transaction", "txn", "ref", "reference", 
        "utr", "upi", "vpa", "date", "time", "balance", "account", "a/c", "masked",
        "mobile", "otp", "code", "successful", "pending", "failed", "reversed",
        "towards", "for", "on", "at", "by", "to", "from", "via", "using", "your",
        "dear", "customer", "with", "has", "been", "limit", "available", "spent", "paid"
    )

    fun extract(message: String): String {
        val cleanMsg = message.replace("\n", " ").trim()

        // 1. Try VPA Pattern first (High Confidence)
        val vpaMatcher = vpaPattern.matcher(cleanMsg)
        if (vpaMatcher.find()) {
            val vpa = vpaMatcher.group(1) ?: ""
            if (vpa.isNotEmpty()) return normalizeMerchant(vpa)
        }

        // 2. Structured Extraction Pipeline
        // Priority order for separators. "by" is usually the most reliable for merchants.
        val candidateSeparators = listOf(" by ", " at ", " on ", " to ", " from ")
        var bestCandidate = ""

        for (sep in candidateSeparators) {
            val parts = cleanMsg.split(Regex(sep, RegexOption.IGNORE_CASE))
            if (parts.size > 1) {
                // Check all occurrences, usually the last one is more specific in bank SMS
                for (i in parts.size - 1 downTo 1) {
                    val candidateRaw = parts[i].trim()
                    val candidate = extractFirstLegitWords(candidateRaw)
                    if (candidate.isNotEmpty() && !isTechnicalNoise(candidate) && !isAccountOrDate(candidate)) {
                        // If it's "on", verify it's not a date
                        if (sep == " on " && isDateLike(candidate)) continue
                        
                        bestCandidate = candidate
                        break
                    }
                }
                if (bestCandidate.isNotEmpty()) break
            }
        }

        if (bestCandidate.isNotEmpty()) {
            return normalizeMerchant(bestCandidate)
        }

        return "Unknown"
    }

    private fun extractFirstLegitWords(raw: String): String {
        val words = raw.split(" ")
        val merchantWords = mutableListOf<String>()
        
        for (word in words) {
            val cleanWord = word.trim().replace(Regex("[^a-zA-Z0-9.\\-&]"), "")
            
            if (cleanWord.isEmpty() || isTechnicalNoise(cleanWord) || isNumericOrRef(cleanWord)) {
                if (merchantWords.isEmpty()) continue else break
            }
            
            if (isAccountOrDate(cleanWord) && !isVpaCandidate(cleanWord)) {
               break
            }

            merchantWords.add(word.trim().replace(Regex("^['\"]|['\"]$"), ""))
            if (merchantWords.size >= 5) break
        }

        return merchantWords.joinToString(" ").trim()
    }

    private fun isTechnicalNoise(word: String): Boolean {
        val normalized = word.lowercase().replace(Regex("[^a-z]"), "")
        return technicalKeywords.contains(normalized)
    }

    private fun isNumericOrRef(word: String): Boolean {
        val digitsOnly = word.all { it.isDigit() || it == '.' || it == ',' }
        return digitsOnly || (word.length > 10 && word.any { it.isDigit() })
    }
    
    private fun isAccountOrDate(word: String): Boolean {
        return word.contains("XX", ignoreCase = true) || 
               word.contains("/") || 
               (word.contains("-") && word.any { it.isDigit() }) ||
               (word.all { it.isDigit() || it == ':' } && word.contains(":")) // Time
    }

    private fun isDateLike(candidate: String): Boolean {
        // Common date formats: 01-01-24, 2024-01-01, 01/01/2024
        val firstWord = candidate.split(" ")[0]
        return isAccountOrDate(firstWord)
    }
    
    private fun isVpaCandidate(word: String): Boolean {
        return word.contains("@")
    }

    private fun normalizeMerchant(merchant: String): String {
        var clean = merchant.trim()
            .substringBefore(".")
            .substringBefore(" Ref")
            .substringBefore(" Ref:")
            .substringBefore(" Ref No")
            .substringBefore(" on ")
            .trim()
            .trim { !it.isLetterOrDigit() && it != '&' }
        
        clean = clean.take(30).trim()
        
        return if (clean.isEmpty() || isTechnicalNoise(clean)) "Unknown" else clean
    }
}
