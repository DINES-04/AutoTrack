package com.example.transaction.sms

import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val merchant: String,
    val category: String = "Others"
)

object SmsParser {
    // Enhanced Patterns for UPI, GPay, and Bank transactions
    // Matches: Rs 100, Rs. 100, INR 100, ₹100, Paid 100, Sent 100, 100 debited
    private val amountPattern = Pattern.compile("(?i)(?:(?:rs|inr|amt|vpa|sum of|amounting to|debited by|credited by|paid|sent|transfer of|payment of|₹|\\$)\\.?\\s*([\\d,]+\\.?\\d*))|(?:([\\d,]+\\.?\\d*)\\s*(?:rs|inr|₹|debited|spent))")
    private val typeDebitPattern = Pattern.compile("(?i)(spent|debited|paid|withdrawal|transaction|txn|sent|transfer to|payment to|payment of|used on)")
    private val typeCreditPattern = Pattern.compile("(?i)(credited|received|deposit|added|refunded|transfer from|money received)")
    private val vpaPattern = Pattern.compile("(?i)([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9.\\-_]+)")

    private val categoryMap = mapOf(
        "Food" to listOf("zomato", "swiggy", "restaurant", "eats", "cafe", "hotel", "bakery", "mcdonalds", "kfc", "starbucks", "swiggy-instamart"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "blinkit", "zepto", "shopping", "retail", "supermarket", "grocery", "jiomart"),
        "Travel" to listOf("uber", "ola", "irctc", "rail", "bus", "fuel", "petrol", "shell", "hpcl", "bpcl", "makemytrip", "goibibo", "rapido"),
        "Bills" to listOf("recharge", "electricity", "broadband", "insurance", "bill", "postpaid", "bescom", "airtel", "jio", "vi", "tneb", "gas")
    )

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

        // Category Auto-Detection
        val cleanForCategory = message.replace(Regex("[^a-zA-Z0-9 ]"), "").lowercase()
        val detectedCategory = categoryMap.entries.find { entry ->
            entry.value.any { keyword -> cleanForCategory.contains(keyword.lowercase()) }
        }?.key ?: "Others"

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            category = detectedCategory
        )
    }
}
