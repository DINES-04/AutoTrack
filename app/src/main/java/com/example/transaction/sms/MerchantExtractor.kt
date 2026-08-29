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
        "dear", "customer", "with", "has", "been", "limit", "available", "spent", "paid",
        "rrn", "lite"
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
        val candidateSeparators = listOf(" by ", " at ", " on ", " to ", " from ", " for ")
        var bestCandidate = ""

        for (sep in candidateSeparators) {
            val parts = cleanMsg.split(Regex(sep, RegexOption.IGNORE_CASE))
            if (parts.size > 1) {
                for (i in parts.size - 1 downTo 1) {
                    val candidateRaw = parts[i].trim()
                    val candidate = extractFirstLegitWords(candidateRaw)
                    
                    if (candidate.isNotEmpty() && !isAccountOrDate(candidate)) {
                        // Special check for "on" - often marks date OR platform
                        if (sep == " on " && isDateLike(candidate)) continue
                        
                        // If the candidate is just "bank", it's likely a bank identifier at the end, ignore it
                        if (candidate.equals("bank", ignoreCase = true) || isBankAtEnd(candidateRaw)) continue

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
            
            if (cleanWord.isEmpty() || isNumericOrRef(cleanWord)) {
                if (merchantWords.isEmpty()) continue else break
            }
            
            // If it's a pure separator or noise word that ends the merchant name
            if (isPureSeparator(cleanWord)) {
                break
            }
            
            if (isAccountOrDate(cleanWord) && !isVpaCandidate(cleanWord)) {
               break
            }

            merchantWords.add(word.trim().replace(Regex("^['\"]|['\"]$"), ""))
            if (merchantWords.size >= 5) break
        }

        return merchantWords.joinToString(" ").trim()
    }

    private fun isPureSeparator(word: String): Boolean {
        val normalized = word.lowercase()
        return listOf("rrn", "ref", "utr", "txn", "not", "sms", "on").contains(normalized)
    }

    private fun isNumericOrRef(word: String): Boolean {
        val digitsOnly = word.all { it.isDigit() || it == '.' || it == ',' }
        return digitsOnly || (word.length > 10 && word.any { it.isDigit() })
    }
    
    private fun isAccountOrDate(word: String): Boolean {
        return word.contains("XX", ignoreCase = true) || 
               word.contains("/") || 
               (word.contains("-") && word.any { it.isDigit() }) ||
               (word.all { it.isDigit() || it == ':' } && word.contains(":")) ||
               word.startsWith("*")
    }

    private fun isDateLike(candidate: String): Boolean {
        val firstWord = candidate.split(" ")[0]
        return isAccountOrDate(firstWord)
    }

    private fun isBankAtEnd(raw: String): Boolean {
        return raw.startsWith("-") && raw.lowercase().contains("bank")
    }
    
    private fun isVpaCandidate(word: String): Boolean {
        return word.contains("@")
    }

    private fun normalizeMerchant(merchant: String): String {
        var clean = merchant.trim()
            .substringBefore(".")
            .substringBefore(" RRN")
            .substringBefore(" Ref")
            .substringBefore(" txn")
            .substringBefore(" on ")
            .trim()
            .trim { !it.isLetterOrDigit() && it != '&' }
        
        clean = clean.take(30).trim()
        
        return if (clean.isEmpty()) "Unknown" else clean
    }
}
