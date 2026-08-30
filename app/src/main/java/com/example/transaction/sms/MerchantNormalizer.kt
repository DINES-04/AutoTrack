package com.example.transaction.sms

object MerchantNormalizer {
    /**
     * Normalizes a merchant name for consistent lookup and storage.
     * Performs:
     * - Lowercasing
     * - Trimming whitespace
     * - Collapsing multiple spaces into one
     * - Removing unnecessary punctuation at start/end
     */
    fun normalize(merchant: String): String {
        return merchant.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"), "")
            .trim()
    }
}
