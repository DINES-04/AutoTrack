package com.example.transaction.sms

import java.util.regex.Pattern

object MerchantExtractor {
    private val vpaPattern = Pattern.compile("(?i)([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9.\\-_]+)")
    
    private val stopKeywords = listOf(
        "rrn", "ref", "utr", "txn", "not", "sms", "block", "call", "contact", 
        "helpline", "report", "care", "customer", "please", "do", "if", "for", 
        "on", "at", "by", "to", "from", "via", "using", "with", "limit", "available", 
        "balance", "a/c", "account", "acc", "ac", "towards", "transfer", "debited", "credited",
        "paid", "sent", "spent", "received", "added", "successful", "transaction",
        "you", "no", "is", "your", "has", "been", "dear", "info", "upi", "lite", "wallet",
        "of", "due", "insufficient", "funds", "failed", "login", "otp", "code", "thank",
        "regards", "team", "alert", "notification", "you?", "if", "not"
    )

    private val months = listOf(
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
        "january", "february", "march", "april", "june", "july", "august", "september", "october", "november", "december"
    )

    fun extract(message: String): String {
        val cleanMsg = message.replace("\n", " ").trim()

        // 1. Try VPA Pattern first
        val vpaMatcher = vpaPattern.matcher(cleanMsg)
        if (vpaMatcher.find()) {
            val vpa = vpaMatcher.group(1) ?: ""
            if (vpa.isNotEmpty()) return MerchantNormalizer.normalize(vpa)
        }

        // 2. Structured Extraction Pipeline
        val candidateSeparators = listOf(" by ", " at ", " to ", " from ", " on ", " for ")
        
        for (sep in candidateSeparators) {
            val parts = cleanMsg.split(Regex(sep, RegexOption.IGNORE_CASE))
            if (parts.size > 1) {
                for (i in parts.size - 1 downTo 1) {
                    val candidateRaw = parts[i].trim()
                    val candidate = extractMerchantSpan(candidateRaw)
                    
                    if (candidate.isNotEmpty() && !isKnownNoise(candidate)) {
                        return normalizeMerchant(candidate)
                    }
                }
            }
        }
        
        // 3. Fallback: Search for high-value keywords
        if (cleanMsg.contains("salary", true)) return "Salary"
        if (cleanMsg.contains("refund", true)) return "Refund"

        return "Unknown"
    }

    private fun extractMerchantSpan(raw: String): String {
        val words = raw.split(Regex("[\\s,;]+"))
        val merchantWords = mutableListOf<String>()
        
        for (word in words) {
            var cleanWord = word.trim().replace(Regex("[^a-zA-Z0-9.\\-&?]"), "")
            if (cleanWord.isEmpty()) continue
            
            // Check for terminal punctuation
            val hasTerminalPunc = cleanWord.endsWith(".") || cleanWord.endsWith("?") || cleanWord.endsWith(":")
            cleanWord = cleanWord.trimEnd('.', '?', ':')
            
            val isStop = isStopWord(cleanWord)
            val isNum = isNumericOrRef(cleanWord)
            val isDateAcc = isAccountOrDate(cleanWord)
            
            if (isStop || isNum || isDateAcc) {
                if (merchantWords.isEmpty()) {
                    // Skip leading noise words, but if we encounter a terminal punctuation, we can't skip it to find a merchant later
                    if (hasTerminalPunc) return ""
                    continue
                } else {
                    // Merchant name usually doesn't contain stop words/dates
                    break
                }
            }
            
            merchantWords.add(cleanWord)
            
            if (hasTerminalPunc || merchantWords.size >= 5) break
        }

        return merchantWords.joinToString(" ").trim()
    }

    private fun isStopWord(word: String): Boolean {
        val normalized = word.lowercase()
        // Components of common merchants that are also technical words
        if (listOf("bank", "google", "app", "pay", "amazon", "india", "recharge", "coffee", "day", "jana", "salary").contains(normalized)) return false
        return stopKeywords.contains(normalized)
    }

    private fun isKnownNoise(candidate: String): Boolean {
        val normalized = candidate.lowercase()
        return normalized == "bank" || normalized == "account" || normalized == "unknown" || 
               normalized == "upi" || normalized == "vpa" || normalized == "mobile" ||
               normalized == "lite" || normalized == "wallet"
    }

    private fun isNumericOrRef(word: String): Boolean {
        val digits = word.filter { it.isDigit() }
        if (digits.length > 10) return true
        // Allow numbers in merchant names if they are short (e.g. "Shop 1")
        if (word.all { it.isDigit() } && word.length <= 2) return false
        return word.all { it.isDigit() || it == '.' || it == ',' }
    }
    
    private fun isAccountOrDate(word: String): Boolean {
        val normalized = word.lowercase().trimEnd('.', ',', ';')
        if (word.contains("/") || (word.contains("-") && word.any { it.isDigit() })) return true
        if (months.contains(normalized)) return true
        if (word.contains("XX", ignoreCase = true) || word.startsWith("*")) return true
        if (word.contains(":") && word.any { it.isDigit() }) return true
        return false
    }
    
    private fun normalizeMerchant(merchant: String): String {
        var clean = merchant.trim()
            .substringBefore(" RRN")
            .substringBefore(" Ref")
            .substringBefore(" txn")
            .substringBefore(" account")
            .substringBefore(" a/c")
            .trim()
            .trim { !it.isLetterOrDigit() && it != '&' }
        
        clean = clean.take(30).trim()
        
        return if (clean.isEmpty() || isKnownNoise(clean)) "Unknown" else clean
    }
}
