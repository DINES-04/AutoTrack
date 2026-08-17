package com.example.transaction.sms

import java.util.regex.Pattern

object MerchantExtractor {
    private val vpaPattern = Pattern.compile("(?i)([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9.\\-_]+)")

    fun extract(message: String): String {
        var merchantRaw = ""
        val vpaMatcher = vpaPattern.matcher(message)
        
        if (vpaMatcher.find()) {
            merchantRaw = vpaMatcher.group(1) ?: "UPI"
        } else {
            val cleanMsg = message.replace("\n", " ")
            // Find common separators for merchant extraction
            val forIndex = cleanMsg.indexOf(" for ", ignoreCase = true)
            val toIndex = cleanMsg.indexOf(" to ", ignoreCase = true)
            val atIndex = cleanMsg.indexOf(" at ", ignoreCase = true)
            val byIndex = cleanMsg.indexOf(" by ", ignoreCase = true)
            val viaIndex = cleanMsg.indexOf(" via ", ignoreCase = true)
            
            merchantRaw = when {
                forIndex != -1 -> cleanMsg.substring(forIndex + 5)
                toIndex != -1 -> cleanMsg.substring(toIndex + 4)
                atIndex != -1 -> cleanMsg.substring(atIndex + 4)
                byIndex != -1 -> cleanMsg.substring(byIndex + 4)
                viaIndex != -1 -> cleanMsg.substring(viaIndex + 5)
                else -> "Bank Transaction"
            }
        }

        // Clean merchant: Filter out account numbers/technical noise and take reasonable length
        var merchant = merchantRaw.trim()
            .substringBefore(".")
            .substringBefore(" UPI")
            .substringBefore(" Ref")
            .substringBefore(" on ")
            .trim()

        if (merchant.contains("a/c", true) || merchant.contains("*") || merchant.length < 2) {
            // If primary extraction failed or got an account number, check if there's a better name
            if (message.contains("by ", true)) {
                merchant = message.substringAfter("by ", "").substringBefore(".")
            } else {
                merchant = "General Transaction"
            }
        }

        merchant = merchant.take(30).trim()
        if (merchant.isBlank()) merchant = "UPI Payment"
        
        return merchant
    }
}
